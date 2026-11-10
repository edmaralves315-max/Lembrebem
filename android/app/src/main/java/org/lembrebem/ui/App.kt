package org.lembrebem.ui

import android.Manifest
import android.app.AlarmManager
import android.content.*
import android.content.pm.PackageManager
import android.net.*
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.*
import org.lembrebem.*
import org.lembrebem.data.*
import org.lembrebem.domain.Rules
import org.lembrebem.platform.*
import org.lembrebem.clinical.ClinicalScreen
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable fun BigButton(id:Int,onClick:()->Unit){Button(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp),contentPadding=PaddingValues(16.dp)){Text(stringResource(id),fontSize=22.sp)}}
@Composable fun Field(id:Int,value:String,password:Boolean=false,change:(String)->Unit){OutlinedTextField(value=value,onValueChange=change,label={Text(stringResource(id))},modifier=Modifier.fillMaxWidth(),visualTransformation=if(password)PasswordVisualTransformation()else VisualTransformation.None,keyboardOptions=KeyboardOptions(keyboardType=if(password)KeyboardType.Password else KeyboardType.Text))}
@Composable fun Check(id:Int,checked:Boolean,change:(Boolean)->Unit){Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(checked=checked,onCheckedChange=change);TextButton(onClick={change(!checked)},modifier=Modifier.weight(1f)){Text(stringResource(id),fontSize=18.sp)}}}
fun safeOpen(context:Context,intent:Intent){try{context.startActivity(intent)}catch(_:android.content.ActivityNotFoundException){android.widget.Toast.makeText(context,R.string.invalid,android.widget.Toast.LENGTH_LONG).show()}}
fun formatInstant(value:String,context:Context):String=try{val at=Instant.parse(value).atZone(ZoneId.systemDefault());val date=at.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(context.resources.configuration.locales[0]));val time=android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date.from(at.toInstant()));"$date $time"}catch(_:Exception){value}

@Composable fun LembreBem(requestedDose:String?=null,requestVersion:Int=0){val context=LocalContext.current;val app=context.applicationContext as LembreBemApp;val dao=app.repo.dao;val scope=rememberCoroutineScope()
 val prefs by context.settings.data.collectAsStateWithLifecycle(initialValue=androidx.datastore.preferences.core.emptyPreferences());val mode=prefs[Keys.MODE].orEmpty()
 var editingPlan by remember{mutableStateOf<MedicationPlan?>(null)};var screen by remember{mutableStateOf("home")};var message by remember{mutableStateOf<Int?>(null)}
 val plans by dao.plans().collectAsStateWithLifecycle(emptyList());val doses by dao.doses().collectAsStateWithLifecycle(emptyList());val contacts by dao.contacts().collectAsStateWithLifecycle(emptyList());val appointments by dao.appointments().collectAsStateWithLifecycle(emptyList())
 LaunchedEffect(requestedDose,requestVersion){if(requestedDose!=null)screen="dose"}
 if(screen=="clinical"){ClinicalScreen(onBack={screen="home"},canReview=mode=="caregiver");return}
 Surface(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize()){
 if(mode.isNotBlank())BigButton(R.string.help){scope.launch{app.repo.event("help",JSONObject().put("initiatedByUser",true))};screen="help"}
 Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text(stringResource(R.string.app_name),style=MaterialTheme.typography.headlineLarge)
 if(prefs[Keys.ENDPOINT].isNullOrBlank()||!RemoteAuth.ready(context))Text(stringResource(R.string.demo),style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.secondary)
 message?.let{Text(stringResource(it),color=MaterialTheme.colorScheme.error);TextButton(onClick={message=null}){Text(stringResource(R.string.close))}}
 if(mode.isBlank()){
 Text(stringResource(R.string.welcome),style=MaterialTheme.typography.headlineSmall);LanguagePicker()
 BigButton(R.string.assisted){scope.launch{context.settings.edit{it[Keys.MODE]="assisted"}}};BigButton(R.string.caregiver){scope.launch{context.settings.edit{it[Keys.MODE]="caregiver"}}};Text(stringResource(R.string.local_only));return@Column
 }
 if(screen!="home")BigButton(R.string.back){screen="home"}
 when(screen){
 "home"->{VoiceStatus();ConnectivityStatus();BigButton(R.string.my_day){screen="day"};BigButton(R.string.medications){screen="medications"};BigButton(R.string.appointments){screen="appointments"};BigButton(R.string.family){screen="family"};BigButton(R.string.record_consultation){screen="clinical"};BigButton(R.string.settings){screen="settings"}}
 "dose"->{val selected=doses.find{it.id==requestedDose};val plan=plans.find{it.id==selected?.planId};if(selected!=null&&plan!=null)MedicationCard(plan,listOf(selected),onMessage={message=it})else Text(stringResource(R.string.empty))}
 "day","medications"->{Text(stringResource(if(screen=="day")R.string.my_day else R.string.medications),style=MaterialTheme.typography.headlineMedium)
 if(plans.isEmpty())Text(stringResource(R.string.empty));plans.forEach{plan->MedicationCard(plan,doses.filter{it.planId==plan.id},onMessage={message=it},onEdit=if(mode=="caregiver"){{editingPlan=plan;screen="addMedication"}}else null)}
 if(screen=="day")appointments.forEach{AppointmentCard(it)}
 if(mode=="caregiver")BigButton(R.string.add){editingPlan=null;screen="addMedication"}}
 "addMedication"->MedicationForm(initial=editingPlan){p->scope.launch{try{app.repo.savePlan(p);if(prefs[Keys.REMOTE_OWNER]!=false)AlarmScheduler(context).schedule(p);message=R.string.saved;screen="medications"}catch(_:Exception){message=R.string.invalid}}}
 "appointments"->{Text(stringResource(R.string.appointments),style=MaterialTheme.typography.headlineMedium);if(appointments.isEmpty())Text(stringResource(R.string.empty));appointments.forEach{AppointmentCard(it)};if(mode=="caregiver")BigButton(R.string.add){screen="addAppointment"}}
 "addAppointment"->AppointmentForm{a->scope.launch{dao.appointment(a);AlarmScheduler(context).scheduleAppointment(a);app.repo.event("appointment",JSONObject().put("id",a.id).put("title",a.title).put("at",a.at).put("place",a.place).put("preparation",a.preparation).put("documents",a.documents));dao.audit(Audit(action="appointment_saved",targetId=a.id));screen="appointments"}}
 "family"->{Text(stringResource(R.string.family),style=MaterialTheme.typography.headlineMedium);contacts.forEach{ContactCardView(it)};if(mode=="caregiver")BigButton(R.string.add){screen="addContact"}}
 "addContact"->ContactForm{c->scope.launch{dao.contact(c);screen="family"}}
 "help"->{Text(stringResource(R.string.help_guidance),fontSize=24.sp);BigButton(R.string.repeat){Voice(context).speak(context.getString(R.string.help_guidance)){message=R.string.voice_missing}};contacts.forEach{ContactCardView(it)};BigButton(R.string.third_party){screen="thirdParty"}}
 "thirdParty"->{var reference by remember{mutableStateOf("")};Text(stringResource(R.string.help_guidance),fontSize=22.sp);Field(R.string.reference,reference){reference=it};Text(stringResource(R.string.cannot_validate));BigButton(R.string.save){scope.launch{app.repo.event("help",JSONObject().put("reference",reference).put("validation","not_possible"));screen="help"}}}
 "settings"->{BigButton(R.string.language){screen="language"};BigButton(R.string.permission_center){screen="permissions"};BigButton(R.string.safe_area){screen="safe"};BigButton(R.string.privacy){screen="privacy"};BigButton(R.string.remote){screen="remote"};BigButton(R.string.link_family){screen="links"};BigButton(R.string.audit){screen="audit"};BigButton(R.string.switch_mode){scope.launch{context.settings.edit{it.remove(Keys.MODE)}}}}
 "language"->{LanguagePicker();var country by remember{mutableStateOf(prefs[Keys.COUNTRY]?:"BR")};Field(R.string.country,country){country=it};BigButton(R.string.save){scope.launch{context.settings.edit{it[Keys.COUNTRY]=country.uppercase()};dao.profile()?.let{dao.profile(it.copy(countryCode=country.uppercase()))};message=R.string.saved}}}
 "permissions"->PermissionCenter()
 "privacy"->PrivacyScreen(onMessage={message=it})
 "safe"->SafeAreaScreen(onMessage={message=it},isCaregiver=mode=="caregiver")
 "remote"->RemoteScreen(onMessage={message=it})
 "links"->FamilyLinksScreen(onMessage={message=it})
 "audit"->{val audits by dao.audits().collectAsStateWithLifecycle(emptyList());audits.forEach{Text("${formatInstant(it.at,context)} • ${it.action} • v${it.version}")}}
 }
 }}}}

@Composable fun MedicationCard(plan:MedicationPlan,doses:List<Dose>,onMessage:(Int)->Unit,onEdit:(()->Unit)?=null){val context=LocalContext.current;val app=context.applicationContext as LembreBemApp;val scope=rememberCoroutineScope()
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 Text(plan.name,fontSize=26.sp);Text(plan.dosage,fontSize=22.sp);if(onEdit!=null)BigButton(R.string.edit,onEdit);Text("${plan.time} • ${plan.timeZone}",fontSize=20.sp)
 BigButton(R.string.repeat){Voice(context).speak(context.getString(R.string.medication_speech,plan.name,plan.dosage)){onMessage(R.string.voice_missing)}}
 val latest=doses.firstOrNull();if(latest!=null){Text(formatInstant(latest.scheduledAt,context));Text(stringResource(when(latest.status){"taken"->R.string.status_taken;"not_yet"->R.string.status_not_yet;"snoozed"->R.string.status_snoozed;else->R.string.pending}),fontSize=22.sp);latest.acknowledgedAt?.let{Text(formatInstant(it,context))}
 var correcting by remember(latest.id,latest.status){mutableStateOf(false)}
 if(latest.status=="taken"&&!correcting){BigButton(R.string.correction){correcting=true}}else{
 listOf(R.string.taken to "taken",R.string.not_yet to "not_yet",R.string.snooze to "snoozed").forEach{(label,status)->BigButton(label){scope.launch{app.repo.acknowledge(latest,status);if(status=="snoozed")AlarmScheduler(context).schedule(plan,Instant.now().plusSeconds(600),latest.scheduledAt);onMessage(R.string.saved)}}}
 }
 }else{Text(stringResource(R.string.pending))}
 }}}
@Composable fun MedicationForm(initial:MedicationPlan?=null,save:(MedicationPlan)->Unit){var name by remember{mutableStateOf(initial?.name.orEmpty())};var presentation by remember{mutableStateOf(initial?.presentation.orEmpty())};var dosage by remember{mutableStateOf(initial?.dosage.orEmpty())};var time by remember{mutableStateOf(initial?.time?:"08:00")};var days by remember{mutableStateOf(initial?.days?:"1,2,3,4,5,6,7")};var start by remember{mutableStateOf(initial?.startDate?:LocalDate.now().toString())};var end by remember{mutableStateOf(initial?.endDate.orEmpty())};var zone by remember{mutableStateOf(initial?.timeZone?:ZoneId.systemDefault().id)};var notes by remember{mutableStateOf(initial?.notes.orEmpty())};var source by remember{mutableStateOf(initial?.source.orEmpty())};var approved by remember{mutableStateOf(false)};var invalid by remember{mutableStateOf(false)}
 Text(stringResource(R.string.clinical_warning),fontSize=22.sp);Field(R.string.name,name){name=it};Field(R.string.presentation,presentation){presentation=it};Field(R.string.dosage,dosage){dosage=it};Field(R.string.time,time){time=it};Field(R.string.days,days){days=it};Field(R.string.start_date,start){start=it};Field(R.string.end_date,end){end=it};Field(R.string.timezone,zone){zone=it};Field(R.string.notes,notes){notes=it};Field(R.string.source,source){source=it};Check(R.string.reviewed,approved){approved=it};if(invalid)Text(stringResource(R.string.invalid),color=MaterialTheme.colorScheme.error)
 BigButton(R.string.save){try{require(approved&&name.isNotBlank()&&dosage.isNotBlank()&&source.isNotBlank());LocalTime.parse(time);val first=LocalDate.parse(start);val last=end.takeIf{it.isNotBlank()}?.let(LocalDate::parse);require(last==null||last>=first);require(days.split(',').all{it.toInt() in 1..7});ZoneId.of(zone);save(MedicationPlan(id=initial?.id?:java.util.UUID.randomUUID().toString(),version=(initial?.version?:0)+1,name=name,presentation=presentation,dosage=dosage,time=time,days=days,startDate=start,endDate=end,timeZone=zone,notes=notes,source=source,approved=true))}catch(_:Exception){invalid=true}}
}
@Composable fun AppointmentCard(a:Appointment){val context=LocalContext.current;Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(a.title,fontSize=24.sp);Text(formatInstant(a.at,context));Text(a.place);Text(a.preparation);Text(a.documents)}}}
@Composable fun AppointmentForm(save:(Appointment)->Unit){var title by remember{mutableStateOf("")};var at by remember{mutableStateOf("")};var place by remember{mutableStateOf("")};var preparation by remember{mutableStateOf("")};var docs by remember{mutableStateOf("")};var invalid by remember{mutableStateOf(false)}
 Field(R.string.name,title){title=it};Field(R.string.appointment_at,at){at=it};Field(R.string.place,place){place=it};Field(R.string.notes,preparation){preparation=it};Field(R.string.documents,docs){docs=it};if(invalid)Text(stringResource(R.string.invalid));BigButton(R.string.save){try{require(title.isNotBlank());val instant=LocalDateTime.parse(at).atZone(ZoneId.systemDefault()).toInstant();save(Appointment(title=title,at=instant.toString(),place=place,preparation=preparation,documents=docs))}catch(_:Exception){invalid=true}}}

@Composable fun ContactCardView(c:ContactCard){val context=LocalContext.current
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 val photo=remember(c.photoUri){try{if(c.photoUri.isBlank())null else context.contentResolver.openInputStream(Uri.parse(c.photoUri))?.use{android.graphics.BitmapFactory.decodeStream(it)}}catch(_:Exception){null}}
 if(photo!=null)androidx.compose.foundation.Image(bitmap=photo.asImageBitmap(),contentDescription=c.label,modifier=Modifier.size(100.dp))
 Text(c.label,fontSize=26.sp);if(c.relation.isNotBlank())Text(c.relation);if(c.phone.isNotBlank()){BigButton(R.string.call){safeOpen(context,Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",c.phone,null)))};BigButton(R.string.whatsapp){safeOpen(context,Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+c.phone.filter{it.isDigit()})).setPackage("com.whatsapp"))};BigButton(R.string.sms){safeOpen(context,Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",c.phone,null)).putExtra("sms_body",context.getString(R.string.help_guidance)))}}
 }}}
@Composable fun ContactForm(save:(ContactCard)->Unit){val context=LocalContext.current;var label by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var relation by remember{mutableStateOf("")};var photo by remember{mutableStateOf("")};var error by remember{mutableStateOf(false)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->if(uri!=null){try{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};photo=uri.toString()}}
 Field(R.string.name,label){label=it};Field(R.string.phone,phone){phone=it};Field(R.string.relation,relation){relation=it};BigButton(R.string.photo){picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))};if(error)Text(stringResource(R.string.invalid));BigButton(R.string.save){if(label.isNotBlank())save(ContactCard(label=label,phone=phone,relation=relation,photoUri=photo))else error=true}}
@Composable fun LanguagePicker(){val context=LocalContext.current;val app=context.applicationContext as LembreBemApp;val scope=rememberCoroutineScope();Text(stringResource(R.string.language),style=MaterialTheme.typography.headlineSmall)
 val choices=listOf("" to stringResource(R.string.system_language),"pt-BR" to "Português (Brasil)","pt-PT" to "Português (Portugal)","en-US" to "English (United States)","ja-JP" to "日本語", "es-ES" to "Español (España)")
 choices.forEach{(tag,label)->OutlinedButton(onClick={scope.launch{context.settings.edit{it[Keys.LOCALE]=tag};context.getSharedPreferences("language",0).edit().putString("locale",tag).apply();app.repo.dao.profile()?.let{app.repo.dao.profile(it.copy(preferredLocale=tag.ifBlank{Locale.getDefault().toLanguageTag()}))};withContext(Dispatchers.Main){AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))}}},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text(label,fontSize=20.sp)}}
}
@Composable fun VoiceStatus(){val context=LocalContext.current;val prefs=remember{context.getSharedPreferences("voice",0)};var missing by remember{mutableStateOf(prefs.getBoolean("unavailable",false))}
 DisposableEffect(prefs){val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener{p,key->if(key=="unavailable")missing=p.getBoolean(key,false)};prefs.registerOnSharedPreferenceChangeListener(listener);onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}}
 if(missing){Text(stringResource(R.string.voice_missing),fontSize=20.sp);BigButton(R.string.install_voice){safeOpen(context,Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))}}
}
@Composable fun ConnectivityStatus(){val context=LocalContext.current;val cm=requireNotNull(context.getSystemService(ConnectivityManager::class.java));var connected by remember{mutableStateOf(cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true)}
 DisposableEffect(cm){val cb=object:ConnectivityManager.NetworkCallback(){override fun onCapabilitiesChanged(network:Network,c:NetworkCapabilities){connected=c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)};override fun onLost(network:Network){connected=false}};cm.registerDefaultNetworkCallback(cb);onDispose{cm.unregisterNetworkCallback(cb)}}
 Text(stringResource(if(connected)R.string.online else R.string.offline),fontSize=18.sp);if(!connected)BigButton(R.string.network_settings){safeOpen(context,Intent(Settings.ACTION_WIRELESS_SETTINGS))}
}
@Composable fun PermissionCenter(){val context=LocalContext.current;var revision by remember{mutableStateOf(0)}
 val single=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){revision++}
 val location=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){revision++}
 Text(stringResource(R.string.permission_explanation),fontSize=20.sp)
 fun granted(p:String)=ContextCompat.checkSelfPermission(context,p)==PackageManager.PERMISSION_GRANTED
 @Composable fun State(granted:Boolean){Text(stringResource(R.string.permission_status,stringResource(if(granted)R.string.permission_granted else R.string.permission_denied)))}
 State(Build.VERSION.SDK_INT<33||granted(Manifest.permission.POST_NOTIFICATIONS));BigButton(R.string.notifications){if(Build.VERSION.SDK_INT>=33)single.launch(Manifest.permission.POST_NOTIFICATIONS)}
 State(granted(Manifest.permission.RECORD_AUDIO));BigButton(R.string.microphone){single.launch(Manifest.permission.RECORD_AUDIO)}
 State(granted(Manifest.permission.ACCESS_FINE_LOCATION));BigButton(R.string.location_permission){location.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))}
 if(Build.VERSION.SDK_INT>=29){State(granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION));BigButton(R.string.background_location){if(granted(Manifest.permission.ACCESS_FINE_LOCATION)){if(Build.VERSION.SDK_INT==29)single.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)else safeOpen(context,Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}}}
 val alarm=requireNotNull(context.getSystemService(AlarmManager::class.java));State(Build.VERSION.SDK_INT<31||alarm.canScheduleExactAlarms());BigButton(R.string.exact_alarm){if(Build.VERSION.SDK_INT>=31)safeOpen(context,Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:${context.packageName}")))}
 BigButton(R.string.gps_settings){safeOpen(context,Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))};BigButton(R.string.install_voice){safeOpen(context,Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))};Text(revision.toString(),fontSize=1.sp)
}
@Composable fun PrivacyScreen(onMessage:(Int)->Unit){val context=LocalContext.current;val app=context.applicationContext as LembreBemApp;val scope=rememberCoroutineScope();var profile by remember{mutableStateOf<Profile?>(null)};var confirm by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){profile=app.repo.dao.profile()}
 Text(stringResource(R.string.clinical_warning));profile?.let{p->Check(R.string.health_consent,p.healthConsent){value->scope.launch{val n=p.copy(healthConsent=value);app.repo.dao.profile(n);profile=n}};Check(R.string.location_consent,p.locationConsent){value->scope.launch{val n=p.copy(locationConsent=value);app.repo.dao.profile(n);profile=n;if(!value)try{SafeArea(context).disable()}catch(_:Exception){}}}}
 val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)scope.launch(Dispatchers.IO){val json=exportLocal(context);context.contentResolver.openOutputStream(uri)?.use{it.write(json.toString(2).toByteArray())}}}
 BigButton(R.string.export){export.launch("lembrebem-export.json")};BigButton(R.string.delete){confirm=true}
 if(confirm)AlertDialog(onDismissRequest={confirm=false},text={Text(stringResource(R.string.delete_confirm))},confirmButton={TextButton(onClick={scope.launch(Dispatchers.IO){app.repo.dao.allPlans().forEach{p->val next=Rules.nextDose(p.time,p.days.split(',').map{it.toInt()}.toSet(),LocalDate.parse(p.startDate),p.endDate.takeIf{it.isNotBlank()}?.let(LocalDate::parse),ZoneId.of(p.timeZone),Instant.now());if(next!=null)AlarmScheduler(context).cancel(p.id,Rules.wireInstant(next))};try{SafeArea(context).disable()}catch(_:Exception){};org.lembrebem.clinical.ClinicalStorage(context).deleteAll();Store.get(context).clearAllTables();context.filesDir.listFiles()?.filter{it.name!="datastore"}?.forEach{it.deleteRecursively()};context.settings.edit{it.clear()};if(RemoteAuth.ready(context))com.google.firebase.auth.FirebaseAuth.getInstance().signOut();listOf("push","sync","battery","language","clinical","voice","firebase","sync_cursors").forEach{context.getSharedPreferences(it,0).edit().clear().apply()};withContext(Dispatchers.Main){confirm=false;onMessage(R.string.saved)}}}){Text(stringResource(R.string.delete))}},dismissButton={TextButton(onClick={confirm=false}){Text(stringResource(R.string.cancel))}})
}
@Composable fun SafeAreaScreen(onMessage:(Int)->Unit,isCaregiver:Boolean){val context=LocalContext.current;val app=context.applicationContext as LembreBemApp;val scope=rememberCoroutineScope();var lat by remember{mutableStateOf("")};var lon by remember{mutableStateOf("")};var radius by remember{mutableStateOf("300")};val last by app.repo.dao.lastLocation().collectAsStateWithLifecycle(null)
 Text(stringResource(R.string.safe_limits),fontSize=20.sp)
 if(isCaregiver){Field(R.string.latitude,lat){lat=it};Field(R.string.longitude,lon){lon=it};Field(R.string.radius,radius){radius=it};BigButton(R.string.enable){scope.launch{try{val latitude=lat.toDouble();val longitude=lon.toDouble();require(latitude in -90.0..90.0&&longitude in -180.0..180.0);SafeArea(context).enable(GeofenceConfig(latitude=latitude,longitude=longitude,radius=radius.toFloat()));onMessage(R.string.saved)}catch(_:Exception){onMessage(R.string.invalid)}}}}
 BigButton(R.string.disable){scope.launch{try{SafeArea(context).disable();onMessage(R.string.saved)}catch(_:Exception){onMessage(R.string.invalid)}}}
 Text(stringResource(R.string.last_location));if(last==null)Text(stringResource(R.string.no_location))else{val l=last!!;Text("${formatInstant(l.at,context)} • ±${l.accuracy} m");if(isCaregiver)BigButton(R.string.open_map){safeOpen(context,Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${l.latitude},${l.longitude}")))}}
}
@Composable fun RemoteScreen(onMessage:(Int)->Unit){val context=LocalContext.current;val scope=rememberCoroutineScope();val prefs by context.settings.data.collectAsStateWithLifecycle(androidx.datastore.preferences.core.emptyPreferences());var endpoint by remember(prefs[Keys.ENDPOINT]){mutableStateOf(prefs[Keys.ENDPOINT].orEmpty())};var key by remember{mutableStateOf("")};var appId by remember{mutableStateOf("")};var project by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
 Text(stringResource(R.string.remote_warning),fontSize=20.sp);Field(R.string.endpoint,endpoint){endpoint=it};Field(R.string.firebase_key,key){key=it};Field(R.string.firebase_app,appId){appId=it};Field(R.string.firebase_project,project){project=it};Field(R.string.email,email){email=it};Field(R.string.password,password,true){password=it}
 fun signIn(create:Boolean){scope.launch{try{require(endpoint.startsWith("https://"));RemoteAuth.configure(context,key,appId,project);RemoteAuth.signIn(email,password,create);context.getSharedPreferences("firebase",0).edit().putString("key",key).putString("app",appId).putString("project",project).apply();context.settings.edit{it[Keys.ENDPOINT]=endpoint};password="";onMessage(R.string.saved)}catch(_:Exception){onMessage(R.string.invalid)}}}
 BigButton(R.string.sign_in){signIn(false)};BigButton(R.string.create_account){signIn(true)};BigButton(R.string.sync_now){WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build());onMessage(R.string.saved)}
 val sync=context.getSharedPreferences("sync",0);Text(stringResource(R.string.sync_status));Text(sync.getString("at",null)?.let{formatInstant(it,context)}?:stringResource(R.string.demo));if(sync.getString("status","") in listOf("error","rejected"))Text(stringResource(R.string.sync_error),color=MaterialTheme.colorScheme.error)
}

@Composable fun FamilyLinksScreen(onMessage:(Int)->Unit){val context=LocalContext.current;val app=context.applicationContext as LembreBemApp;val scope=rememberCoroutineScope();val prefs by context.settings.data.collectAsStateWithLifecycle(androidx.datastore.preferences.core.emptyPreferences());var invite by remember{mutableStateOf("")};var consent by remember{mutableStateOf(false)};var created by remember{mutableStateOf("")};var people by remember{mutableStateOf<List<JSONObject>>(emptyList())};var grants by remember{mutableStateOf<List<JSONObject>>(emptyList())};var alerts by remember{mutableStateOf<List<JSONObject>>(emptyList())};var revoke by remember{mutableStateOf<String?>(null)};var remove by remember{mutableStateOf(false)}
 suspend fun api():Api{val token=RemoteAuth.token(context)?:error("auth");return Api(prefs[Keys.ENDPOINT]?:error("endpoint"),token)}
 suspend fun personId():String=prefs[Keys.REMOTE_PERSON]?:app.repo.dao.profile()?.id?:error("profile")
 fun call(block:suspend ()->Unit){scope.launch{try{withContext(Dispatchers.IO){block()}}catch(_:Exception){onMessage(R.string.invalid)}}}
 Text(stringResource(R.string.invite_warning),fontSize=20.sp);Check(R.string.consent_confirmed,consent){consent=it}
 BigButton(R.string.invite){if(!consent){onMessage(R.string.invalid)}else call{created=api().request("/people/${personId()}/invites",JSONObject().put("scopes",JSONArray(listOf("read","health","location","alerts"))).put("consentConfirmed",true)).getString("code")}}
 if(created.isNotBlank())androidx.compose.foundation.text.selection.SelectionContainer{Text(created)}
 Field(R.string.invite_code,invite){invite=it};BigButton(R.string.accept_invite){call{val profile=app.repo.dao.profile()?:error("profile");val id=api().request("/invites/accept",JSONObject().put("code",invite).put("preferredLocale",profile.preferredLocale)).getString("personId");require(app.repo.dao.allPlans().isEmpty()||(prefs[Keys.REMOTE_PERSON]==id));context.settings.edit{it[Keys.REMOTE_PERSON]=id;it[Keys.REMOTE_OWNER]=false};onMessage(R.string.saved)}}
 BigButton(R.string.refresh){call{val a=api();val array=a.request("/people").getJSONArray("people");people=(0 until array.length()).map{array.getJSONObject(it)};try{val g=a.request("/people/${personId()}/grants").getJSONArray("grants");grants=(0 until g.length()).map{g.getJSONObject(it)}}catch(_:Exception){grants=emptyList()};val notices=a.request("/people/${personId()}/alerts").getJSONArray("alerts");alerts=(0 until notices.length()).map{notices.getJSONObject(it)}}}
 Text(stringResource(R.string.select_person));people.forEach{p->OutlinedButton(onClick={scope.launch{if(app.repo.dao.allPlans().isNotEmpty()&&p.getString("id")!=(prefs[Keys.REMOTE_PERSON]?:app.repo.dao.profile()?.id)){onMessage(R.string.single_profile);return@launch};context.settings.edit{it[Keys.REMOTE_PERSON]=p.getString("id");it[Keys.REMOTE_OWNER]=(p.getString("id")==app.repo.dao.profile()?.id)}}},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text(p.getString("id"))}}
 grants.forEach{g->Text(g.getString("uid"));BigButton(R.string.revoke){revoke=g.getString("uid")}}
 if(revoke!=null)AlertDialog(onDismissRequest={revoke=null},text={Text(stringResource(R.string.revoke_confirm))},confirmButton={TextButton(onClick={val uid=revoke!!;revoke=null;call{api().request("/people/${personId()}/grants/$uid",method="DELETE");grants=grants.filter{it.getString("uid")!=uid}}}){Text(stringResource(R.string.revoke))}},dismissButton={TextButton(onClick={revoke=null}){Text(stringResource(R.string.cancel))}})
 Text(stringResource(R.string.alerts),style=MaterialTheme.typography.headlineSmall);if(alerts.isEmpty())Text(stringResource(R.string.empty));alerts.forEach{a->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(when(a.optString("kind")){"dose"->stringResource(R.string.medication_alarm);"unconfirmed"->stringResource(R.string.remote_unconfirmed);"heartbeat"->stringResource(R.string.heartbeat_alert,formatInstant(a.getString("occurredAt"),context));"charging"->stringResource(R.string.charging);"battery"->stringResource(R.string.battery_title);"geofence_exit","help"->stringResource(R.string.safe_question);else->stringResource(R.string.private_alert)},fontSize=22.sp);Text(formatInstant(a.getString("occurredAt"),context));val data=a.optJSONObject("data");if(data?.optBoolean("locationUnavailable",false)==true)Text(stringResource(R.string.no_location));if(data!=null&&data.has("latitude")){Text("±${data.optDouble("accuracy")} m");BigButton(R.string.open_map){safeOpen(context,Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${data.getDouble("latitude")},${data.getDouble("longitude")}")))}}}}}
 BigButton(R.string.stop_remote){scope.launch{context.settings.edit{it.remove(Keys.ENDPOINT);it.remove(Keys.REMOTE_PERSON);it.remove(Keys.REMOTE_OWNER)};if(RemoteAuth.ready(context))com.google.firebase.auth.FirebaseAuth.getInstance().signOut();onMessage(R.string.saved)}}
 BigButton(R.string.remote_delete){remove=true};if(remove)AlertDialog(onDismissRequest={remove=false},text={Text(stringResource(R.string.delete_confirm))},confirmButton={TextButton(onClick={remove=false;call{api().request("/people/${personId()}",method="DELETE");onMessage(R.string.saved)}}){Text(stringResource(R.string.delete))}},dismissButton={TextButton(onClick={remove=false}){Text(stringResource(R.string.cancel))}})
}
