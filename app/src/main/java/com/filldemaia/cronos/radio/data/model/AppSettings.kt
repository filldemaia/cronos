package com.filldemaia.cronos.radio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val bufferDurationMinutes: Int = 180,
    /**
     * Camp antic conservat per poder llegir ajustos ja desats. Els controls
     * actuals fan servir [skipBackSeconds] i [skipForwardSeconds].
     */
    val skipSeconds: Int = 30,
    val skipBackSeconds: Int = 10,
    val skipForwardSeconds: Int = 10,
    val defaultFadeInSeconds: Int = 20,
    val warnOnMobileData: Boolean = true,
    val keepBufferOnStationChange: Boolean = false,
    /** Mostra el selector d'emissores en mosaic (true) o en llista (false). */
    val stationsGrid: Boolean = false,
    val lastStationId: String = "catalunya-radio"
) {
    companion object {
        val BUFFER_OPTIONS_MINUTES = listOf(60, 180, 360, 720)
        val SKIP_OPTIONS_SECONDS = listOf(5, 10, 15, 30, 60)
        val FADE_OPTIONS_SECONDS = listOf(0, 10, 20, 30, 60)
    }
}
