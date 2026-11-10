package com.filldemaia.cronos.radio.domain.playback

import com.filldemaia.cronos.radio.data.model.Station

enum class AttemptKind {
    PRIMARY_URL,
    ALTERNATIVE_URL,
    FALLBACK_STATION,
    LOCAL_TONE
}

data class PlaybackAttempt(
    val kind: AttemptKind,
    val station: Station?,
    val url: String?,
    val retry: Int,
    val delayBeforeMs: Long,
    val label: String
)

/**
 * Cadena de seguretat determinista. El to local sempre és l'últim element i no
 * depèn de la xarxa.
 */
object FallbackPlanner {

    fun plan(
        primary: Station,
        fallbackStations: List<Station>,
        retriesPerUrl: Int = 3,
        baseBackoffMs: Long = 1_000,
        includeLocalTone: Boolean = true
    ): List<PlaybackAttempt> {
        require(retriesPerUrl >= 1)
        val attempts = mutableListOf<PlaybackAttempt>()

        primary.urls.forEachIndexed { urlIndex, url ->
            val kind = if (urlIndex == 0) AttemptKind.PRIMARY_URL else AttemptKind.ALTERNATIVE_URL
            for (retry in 0 until retriesPerUrl) {
                val delay = if (retry == 0) 0 else baseBackoffMs * (1L shl (retry - 1))
                attempts += PlaybackAttempt(
                    kind = kind,
                    station = primary,
                    url = url,
                    retry = retry,
                    delayBeforeMs = delay,
                    label = if (retry == 0) {
                        "${primary.name} · ${if (urlIndex == 0) "principal" else "alternativa ${urlIndex}"}"
                    } else {
                        "${primary.name} · reintent ${retry + 1}"
                    }
                )
            }
        }

        fallbackStations.forEach { station ->
            station.urls.forEachIndexed { urlIndex, url ->
                for (retry in 0 until maxOf(1, retriesPerUrl - 1)) {
                    attempts += PlaybackAttempt(
                        kind = AttemptKind.FALLBACK_STATION,
                        station = station,
                        url = url,
                        retry = retry,
                        delayBeforeMs = if (retry == 0) 0 else baseBackoffMs * (1L shl retry),
                        label = "${station.name} · reserva${if (urlIndex > 0) " ${urlIndex + 1}" else ""}"
                    )
                }
            }
        }

        if (includeLocalTone) {
            attempts += PlaybackAttempt(
                kind = AttemptKind.LOCAL_TONE,
                station = null,
                url = null,
                retry = 0,
                delayBeforeMs = 0,
                label = "To d'alarma local"
            )
        }
        return attempts
    }
}
