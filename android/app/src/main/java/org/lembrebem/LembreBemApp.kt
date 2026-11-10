package org.lembrebem
import android.app.Application
import android.content.*
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.*
import org.lembrebem.data.*
import org.lembrebem.platform.*
import java.util.concurrent.TimeUnit
class LembreBemApp:Application(){
 val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 val repo by lazy{Repository(Store.get(this))}
 override fun onCreate(){super.onCreate();val firebase=getSharedPreferences("firebase",0);if(firebase.getString("key",null)!=null)try{RemoteAuth.configure(this,firebase.getString("key","")!!,firebase.getString("app","")!!,firebase.getString("project","")!!)}catch(_:Exception){};scope.launch{repo.seed();AlarmScheduler(this).restore()}
 ContextCompat.registerReceiver(this,BatteryReceiver(),IntentFilter(Intent.ACTION_BATTERY_CHANGED),ContextCompat.RECEIVER_NOT_EXPORTED)
 WorkManager.getInstance(this).enqueueUniquePeriodicWork("sync",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<SyncWorker>(15,TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
 }
}
