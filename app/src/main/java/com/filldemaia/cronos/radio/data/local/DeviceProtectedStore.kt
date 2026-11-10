package com.filldemaia.cronos.radio.data.local

import android.content.Context
import android.os.Build
import android.util.Log
import com.filldemaia.cronos.radio.data.model.AppSettings
import com.filldemaia.cronos.radio.data.model.Schedule
import com.filldemaia.cronos.radio.data.model.Station
import java.io.File
import kotlinx.serialization.builtins.ListSerializer

/**
 * Còpia de seguretat de les alarmes a l'emmagatzematge protegit pel dispositiu
 * (device-protected), que és accessible abans del primer desbloqueig.
 *
 * DataStore viu a l'emmagatzematge xifrat per credencials (CE) i, per tant, no
 * es pot llegir fins que l'usuari desbloqueja el mòbil. Sense aquesta còpia, un
 * reinici nocturn esborrava les alarmes de l'AlarmManager i l'app no podia
 * rearmar-les fins al matí, quan l'usuari desbloquejava el telèfon.
 *
 * Les escriptures són petites i atòmiques (fitxer temporal + rename) perquè el
 * receptor de LOCKED_BOOT_COMPLETED sempre trobi un JSON sencer.
 */
class DeviceProtectedStore(context: Context) {

    private val writes = SerializedProtectedWrites { name, error ->
        Log.e("DeviceProtectedStore", "Error escrivint $name", error)
    }

    fun writeErrors(): Map<String, String> = writes.errors()

    private val storage: Context =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }

    val filesDir: File
        get() = storage.filesDir.also { runCatching { it.mkdirs() } }

    val cacheDir: File
        get() = storage.cacheDir.also { runCatching { it.mkdirs() } }

    suspend fun saveSchedules(schedules: List<Schedule>) {
        write(SCHEDULES_FILE, AppJson.encodeToString(ListSerializer(Schedule.serializer()), schedules))
    }

    fun loadSchedules(): List<Schedule> = read(SCHEDULES_FILE)?.let { raw ->
        runCatching {
            AppJson.decodeFromString(ListSerializer(Schedule.serializer()), raw)
        }.getOrNull()
    } ?: emptyList()

    suspend fun updateSchedule(schedule: Schedule) {
        writes.write(SCHEDULES_FILE) {
            val all = loadSchedules().toMutableList()
            val index = all.indexOfFirst { it.id == schedule.id }
            if (index >= 0) all[index] = schedule else all.add(schedule)
            writeFile(SCHEDULES_FILE, AppJson.encodeToString(ListSerializer(Schedule.serializer()), all))
        }
    }

    suspend fun removeSchedule(id: String) {
        writes.write(SCHEDULES_FILE) {
            val all = loadSchedules().filterNot { it.id == id }
            writeFile(SCHEDULES_FILE, AppJson.encodeToString(ListSerializer(Schedule.serializer()), all))
        }
    }

    suspend fun saveStations(stations: List<Station>) {
        write(STATIONS_FILE, AppJson.encodeToString(ListSerializer(Station.serializer()), stations))
    }

    fun loadStations(): List<Station> = read(STATIONS_FILE)?.let { raw ->
        runCatching {
            AppJson.decodeFromString(ListSerializer(Station.serializer()), raw)
        }.getOrNull()
    } ?: emptyList()

    suspend fun saveSettings(settings: AppSettings) {
        write(SETTINGS_FILE, AppJson.encodeToString(AppSettings.serializer(), settings))
    }

    fun loadSettings(): AppSettings? = read(SETTINGS_FILE)?.let { raw ->
        runCatching { AppJson.decodeFromString<AppSettings>(raw) }.getOrNull()
    }

    private suspend fun write(name: String, value: String) = writes.write(name) { writeFile(name, value) }

    private fun writeFile(name: String, value: String) = AtomicProtectedFile.write(File(filesDir, name), value)

    private fun read(name: String): String? = runCatching {
        File(filesDir, name).takeIf { it.exists() }?.readText()
    }.getOrNull()

    private companion object {
        const val SCHEDULES_FILE = "alarms_device_protected.json"
        const val STATIONS_FILE = "stations_device_protected.json"
        const val SETTINGS_FILE = "settings_device_protected.json"
    }
}
