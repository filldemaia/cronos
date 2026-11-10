package com.filldemaia.cronos.radio.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.filldemaia.cronos.radio.data.local.AppJson
import com.filldemaia.cronos.radio.data.local.PreferenceKeys
import com.filldemaia.cronos.radio.data.local.radioDataStore
import com.filldemaia.cronos.radio.data.model.AlarmLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer

class ReliabilityRepository(private val context: Context) {

    val oemAcknowledged: Flow<Boolean> = context.radioDataStore.data.map { prefs ->
        prefs[PreferenceKeys.OEM_ACK] ?: false
    }

    val onboarded: Flow<Boolean> = context.radioDataStore.data.map { prefs ->
        prefs[PreferenceKeys.ONBOARDED] ?: false
    }

    val logs: Flow<List<AlarmLog>> = context.radioDataStore.data.map { prefs ->
        decodeLogs(prefs[PreferenceKeys.ALARM_LOGS])
    }

    suspend fun currentOemAcknowledged(): Boolean = oemAcknowledged.first()

    suspend fun setOemAcknowledged(value: Boolean) {
        context.radioDataStore.edit { it[PreferenceKeys.OEM_ACK] = value }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.radioDataStore.edit { it[PreferenceKeys.ONBOARDED] = value }
    }

    suspend fun currentLogs(): List<AlarmLog> = logs.first()

    suspend fun addLog(log: AlarmLog) {
        context.radioDataStore.edit { prefs ->
            val all = (listOf(log) + decodeLogs(prefs[PreferenceKeys.ALARM_LOGS]))
                .distinctBy { it.id }
                .sortedByDescending { it.actualAtMillis }
                .take(MAX_LOGS)
            prefs[PreferenceKeys.ALARM_LOGS] =
                AppJson.encodeToString(ListSerializer(AlarmLog.serializer()), all)
        }
    }

    suspend fun clearLogs() {
        context.radioDataStore.edit { it[PreferenceKeys.ALARM_LOGS] = "" }
    }

    private fun decodeLogs(raw: String?): List<AlarmLog> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            AppJson.decodeFromString(ListSerializer(AlarmLog.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    private companion object {
        const val MAX_LOGS = 60
    }
}
