package com.example.cronos

import android.content.Context
import android.content.SharedPreferences

/**
 * Repositori de preferències de l'usuari. Emmagatzema en
 * SharedPreferences per simplicitat — no cal DataStore per
 * quatre booleans.
 *
 * Exposa [observe] i [update] com a funcions pures (sense
 * coroutines) perquè es puguin cridar des de qualsevol lloc.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Mostra els segons a l'hora digital (per defecte: false). */
    var showSeconds: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS, false)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_SECONDS, value).apply() }

    /** Mostra els segons per escrit a l'hora tradicional (per defecte: false). */
    var showSecondsWritten: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS_WRITTEN, false)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_SECONDS_WRITTEN, value).apply() }

    /** Mostra l'hora digital sota la tradicional (per defecte: false). */
    var showDigital: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DIGITAL, false)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_DIGITAL, value).apply() }

    /** Mida de l'hora catalana, en sp (per defecte: 28). */
    var hourSize: Float
        get() = prefs.getFloat(KEY_HOUR_SIZE, 28f)
        set(value) { prefs.edit().putFloat(KEY_HOUR_SIZE, value).apply() }

    /** Esborra totes les preferències i torna als valors per defecte. */
    fun reset() {
        prefs.edit().clear().apply()
    }

    /** Registra un callback per quan canvia qualsevol preferència. */
    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "cronos_prefs"
        private const val KEY_SHOW_SECONDS = "show_seconds"
        private const val KEY_SHOW_SECONDS_WRITTEN = "show_seconds_written"
        private const val KEY_SHOW_DIGITAL = "show_digital"
        private const val KEY_HOUR_SIZE = "hour_size"
    }
}
