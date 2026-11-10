package org.lembrebem
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import androidx.room.Room
import org.lembrebem.data.*
@RunWith(AndroidJUnit4::class) class OfflineTest {
 @Test fun acknowledgementAndAuditPersistOffline()=runBlocking {val context=InstrumentationRegistry.getInstrumentation().targetContext;val db=Room.inMemoryDatabaseBuilder(context,Store::class.java).build();val repo=Repository(db);val dose=Dose("demo:2026-11-09T08:00:00Z","demo","2026-11-09T08:00:00Z");db.dao().insertDose(dose);repo.acknowledge(dose,"taken");Assert.assertEquals("taken",db.dao().dose(dose.id)?.status);Assert.assertNotNull(db.dao().dose(dose.id)?.acknowledgedAt);Assert.assertEquals(1,db.dao().pending().size);Assert.assertEquals(1,db.dao().allAudits().size);db.close()}
}
