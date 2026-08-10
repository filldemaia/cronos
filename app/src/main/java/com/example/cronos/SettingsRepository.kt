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

    /** Mostra els segons a l'hora digital (per defecte: true). */
    var showSeconds: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS, true)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_SECONDS, value).apply() }

    /** Activa un format compacte quan l'hora és exacta (per defecte: true). */
    var useCompactExactHour: Boolean
        get() = prefs.getBoolean(KEY_COMPACT_EXACT, true)
        set(value) { prefs.edit().putBoolean(KEY_COMPACT_EXACT, value).apply() }

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
        private const val KEY_COMPACT_EXACT = "compact_exact"
    }
}
