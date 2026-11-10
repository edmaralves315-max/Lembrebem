package org.lembrebem.platform
import android.Manifest
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.lembrebem.*
import org.lembrebem.data.*
import org.lembrebem.domain.Rules

class BatteryReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){val level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);val scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);if(level<0||scale<=0)return
 val percent=level*100/scale;val plugged=intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0
 val prefs=context.getSharedPreferences("battery",Context.MODE_PRIVATE);val previous=prefs.getInt("level",101);val oldCharging=prefs.getBoolean("charging",false)
 prefs.edit().putInt("level",percent).putBoolean("charging",plugged).apply()
 val crossings=Rules.batteryCrossings(previous,percent,plugged)
 val app=context.applicationContext as LembreBemApp
 crossings.forEach{threshold->Alerts.show(context,R.string.battery_title,if(threshold==15)R.string.battery_critical else R.string.battery_low,threshold);Voice(context).speak(localizedContext(context).getString(if(threshold==15)R.string.battery_critical else R.string.battery_low))}
 if(crossings.isNotEmpty()||oldCharging!=plugged)app.scope.launch{app.repo.event("battery",JSONObject().put("level",percent).put("charging",plugged).put("thresholds",org.json.JSONArray(crossings)))}
}}
class SafeArea(private val context:Context){
 private val client=LocationServices.getGeofencingClient(context)
 private val pi get()=PendingIntent.getBroadcast(context,98,Intent(context,GeofenceReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
 suspend fun enable(config:GeofenceConfig){require(config.radius>=100);require(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
 val profile=Store.get(context).dao().profile();require(profile?.locationConsent==true)
 val fence=Geofence.Builder().setRequestId("home").setCircularRegion(config.latitude,config.longitude,config.radius).setExpirationDuration(Geofence.NEVER_EXPIRE).setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT).setNotificationResponsiveness(120000).build()
 client.addGeofences(GeofencingRequest.Builder().addGeofence(fence).setInitialTrigger(0).build(),pi).await();Store.get(context).dao().geofence(config.copy(enabled=true))
 }
 suspend fun disable(){client.removeGeofences(pi).await();Store.get(context).dao().geofence()?.let{Store.get(context).dao().geofence(it.copy(enabled=false))}}
}
class GeofenceReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){val event=GeofencingEvent.fromIntent(intent)?:return;if(event.hasError()||event.geofenceTransition!=Geofence.GEOFENCE_TRANSITION_EXIT)return
 val pending=goAsync();val app=context.applicationContext as LembreBemApp;app.scope.launch{try{val p=app.repo.dao.profile();if(p?.locationConsent!=true)return@launch
 val location=event.triggeringLocation;val payload=JSONObject().put("detectedAt",java.time.Instant.now().toString())
 if(location!=null){val saved=LocationEvent(latitude=location.latitude,longitude=location.longitude,accuracy=location.accuracy,at=java.time.Instant.ofEpochMilli(location.time).toString());app.repo.dao.location(saved);payload.put("locationUnavailable",false).put("latitude",saved.latitude).put("longitude",saved.longitude).put("accuracy",saved.accuracy).put("locationAt",saved.at)}else payload.put("locationUnavailable",true)
 app.repo.event("geofence_exit",payload);Alerts.show(context,R.string.help,R.string.safe_question,99);Voice(context).speak(localizedContext(context).getString(R.string.safe_question))
 }finally{pending.finish()}}}}
