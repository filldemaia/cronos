package com.filldemaia.cronos

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Repositori de preferències de l'usuari. Emmagatzema en
 * SharedPreferences per simplicitat — no cal DataStore per
 * quatre valors. Les propietats es llegeixen directament en
 * cada accés, sense coroutines.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Mostra els segons per escrit a l'hora tradicional (per defecte: false). */
    var showSecondsWritten: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS_WRITTEN, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_SECONDS_WRITTEN, value) }

    /** Mida de l'hora catalana, en sp (per defecte: 27). */
    var hourSize: Float
        get() = prefs.getFloat(KEY_HOUR_SIZE, 27f)
        set(value) = prefs.edit { putFloat(KEY_HOUR_SIZE, value) }

    fun widgetTextSize(type: WidgetType): Float =
        type.normalizeTextSize(prefs.getFloat(type.preferenceKey, type.defaultTextSizeSp))

    fun setWidgetTextSize(type: WidgetType, sizeSp: Float) {
        prefs.edit { putFloat(type.preferenceKey, type.normalizeTextSize(sizeSp)) }
    }

    fun resetWidgets() {
        prefs.edit { WidgetType.entries.forEach { remove(it.preferenceKey) } }
    }

    /** Nom de la ubicació triada (per defecte: Barcelona). */
    var locationCity: String
        get() = prefs.getString(KEY_LOCATION_CITY, Cities.default.name) ?: Cities.default.name
        set(value) = prefs.edit { putString(KEY_LOCATION_CITY, value) }

    /** Latitud de la ubicació (per defecte: Barcelona). */
    var locationLatitude: Double
        get() = prefs.getFloat(KEY_LOCATION_LAT, Cities.default.latitude.toFloat()).toDouble()
        set(value) = prefs.edit { putFloat(KEY_LOCATION_LAT, value.toFloat()) }

    /** Longitud de la ubicació (per defecte: Barcelona). */
    var locationLongitude: Double
        get() = prefs.getFloat(KEY_LOCATION_LON, Cities.default.longitude.toFloat()).toDouble()
        set(value) = prefs.edit { putFloat(KEY_LOCATION_LON, value.toFloat()) }

    /** Desa una ubicació sencera (nom + coordenades). */
    fun setLocation(city: CityLocation) {
        prefs.edit {
            putString(KEY_LOCATION_CITY, city.name)
            putFloat(KEY_LOCATION_LAT, city.latitude.toFloat())
            putFloat(KEY_LOCATION_LON, city.longitude.toFloat())
        }
    }

    /** Restableix el rellotge sense modificar les mides dels widgets. */
    fun reset() {
        prefs.edit {
            remove(KEY_SHOW_SECONDS_WRITTEN)
            remove(KEY_HOUR_SIZE)
            remove(KEY_LOCATION_CITY)
            remove(KEY_LOCATION_LAT)
            remove(KEY_LOCATION_LON)
            remove("show_digital")
            remove("show_seconds")
        }
    }

    companion object {
        private const val PREFS_NAME = "cronos_prefs"
        private const val KEY_SHOW_SECONDS_WRITTEN = "show_seconds_written"
        private const val KEY_HOUR_SIZE = "hour_size"
        private const val KEY_LOCATION_CITY = "location_city"
        private const val KEY_LOCATION_LAT = "location_lat"
        private const val KEY_LOCATION_LON = "location_lon"
    }
}
