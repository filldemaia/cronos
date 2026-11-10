package com.filldemaia.cronos.radio.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.filldemaia.cronos.radio.data.local.AppJson
import com.filldemaia.cronos.radio.data.local.DeviceProtectedStore
import com.filldemaia.cronos.radio.data.local.PreferenceKeys
import com.filldemaia.cronos.radio.data.local.radioDataStore
import com.filldemaia.cronos.radio.data.model.Station
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

class StationRepository(
    private val context: Context,
    private val deviceProtectedStore: DeviceProtectedStore
) {

    private val builtIn: List<Station> by lazy {
        runCatching {
            context.assets.open("stations.json").bufferedReader().use { it.readText() }
                .let { AppJson.decodeFromString(ListSerializer(Station.serializer()), it) }
        }.getOrElse { emptyList() }
    }

    /** Emissores del catàleg inclòs a l'APK. No depenen de DataStore ni del desbloqueig. */
    fun builtInStations(): List<Station> = builtIn

    private val customStationsFlow: Flow<List<Station>> = context.radioDataStore.data.map { prefs ->
        decodeCustom(prefs[PreferenceKeys.CUSTOM_STATIONS])
    }

    val stations: Flow<List<Station>> = customStationsFlow.map { custom ->
        (builtIn + custom).distinctBy { it.id }
    }

    suspend fun current(): List<Station> = (builtIn + decodeCustom(
        context.radioDataStore.data.first()[PreferenceKeys.CUSTOM_STATIONS]
    )).distinctBy { it.id }

    suspend fun find(id: String): Station? = current().firstOrNull { it.id == id }

    suspend fun addCustom(name: String, url: String): Station {
        val cleanUrl = url.trim()
        require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            "L'adreça ha de començar per http:// o https://"
        }
        val station = Station(
            id = "custom-${UUID.randomUUID()}",
            name = name.trim().ifBlank { "Emissora personalitzada" },
            tagline = "Emissora personalitzada",
            urls = listOf(cleanUrl),
            logoText = name.trim().take(2).uppercase().ifBlank { "R" },
            logoColor = CUSTOM_COLORS.random(),
            isCustom = true
        )
        context.radioDataStore.edit { prefs ->
            val all = decodeCustom(prefs[PreferenceKeys.CUSTOM_STATIONS]) + station
            prefs[PreferenceKeys.CUSTOM_STATIONS] =
                AppJson.encodeToString(ListSerializer(Station.serializer()), all)
        }
        mirrorStations()
        return station
    }

    suspend fun removeCustom(id: String) {
        context.radioDataStore.edit { prefs ->
            val all = decodeCustom(prefs[PreferenceKeys.CUSTOM_STATIONS]).filterNot { it.id == id }
            prefs[PreferenceKeys.CUSTOM_STATIONS] =
                AppJson.encodeToString(ListSerializer(Station.serializer()), all)
        }
        mirrorStations()
    }

    /**
     * Còpia de totes les emissores (incloses les personalitzades) a
     * l'emmagatzematge protegida pel dispositiu, perquè una alarma pugui
     * arrencar abans del desbloqueig.
     */
    suspend fun mirrorStations() {
        val all = runCatching { current() }.getOrNull() ?: return
        withContext(Dispatchers.IO) {
            runCatching { deviceProtectedStore.saveStations(all) }
        }
    }

    private fun decodeCustom(raw: String?): List<Station> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            AppJson.decodeFromString(ListSerializer(Station.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    private companion object {
        val CUSTOM_COLORS = listOf("#FF7A1A", "#31A36C", "#7A5AF8", "#E34D59", "#1D9BF0")
    }
}
