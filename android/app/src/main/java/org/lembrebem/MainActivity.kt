package org.lembrebem

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import org.lembrebem.ui.LembreBem
class MainActivity:AppCompatActivity(){private val requestedDose=mutableStateOf<String?>(null);private val requestVersion=mutableStateOf(0)
 override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);requestedDose.value=intent.getStringExtra("doseId");requestVersion.value++}
 override fun onResume(){super.onResume();val app=application as LembreBemApp;app.scope.launch{org.lembrebem.platform.AlarmScheduler(this@MainActivity).restore()}};override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);requestedDose.value=intent.getStringExtra("doseId");setContent{MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF075A43),onPrimary=Color.White,secondary=Color(0xFF144779),background=Color(0xFFF9FBF6),onBackground=Color(0xFF14231C),surface=Color(0xFFF9FBF6),onSurface=Color(0xFF14231C))){LembreBem(requestedDose.value,requestVersion.value)}}}}
