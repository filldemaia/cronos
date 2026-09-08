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

    /** Mostra els segons a l'hora digital (per defecte: false). */
    var showSeconds: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_SECONDS, value) }

    /** Mostra els segons per escrit a l'hora tradicional (per defecte: false). */
    var showSecondsWritten: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS_WRITTEN, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_SECONDS_WRITTEN, value) }

    /** Mostra l'hora digital sota la tradicional (per defecte: false). */
    var showDigital: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DIGITAL, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_DIGITAL, value) }

    /** Mida de l'hora catalana, en sp (per defecte: 28). */
    var hourSize: Float
        get() = prefs.getFloat(KEY_HOUR_SIZE, 28f)
        set(value) = prefs.edit { putFloat(KEY_HOUR_SIZE, value) }

    /** Esborra totes les preferències i torna als valors per defecte. */
    fun reset() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "cronos_prefs"
        private const val KEY_SHOW_SECONDS = "show_seconds"
        private const val KEY_SHOW_SECONDS_WRITTEN = "show_seconds_written"
        private const val KEY_SHOW_DIGITAL = "show_digital"
        private const val KEY_HOUR_SIZE = "hour_size"
    }
}
