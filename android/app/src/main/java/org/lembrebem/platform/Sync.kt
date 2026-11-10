package org.lembrebem.platform
import android.content.Context
import androidx.work.*
import androidx.datastore.preferences.core.*
import com.google.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.json.*
import org.lembrebem.*
import org.lembrebem.data.*
import org.lembrebem.domain.Rules
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object RemoteAuth{
 fun configure(context:Context,apiKey:String,appId:String,projectId:String){if(FirebaseApp.getApps(context).isEmpty())FirebaseApp.initializeApp(context,FirebaseOptions.Builder().setApiKey(apiKey).setApplicationId(appId).setProjectId(projectId).build())}
 suspend fun signIn(email:String,password:String,create:Boolean){val auth=FirebaseAuth.getInstance();if(create)auth.createUserWithEmailAndPassword(email,password).await() else auth.signInWithEmailAndPassword(email,password).await()}
 fun ready(context:Context)=FirebaseApp.getApps(context).isNotEmpty()
 suspend fun token(context:Context):String?=if(ready(context))FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token else null
}
class ApiException(val status:Int):java.io.IOException("HTTP $status")
class Api(private val base:String,private val token:String){
 fun request(path:String,body:JSONObject?=null,method:String=if(body==null)"GET" else "POST"):JSONObject{
 require(base.startsWith("https://"));val c=URL(base.trimEnd('/')+"/v1"+path).openConnection() as HttpsURLConnection
 try{c.connectTimeout=15000;c.readTimeout=15000;c.requestMethod=method;c.setRequestProperty("Authorization","Bearer $token");c.setRequestProperty("Content-Type","application/json")
 if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toString().toByteArray())}}
 val status=c.responseCode;if(status !in 200..299)throw ApiException(status);val text=c.inputStream.bufferedReader().use{it.readText()};return if(text.isBlank())JSONObject() else JSONObject(text)
 }finally{c.disconnect()}
 }
}
class SyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){override suspend fun doWork():Result{
 val prefs=applicationContext.settings.data.first();val base=prefs[Keys.ENDPOINT]?:return Result.success();if(base.isBlank())return Result.success()
 val token=RemoteAuth.token(applicationContext)?:return Result.success();val dao=Store.get(applicationContext).dao();val profile=dao.profile()?:return Result.success()
 
 return try{val api=Api(base,token);val personId=prefs[Keys.REMOTE_PERSON]?:profile.id
 if(prefs[Keys.REMOTE_OWNER]!=false)api.request("/people",JSONObject().put("id",profile.id).put("preferredLocale",profile.preferredLocale).put("countryCode",profile.countryCode).put("timeZone",profile.timeZone).put("consents",JSONObject().put("health",profile.healthConsent).put("location",profile.locationConsent)))
 if(prefs[Keys.REMOTE_OWNER]!=false)api.request("/people/${personId}",JSONObject().put("preferredLocale",profile.preferredLocale).put("countryCode",profile.countryCode).put("timeZone",profile.timeZone).put("consents",JSONObject().put("health",profile.healthConsent).put("location",profile.locationConsent)).put("heartbeatMinutes",120),"PUT")
 var rejected=false
 var retryPending=false
 for(event in dao.pending()){
  // Categories are independent. Withheld data remains local and is never sent implicitly.
  if(!Rules.maySync(event.type,profile.healthConsent,profile.locationConsent,prefs[Keys.REMOTE_OWNER]!=false))continue
  try{
   if(event.type=="plan")api.request("/people/${personId}/plans",JSONObject(event.payload))
   else api.request("/people/${personId}/events",JSONObject().put("id",event.id).put("type",event.type).put("occurredAt",event.occurredAt).put("payload",JSONObject(event.payload)))
   dao.sent(event.id)
  }catch(error:ApiException){
   if(error.status==401)throw error
   if(Rules.retryHttp(error.status)){retryPending=true;continue}
   // A permanent failure must not block unrelated safety events. Keep an audit,
   // not clinical payload or coordinates, and retain the original local outbox row.
   dao.audit(Audit(action="sync_rejected_${error.status}",targetId=event.id))
   dao.sent(event.id);rejected=true
  }
 }

 if(prefs[Keys.REMOTE_OWNER]!=false)api.request("/people/${personId}/heartbeat",JSONObject().put("occurredAt",java.time.Instant.now().toString()))
 // Commit a page cursor only after every event in the page has been applied.
 val cursorStore=applicationContext.getSharedPreferences("sync_cursors",0)
 val uid=FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
 val cursorKey=listOf(base,personId,uid,profile.healthConsent.toString(),profile.locationConsent.toString(),prefs[Keys.REMOTE_OWNER].toString()).joinToString("|")
 var cursor=cursorStore.getString(cursorKey,null)
 var pages=0
 var more:Boolean
 do {
 val query=cursor?.let{"?cursor="+java.net.URLEncoder.encode(it,"UTF-8")}.orEmpty()
 val pulled=api.request("/people/${personId}/sync$query")
 val plans=pulled.optJSONArray("plans")?:JSONArray()
 for(i in 0 until plans.length()){val data=plans.getJSONObject(i).getJSONObject("data");val id=data.getString("id");val old=dao.plan(id);val version=data.getInt("version");if(old==null||version>old.version){dao.plan(MedicationPlan(id=id,name=data.getString("name"),dosage=data.getString("dosage"),time=data.getString("time"),days=(0 until data.getJSONArray("days").length()).joinToString(","){data.getJSONArray("days").getInt(it).toString()},startDate=data.getString("startDate"),endDate=data.optString("endDate","").takeUnless{it=="null"}.orEmpty(),timeZone=data.getString("timeZone"),source="remote-human-reviewed",approved=true,version=version))}}
 val events=pulled.optJSONArray("events")?:JSONArray();for(i in 0 until events.length()){val e=events.getJSONObject(i);val payload=e.getJSONObject("payload");if(e.getString("type")=="dose_ack"){val d=dao.dose(payload.getString("doseId"));if(d!=null&&(d.acknowledgedAt==null||e.getString("occurredAt")>d.acknowledgedAt))dao.updateDose(d.copy(status=payload.getString("status"),acknowledgedAt=e.getString("occurredAt")))};if(e.getString("type")=="appointment")dao.appointment(Appointment(id=payload.getString("id"),title=payload.getString("title"),at=payload.getString("at"),place=payload.optString("place"),preparation=payload.optString("preparation"),documents=payload.optString("documents")));if(e.getString("type")=="geofence_exit"&&!payload.optBoolean("locationUnavailable",false)&&payload.has("latitude")&&payload.has("longitude"))dao.location(LocationEvent(id=e.getString("id"),latitude=payload.getDouble("latitude"),longitude=payload.getDouble("longitude"),accuracy=payload.getDouble("accuracy").toFloat(),at=payload.optString("timestamp",payload.optString("locationAt"))))}
 cursor=pulled.optString("nextCursor", "").takeUnless{it.isBlank()||it=="null"}
 if(cursor!=null)cursorStore.edit().putString(cursorKey,cursor).apply()
 more=pulled.optBoolean("hasMore",false)
 check(!more||cursor!=null){"sync cursor missing"}
 pages++
 } while(more&&pages<10)
 if(more)retryPending=true
 if(RemoteAuth.ready(applicationContext)){try{val fcm=com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await();api.request("/subscriptions",JSONObject().put("token",fcm).put("preferredLocale",profile.preferredLocale))}catch(_:Exception){}}
 AlarmScheduler(applicationContext).restore()
 applicationContext.getSharedPreferences("sync",0).edit().putString("status",if(rejected)"rejected" else if(retryPending)"error" else "ok").putString("at",java.time.Instant.now().toString()).apply();if(retryPending)Result.retry()else Result.success()
 }catch(e:Exception){applicationContext.getSharedPreferences("sync",0).edit().putString("status","error").apply();Result.retry()}
}}
class PushService:FirebaseMessagingService(){override fun onMessageReceived(message:RemoteMessage){Alerts.show(this,R.string.app_name,R.string.private_alert,101)}
 override fun onNewToken(token:String){getSharedPreferences("push",0).edit().putString("token",token).apply()}
}
