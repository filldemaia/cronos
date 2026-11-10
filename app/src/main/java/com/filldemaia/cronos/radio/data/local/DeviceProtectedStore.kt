package com.filldemaia.cronos.radio.data.local

import android.content.Context
import android.os.Build
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

    fun saveSchedules(schedules: List<Schedule>) {
        write(SCHEDULES_FILE, AppJson.encodeToString(ListSerializer(Schedule.serializer()), schedules))
    }

    fun loadSchedules(): List<Schedule> = read(SCHEDULES_FILE)?.let { raw ->
        runCatching {
            AppJson.decodeFromString(ListSerializer(Schedule.serializer()), raw)
        }.getOrNull()
    } ?: emptyList()

    fun updateSchedule(schedule: Schedule) {
        val all = loadSchedules().toMutableList()
        val index = all.indexOfFirst { it.id == schedule.id }
        if (index >= 0) all[index] = schedule else all.add(schedule)
        saveSchedules(all)
    }

    fun removeSchedule(id: String) {
        saveSchedules(loadSchedules().filterNot { it.id == id })
    }

    fun saveStations(stations: List<Station>) {
        write(STATIONS_FILE, AppJson.encodeToString(ListSerializer(Station.serializer()), stations))
    }

    fun loadStations(): List<Station> = read(STATIONS_FILE)?.let { raw ->
        runCatching {
            AppJson.decodeFromString(ListSerializer(Station.serializer()), raw)
        }.getOrNull()
    } ?: emptyList()

    fun saveSettings(settings: AppSettings) {
        write(SETTINGS_FILE, AppJson.encodeToString(AppSettings.serializer(), settings))
    }

    fun loadSettings(): AppSettings? = read(SETTINGS_FILE)?.let { raw ->
        runCatching { AppJson.decodeFromString<AppSettings>(raw) }.getOrNull()
    }

    @Synchronized
    private fun write(name: String, value: String) {
        runCatching {
            val dir = filesDir
            val target = File(dir, name)
            val temporary = File(dir, "$name.tmp")
            temporary.writeText(value)
            if (!temporary.renameTo(target)) {
                target.writeText(value)
                temporary.delete()
            }
        }
    }

    private fun read(name: String): String? = runCatching {
        File(filesDir, name).takeIf { it.exists() }?.readText()
    }.getOrNull()

    private companion object {
        const val SCHEDULES_FILE = "alarms_device_protected.json"
        const val STATIONS_FILE = "stations_device_protected.json"
        const val SETTINGS_FILE = "settings_device_protected.json"
    }
}
