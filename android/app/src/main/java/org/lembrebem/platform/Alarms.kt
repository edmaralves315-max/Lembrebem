package org.lembrebem.platform
import android.app.*
import android.content.*
import android.os.*
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.lembrebem.*
import org.lembrebem.data.*
import org.lembrebem.domain.Rules
import java.time.*
import java.util.Locale

fun localizedContext(context:Context):Context {val tag=context.getSharedPreferences("language",0).getString("locale","").orEmpty();if(tag.isBlank())return context;val config=android.content.res.Configuration(context.resources.configuration);config.setLocale(Locale.forLanguageTag(tag));return context.createConfigurationContext(config)}

class AlarmScheduler(private val context:Context){
 private val alarm=requireNotNull(context.getSystemService(AlarmManager::class.java))
 private fun pending(id:String,at:String,version:Int=0)=PendingIntent.getBroadcast(context,(id+at).hashCode(),Intent(context,AlarmReceiver::class.java).putExtra("planId",id).putExtra("scheduledAt",at).putExtra("version",version),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 suspend fun restore(){if(context.settings.data.first()[Keys.REMOTE_OWNER]==false)return;Store.get(context).dao().allPlans().filter{it.approved}.forEach{schedule(it)};Store.get(context).dao().allAppointments().forEach{scheduleAppointment(it)};try{Store.get(context).dao().geofence()?.takeIf{it.enabled}?.let{SafeArea(context).enable(it)}}catch(_:Exception){}}
 fun schedule(plan:MedicationPlan,overrideAt:Instant?=null,originalScheduledAt:String?=null){
  val at=overrideAt?:Rules.nextDose(plan.time,plan.days.split(',').map{it.toInt()}.toSet(),LocalDate.parse(plan.startDate),plan.endDate.takeIf{it.isNotBlank()}?.let(LocalDate::parse),ZoneId.of(plan.timeZone),Instant.now())?:return
  val pi=pending(plan.id,originalScheduledAt?:Rules.wireInstant(at),plan.version)
  if(Build.VERSION.SDK_INT<31||alarm.canScheduleExactAlarms())alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at.toEpochMilli(),pi)
  else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at.toEpochMilli(),pi)
 }
 fun scheduleAppointment(appointment:Appointment){val at=Instant.parse(appointment.at).minusSeconds(appointment.reminderMinutes*60L);if(at<=Instant.now())return;val pi=PendingIntent.getBroadcast(context,appointment.id.hashCode(),Intent(context,AlarmReceiver::class.java).putExtra("appointment",appointment.id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT<31||alarm.canScheduleExactAlarms())alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at.toEpochMilli(),pi)else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at.toEpochMilli(),pi)}
 fun cancel(planId:String,at:String){alarm.cancel(pending(planId,at))}
}
class AlarmReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){val pending=goAsync();(context.applicationContext as LembreBemApp).scope.launch{try{
 val dao=Store.get(context).dao();val appointmentId=intent.getStringExtra("appointment");if(appointmentId!=null){val appointment=dao.allAppointments().find{it.id==appointmentId}?:return@launch;Alerts.show(context,R.string.appointments,R.string.open_reminder,appointmentId.hashCode());Voice(context).speak(localizedContext(context).getString(R.string.appointment_speech,appointment.title));return@launch};val plan=dao.plan(intent.getStringExtra("planId")?:return@launch)?:return@launch
 if(intent.getIntExtra("version",plan.version)!=plan.version)return@launch
 val at=intent.getStringExtra("scheduledAt")?:return@launch
 val dose=Dose(Rules.doseId(plan.id,Instant.parse(at)),plan.id,at);dao.insertDose(dose);AlarmScheduler(context).schedule(plan);if(!Rules.shouldAnnounceDose(dao.dose(dose.id)?.status))return@launch
 Alerts.show(context,R.string.medication_alarm,R.string.open_reminder,dose.id.hashCode(),dose.id)
 Voice(context).speak(localizedContext(context).getString(R.string.medication_speech,plan.name,plan.dosage))
 }finally{pending.finish()}}}}
class RestoreReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){val result=goAsync();(context.applicationContext as LembreBemApp).scope.launch{try{AlarmScheduler(context).restore()}finally{result.finish()}}}}
object Alerts{
 fun show(originalContext:Context,title:Int,body:Int,id:Int=1,doseId:String?=null){val context=localizedContext(originalContext);val manager=requireNotNull(context.getSystemService(NotificationManager::class.java))
 manager.createNotificationChannel(NotificationChannel("care",context.getString(R.string.alert_channel),NotificationManager.IMPORTANCE_HIGH).apply{lockscreenVisibility=Notification.VISIBILITY_PRIVATE})
 val open=PendingIntent.getActivity(context,id,Intent(context,MainActivity::class.java).apply{if(doseId!=null){putExtra("doseId",doseId);data=android.net.Uri.parse("lembrebem://dose/"+android.net.Uri.encode(doseId))};flags=Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP},PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
 val notification=NotificationCompat.Builder(context,"care").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(context.getString(title)).setContentText(context.getString(body)).setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(NotificationCompat.Builder(context,"care").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(context.getString(R.string.app_name)).setContentText(context.getString(R.string.private_alert)).build()).build()
 if(Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)NotificationManagerCompat.from(context).notify(id,notification)
 }
}
class Voice(originalContext:Context){private val context=localizedContext(originalContext);private var tts:TextToSpeech?=null
 private fun unavailable(onUnavailable:()->Unit){
  context.getSharedPreferences("voice",0).edit().putBoolean("unavailable",true).apply()
  onUnavailable()
  try{android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM,80).apply{startTone(android.media.ToneGenerator.TONE_PROP_BEEP,600);Handler(Looper.getMainLooper()).postDelayed({release()},650)}}catch(_:RuntimeException){}
 }
 fun speak(text:String,onUnavailable:()->Unit={}){
  // Initialisation and Android audio APIs are confined to the main looper.
  Handler(Looper.getMainLooper()).post{
   tts=TextToSpeech(context){status->
    val engine=tts
    if(status!=TextToSpeech.SUCCESS||engine==null){unavailable(onUnavailable);engine?.shutdown();return@TextToSpeech}
    val locale=context.resources.configuration.locales[0]
    val result=engine.setLanguage(locale)
    val offline=engine.voices?.firstOrNull{it.locale.language==locale.language&&!it.isNetworkConnectionRequired}
    if(result<0||offline==null){unavailable(onUnavailable);engine.shutdown()}else{
     engine.voice=offline
     context.getSharedPreferences("voice",0).edit().putBoolean("unavailable",false).apply()
     engine.setOnUtteranceProgressListener(object:android.speech.tts.UtteranceProgressListener(){
      override fun onStart(id:String?){}
      override fun onDone(id:String?){engine.shutdown()}
      @Deprecated("legacy") override fun onError(id:String?){unavailable(onUnavailable);engine.shutdown()}
     })
     if(engine.speak(text,TextToSpeech.QUEUE_FLUSH,null,"care")==TextToSpeech.ERROR){unavailable(onUnavailable);engine.shutdown()}
    }
   }
  }
 }
}
