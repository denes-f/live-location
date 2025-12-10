package hu.denesf.locationtracker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Top-level extension for DataStore
val Context.settingsDataStore by preferencesDataStore(name = "settings")

object SettingsKeys {
    val DEVICE_ID = stringPreferencesKey("device_id")
    val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
}

data class Settings(
    val deviceId: String?,
    val trackingEnabled: Boolean
)

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<Settings> =
        context.settingsDataStore.data.map { prefs ->
            Settings(
                deviceId = prefs[SettingsKeys.DEVICE_ID],
                trackingEnabled = prefs[SettingsKeys.TRACKING_ENABLED] ?: false
            )
        }

    suspend fun setDeviceId(deviceId: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.DEVICE_ID] = deviceId
        }
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.TRACKING_ENABLED] = enabled
        }
    }
}