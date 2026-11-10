package org.lembrebem.data
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.lembrebem.clinical.ClinicalStorage
import android.content.Context

/** Explicit SAF export only; never called by synchronization. */
suspend fun exportLocal(context:Context):JSONObject {
 val dao=Store.get(context).dao();val p=dao.profile()
 return JSONObject().put("schemaVersion",1).put("exportedAt",java.time.Instant.now().toString())
 .put("profile",p?.let{JSONObject().put("id",it.id).put("name",it.name).put("preferredLocale",it.preferredLocale).put("countryCode",it.countryCode).put("timeZone",it.timeZone).put("healthConsent",it.healthConsent).put("locationConsent",it.locationConsent)})
 .put("medicationPlans",JSONArray(dao.allPlans().map{JSONObject().put("id",it.id).put("name",it.name).put("presentation",it.presentation).put("dosage",it.dosage).put("time",it.time).put("days",it.days).put("timeZone",it.timeZone).put("startDate",it.startDate).put("endDate",it.endDate).put("notes",it.notes).put("source",it.source).put("approved",it.approved).put("version",it.version)}))
 .put("doseEvents",JSONArray(dao.doses().first().map{JSONObject().put("id",it.id).put("planId",it.planId).put("scheduledAt",it.scheduledAt).put("status",it.status).put("acknowledgedAt",it.acknowledgedAt)}))
 .put("appointments",JSONArray(dao.allAppointments().map{JSONObject().put("id",it.id).put("title",it.title).put("at",it.at).put("place",it.place).put("preparation",it.preparation).put("documents",it.documents).put("reminderMinutes",it.reminderMinutes)}))
 .put("contacts",JSONArray(dao.allContacts().map{JSONObject().put("id",it.id).put("label",it.label).put("phone",it.phone).put("relation",it.relation).put("photoUri",it.photoUri)}))
 .put("lastLocation",dao.lastLocation().first()?.let{JSONObject().put("latitude",it.latitude).put("longitude",it.longitude).put("accuracy",it.accuracy).put("at",it.at)})
 .put("audit",JSONArray(dao.allAudits().map{JSONObject().put("id",it.id).put("at",it.at).put("action",it.action).put("actor",it.actor).put("targetId",it.targetId).put("version",it.version)}))
 .put("clinicalWorkingText",runCatching{ClinicalStorage(context).workingText()}.getOrDefault(""))
}
