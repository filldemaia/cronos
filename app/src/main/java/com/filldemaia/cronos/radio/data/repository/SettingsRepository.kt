package com.filldemaia.cronos.radio.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.filldemaia.cronos.radio.data.local.AppJson
import com.filldemaia.cronos.radio.data.local.DeviceProtectedStore
import com.filldemaia.cronos.radio.data.local.PreferenceKeys
import com.filldemaia.cronos.radio.data.local.radioDataStore
import com.filldemaia.cronos.radio.data.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SettingsRepository(
    private val context: Context,
    private val deviceProtectedStore: DeviceProtectedStore
) {

    val settings: Flow<AppSettings> = context.radioDataStore.data.map { prefs ->
        prefs[PreferenceKeys.SETTINGS]?.let { raw ->
            runCatching { AppJson.decodeFromString<AppSettings>(raw) }.getOrNull()
        } ?: AppSettings()
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun save(value: AppSettings) {
        context.radioDataStore.edit { prefs ->
            prefs[PreferenceKeys.SETTINGS] = AppJson.encodeToString(AppSettings.serializer(), value)
        }
        // Disponible abans del desbloqueig per si l'alarma arrenca en direct boot.
        withContext(Dispatchers.IO) {
            runCatching { deviceProtectedStore.saveSettings(value) }
        }
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        save(transform(current()))
    }
}
