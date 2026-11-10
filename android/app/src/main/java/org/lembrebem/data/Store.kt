package org.lembrebem.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.time.*
import java.util.UUID

@Entity data class Profile(@PrimaryKey val id:String=UUID.randomUUID().toString(),val name:String="",val preferredLocale:String="pt-BR",val countryCode:String="BR",val timeZone:String=ZoneId.systemDefault().id,val healthConsent:Boolean=false,val locationConsent:Boolean=false)
@Entity data class MedicationPlan(@PrimaryKey val id:String=UUID.randomUUID().toString(),val name:String,val presentation:String="",val dosage:String,val time:String,val days:String="1,2,3,4,5,6,7",val startDate:String=LocalDate.now().toString(),val endDate:String="",val timeZone:String=ZoneId.systemDefault().id,val notes:String="",val source:String,val approved:Boolean=false,val version:Int=1)
@Entity(indices=[Index(value=["planId","scheduledAt"],unique=true)]) data class Dose(@PrimaryKey val id:String,val planId:String,val scheduledAt:String,val status:String="pending",val acknowledgedAt:String?=null)
@Entity data class Appointment(@PrimaryKey val id:String=UUID.randomUUID().toString(),val title:String,val at:String,val place:String,val preparation:String="",val documents:String="",val reminderMinutes:Int=30)
@Entity data class ContactCard(@PrimaryKey val id:String=UUID.randomUUID().toString(),val label:String,val phone:String,val relation:String="",val photoUri:String="")
@Entity data class Audit(@PrimaryKey val id:String=UUID.randomUUID().toString(),val action:String,val targetId:String,val at:String=Instant.now().toString(),val actor:String="local-human",val version:Int=1)
@Entity data class Outbox(@PrimaryKey val id:String=UUID.randomUUID().toString(),val type:String,val occurredAt:String=Instant.now().toString(),val payload:String,val sent:Boolean=false)
@Entity data class GeofenceConfig(@PrimaryKey val id:Int=1,val latitude:Double,val longitude:Double,val radius:Float,val enabled:Boolean=false)
@Entity data class LocationEvent(@PrimaryKey val id:String=UUID.randomUUID().toString(),val latitude:Double,val longitude:Double,val accuracy:Float,val at:String=Instant.now().toString())
@Dao interface StoreDao {
 @Query("SELECT * FROM Profile LIMIT 1") suspend fun profile():Profile?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun profile(value:Profile)
 @Query("SELECT * FROM MedicationPlan ORDER BY time") fun plans():Flow<List<MedicationPlan>>
 @Query("SELECT * FROM MedicationPlan") suspend fun allPlans():List<MedicationPlan>
 @Query("SELECT * FROM MedicationPlan WHERE id=:id") suspend fun plan(id:String):MedicationPlan?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun plan(value:MedicationPlan)
 @Query("SELECT * FROM Dose ORDER BY scheduledAt DESC") fun doses():Flow<List<Dose>>
 @Query("SELECT * FROM Dose WHERE id=:id") suspend fun dose(id:String):Dose?
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertDose(value:Dose)
 @Update suspend fun updateDose(value:Dose)
 @Query("SELECT * FROM Appointment ORDER BY at") fun appointments():Flow<List<Appointment>>
 @Query("SELECT * FROM Appointment") suspend fun allAppointments():List<Appointment>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun appointment(value:Appointment)
 @Query("SELECT * FROM ContactCard") fun contacts():Flow<List<ContactCard>>
 @Query("SELECT * FROM ContactCard") suspend fun allContacts():List<ContactCard>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun contact(value:ContactCard)
 @Insert suspend fun audit(value:Audit)
 @Query("SELECT * FROM Audit ORDER BY at DESC") fun audits():Flow<List<Audit>>
 @Insert suspend fun enqueue(value:Outbox)
 @Query("SELECT * FROM Outbox WHERE sent=0 ORDER BY occurredAt") suspend fun pending():List<Outbox>
 @Query("UPDATE Outbox SET sent=1 WHERE id=:id") suspend fun sent(id:String)
 @Query("SELECT * FROM GeofenceConfig LIMIT 1") suspend fun geofence():GeofenceConfig?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun geofence(value:GeofenceConfig)
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun location(value:LocationEvent)
 @Query("SELECT * FROM LocationEvent ORDER BY at DESC LIMIT 1") fun lastLocation():Flow<LocationEvent?>
 @Query("SELECT * FROM Audit") suspend fun allAudits():List<Audit>
}
@Database(entities=[Profile::class,MedicationPlan::class,Dose::class,Appointment::class,ContactCard::class,Audit::class,Outbox::class,GeofenceConfig::class,LocationEvent::class],version=1,exportSchema=true)
abstract class Store:RoomDatabase(){ abstract fun dao():StoreDao
 companion object { @Volatile private var instance:Store?=null
 fun get(context:Context):Store=instance?:synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,Store::class.java,"lembrebem.db").build().also{instance=it}}
 }
}
class Repository(private val db:Store){ val dao=db.dao()
 suspend fun seed(){if(dao.profile()==null){dao.profile(Profile(name="Pessoa demonstração")); dao.contact(ContactCard(label="Família demonstração",phone="",relation="demo"))}}
 suspend fun savePlan(p:MedicationPlan){require(p.approved && p.source.isNotBlank()); LocalTime.parse(p.time); ZoneId.of(p.timeZone); require(p.name.isNotBlank()&&p.dosage.isNotBlank());db.withTransaction{dao.plan(p);dao.audit(Audit(action="plan_approved",targetId=p.id,version=p.version));dao.enqueue(Outbox(type="plan",payload=JSONObject().put("id",p.id).put("name",p.name).put("dosage",p.dosage).put("version",p.version).put("approved",true).put("sourceReviewed",true).put("time",p.time).put("days",org.json.JSONArray(p.days.split(',').map{it.toInt()})).put("timeZone",p.timeZone).put("startDate",p.startDate).put("endDate",p.endDate.ifBlank{null}).toString()))}}
 suspend fun acknowledge(d:Dose,status:String){require(status in listOf("taken","not_yet","snoozed"));val now=Instant.now().toString(); db.withTransaction{dao.updateDose(d.copy(status=status,acknowledgedAt=now));dao.audit(Audit(action="dose_$status",targetId=d.id));dao.enqueue(Outbox(type="dose_ack",occurredAt=now,payload=JSONObject().put("doseId",d.id).put("scheduledAt",d.scheduledAt).put("status",status).toString()))}}
 suspend fun event(type:String,payload:JSONObject){dao.enqueue(Outbox(type=type,payload=payload.toString()))}
}
