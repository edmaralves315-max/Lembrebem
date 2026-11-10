package org.lembrebem.domain
import java.time.*
object Rules {
 fun maySync(type:String,healthConsent:Boolean,locationConsent:Boolean,isOwner:Boolean):Boolean {
  if(!isOwner && type !in setOf("plan","appointment","dose_ack"))return false
  return when(type){"plan","appointment","dose_ack"->healthConsent;"geofence_exit"->locationConsent;else->true}
 }
 fun retryHttp(status:Int):Boolean=status==408||status==429||status>=500
 fun shouldAnnounceDose(status:String?):Boolean=status!="taken"
 fun nextDose(time:String,days:Set<Int>,start:LocalDate,end:LocalDate?,zone:ZoneId,now:Instant):Instant? {
  val local=now.atZone(zone).toLocalDate()
  for(offset in 0L..370L){ val date=local.plusDays(offset);if(date<start||end!=null&&date>end||date.dayOfWeek.value !in days)continue
   val candidate=date.atTime(LocalTime.parse(time)).atZone(zone).toInstant();if(candidate>now)return candidate }
  return null
 }
 fun batteryCrossings(previous:Int,current:Int,charging:Boolean):List<Int> = if(charging) emptyList() else listOf(30,15).filter{previous>it&&current<=it}
 fun wireInstant(at:Instant):String=java.time.format.DateTimeFormatterBuilder().appendInstant(3).toFormatter().format(at)
 fun doseId(planId:String,at:Instant):String="$planId:${wireInstant(at)}"
}
