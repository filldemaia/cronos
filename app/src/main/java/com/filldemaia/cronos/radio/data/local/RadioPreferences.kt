package com.filldemaia.cronos.radio.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.serialization.json.Json

val Context.radioDataStore by preferencesDataStore(name = "radio_catalana")

object PreferenceKeys {
    val SETTINGS = stringPreferencesKey("settings_json")
    val SCHEDULES = stringPreferencesKey("schedules_json")
    val CUSTOM_STATIONS = stringPreferencesKey("custom_stations_json")
    val ALARM_LOGS = stringPreferencesKey("alarm_logs_json")
    val OEM_ACK = booleanPreferencesKey("oem_auto_start_ack")
    val ONBOARDED = booleanPreferencesKey("onboarding_completed")
}

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}
