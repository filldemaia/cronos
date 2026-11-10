package com.filldemaia.cronos.radio.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.filldemaia.cronos.radio.data.local.AppJson
import com.filldemaia.cronos.radio.data.local.DeviceProtectedStore
import com.filldemaia.cronos.radio.data.local.PreferenceKeys
import com.filldemaia.cronos.radio.data.local.radioDataStore
import com.filldemaia.cronos.radio.data.model.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ScheduleRepository(
    private val context: Context,
    private val deviceProtectedStore: DeviceProtectedStore
) {

    val schedules: Flow<List<Schedule>> = context.radioDataStore.data.map { prefs ->
        decode(prefs[PreferenceKeys.SCHEDULES])
    }

    suspend fun current(): List<Schedule> = schedules.first()

    suspend fun active(): List<Schedule> = current().filter { it.enabled }

    suspend fun find(id: String): Schedule? = current().firstOrNull { it.id == id }

    suspend fun save(schedule: Schedule) {
        var updated: List<Schedule> = emptyList()
        context.radioDataStore.edit { prefs ->
            val all = decode(prefs[PreferenceKeys.SCHEDULES]).toMutableList()
            val index = all.indexOfFirst { it.id == schedule.id }
            if (index >= 0) all[index] = schedule else all.add(schedule)
            all.sortWith(compareBy({ it.hour }, { it.minute }, { it.createdAtMillis }))
            updated = all
            prefs[PreferenceKeys.SCHEDULES] = encode(all)
        }
        mirror(updated)
    }

    suspend fun delete(id: String) {
        var updated: List<Schedule> = emptyList()
        context.radioDataStore.edit { prefs ->
            updated = decode(prefs[PreferenceKeys.SCHEDULES]).filterNot { it.id == id }
            prefs[PreferenceKeys.SCHEDULES] = encode(updated)
        }
        mirror(updated)
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        val schedule = find(id) ?: return
        save(schedule.copy(enabled = enabled))
    }

    suspend fun disableOneShot(id: String) {
        val schedule = find(id) ?: return
        if (schedule.isOneShot) save(schedule.copy(enabled = false))
    }

    /**
     * Manté la còpia a l'emmagatzematge protegida pel dispositiu. És el que
     * permet rearmar les alarmes just després d'un reinici, abans que l'usuari
     * desbloquegi el telèfon.
     */
    private suspend fun mirror(schedules: List<Schedule>) {
        withContext(Dispatchers.IO) {
            runCatching { deviceProtectedStore.saveSchedules(schedules) }
        }
    }

    private fun encode(all: List<Schedule>): String =
        AppJson.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Schedule.serializer()),
            all
        )

    private fun decode(raw: String?): List<Schedule> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            AppJson.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(Schedule.serializer()),
                raw
            )
        }.getOrElse { emptyList() }
    }
}
