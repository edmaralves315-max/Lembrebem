package org.lembrebem.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
val Context.settings by preferencesDataStore("settings")
object Keys {val REMOTE_PERSON=stringPreferencesKey("remote_person");val REMOTE_OWNER=booleanPreferencesKey("remote_owner");val MODE=stringPreferencesKey("mode");val ENDPOINT=stringPreferencesKey("endpoint");val LOCALE=stringPreferencesKey("locale");val COUNTRY=stringPreferencesKey("country");val RETENTION=intPreferencesKey("retention_days")}
