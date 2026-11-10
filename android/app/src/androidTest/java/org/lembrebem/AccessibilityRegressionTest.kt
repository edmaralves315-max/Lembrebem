package org.lembrebem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.lembrebem.data.*
import org.lembrebem.ui.LembreBem

/** Instrumented acceptance tests; run on emulator/device, not counted as JVM validation. */
class AccessibilityRegressionTest {
 @get:Rule val compose=createComposeRule()
 @Test fun helpRemainsVisibleAtDoubleFontScaleWithoutScrolling(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  runBlocking{context.settings.edit{it[Keys.MODE]="assisted"}}
  compose.setContent{CompositionLocalProvider(LocalDensity provides Density(1f,2f)){MaterialTheme{Box(androidx.compose.ui.Modifier.width(360.dp).height(640.dp)){LembreBem()}}}}
  compose.onNodeWithText(context.getString(R.string.help)).assertIsDisplayed().performClick()
  compose.onNodeWithText(context.getString(R.string.help_guidance)).assertIsDisplayed()
 }
 @Test fun notificationDestinationShowsSpecificDoseWithoutSelectingMenu(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val id="30000000-0000-4000-8000-000000000099"
  val doseId="$id:2026-11-10T08:00:00.000Z"
  runBlocking{
   context.settings.edit{it[Keys.MODE]="assisted"}
   Store.get(context).dao().plan(MedicationPlan(id=id,name="Fictício teste",dosage="Texto fictício",time="08:00",source="teste humano",approved=true))
   Store.get(context).dao().insertDose(Dose(doseId,id,"2026-11-10T08:00:00.000Z"))
  }
  compose.setContent{MaterialTheme{LembreBem(doseId)}}
  compose.onNodeWithText("Fictício teste").assertIsDisplayed()
  compose.onNodeWithText(context.getString(R.string.taken)).assertExists()
  compose.onNodeWithText(context.getString(R.string.not_yet)).assertExists()
  compose.onNodeWithText(context.getString(R.string.snooze)).assertExists()
 }
}
