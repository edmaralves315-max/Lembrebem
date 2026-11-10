package org.lembrebem
import org.junit.Test
import org.junit.Assert.*
import org.lembrebem.domain.Rules
import java.time.*
class RulesTest {
 @Test fun reminder0800Offline(){val next=Rules.nextDose("08:00",(1..7).toSet(),LocalDate.parse("2026-11-01"),null,ZoneId.of("America/Sao_Paulo"),Instant.parse("2026-11-09T10:00:00Z"));assertEquals(Instant.parse("2026-11-09T11:00:00Z"),next)}
 @Test fun dstGapUsesZoneRules(){val next=Rules.nextDose("02:30",(1..7).toSet(),LocalDate.parse("2026-03-01"),null,ZoneId.of("America/New_York"),Instant.parse("2026-03-08T05:00:00Z"));assertEquals(Instant.parse("2026-03-08T07:30:00Z"),next)}
 @Test fun endDateStopsSchedule(){assertNull(Rules.nextDose("08:00",(1..7).toSet(),LocalDate.parse("2026-01-01"),LocalDate.parse("2026-01-02"),ZoneId.of("UTC"),Instant.parse("2026-11-01T00:00:00Z")))}
 @Test fun batteryCrossingsDeduplicate(){assertEquals(listOf(30),Rules.batteryCrossings(31,30,false));assertEquals(emptyList<Int>(),Rules.batteryCrossings(30,29,false));assertEquals(listOf(15),Rules.batteryCrossings(16,15,false));assertEquals(emptyList<Int>(),Rules.batteryCrossings(31,14,true))}
 @Test fun idStableAcrossLocale(){val at=Instant.parse("2026-11-09T08:00:00Z");assertEquals("plan:2026-11-09T08:00:00.000Z",Rules.doseId("plan",at))}
 @Test fun healthConsentDoesNotDisableIndependentSafetyCategories(){
  for(type in listOf("battery","help","connectivity","geofence_exit"))assertTrue(Rules.maySync(type,false,true,true))
  for(type in listOf("plan","appointment","dose_ack"))assertFalse(Rules.maySync(type,false,true,true))
  assertFalse(Rules.maySync("geofence_exit",true,false,true))
 }
 @Test fun caregiverDoesNotUploadOwnDeviceMonitoring(){
  for(type in listOf("battery","help","geofence_exit"))assertFalse(Rules.maySync(type,true,true,false))
  assertTrue(Rules.maySync("dose_ack",true,false,false))
 }
 @Test fun permanentHttpRejectionsDoNotRequestInfiniteRetry(){
  for(status in listOf(400,403,409,422))assertFalse(Rules.retryHttp(status))
  for(status in listOf(408,429,500,503))assertTrue(Rules.retryHttp(status))
 }
 @Test fun takenDoseSuppressesCurrentAnnouncementButHasNextRecurrence(){
  assertFalse(Rules.shouldAnnounceDose("taken"))
  val next=Rules.nextDose("08:00",(1..7).toSet(),LocalDate.parse("2026-11-01"),null,ZoneId.of("UTC"),Instant.parse("2026-11-09T08:00:01Z"))
  assertEquals(Instant.parse("2026-11-10T08:00:00Z"),next)
 }
}
