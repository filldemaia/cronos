package com.filldemaia.cronos.radio.playback

import com.filldemaia.cronos.radio.data.model.Station
import com.filldemaia.cronos.radio.domain.playback.TimeshiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PlaybackMode {
    IDLE,
    TIMESHIFT,
    HLS_LIVE,
    FALLBACK_STATION,
    LOCAL_ALARM
}

data class PlaybackSnapshot(
    val station: Station? = null,
    val mode: PlaybackMode = PlaybackMode.IDLE,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isScheduled: Boolean = false,
    val isTest: Boolean = false,
    val isLocalAlarm: Boolean = false,
    val isFallbackStation: Boolean = false,
    val currentTitle: String? = null,
    val errorMessage: String? = null,
    val positionUs: Long = 0L,
    val windowStartUs: Long = 0L,
    val liveEdgeUs: Long = 0L,
    val bufferDurationUs: Long = 0L,
    /** HLS live latency in milliseconds; null when the player cannot report it. */
    val hlsLiveOffsetMs: Long? = null,
    val volume: Float = 1f,
    val scheduledLabel: String? = null,
    val sourceLabel: String? = null,
    val startedAtMillis: Long = 0L,
    val stopAfterAtMillis: Long? = null,
    val sleepTimerAtMillis: Long? = null,
    val skipBackSeconds: Int = 10,
    val skipForwardSeconds: Int = 10
) {
    val window: TimeshiftWindow
        get() = TimeshiftWindow(
            startUs = windowStartUs.coerceAtMost(liveEdgeUs),
            liveEdgeUs = liveEdgeUs
        )

    val isAtLive: Boolean
        get() = when (mode) {
            PlaybackMode.HLS_LIVE -> hlsLiveOffsetMs?.let { it <= 2_000L } ?: true
            else -> window.isAtLive(positionUs)
        }

    val delayLabel: String
        get() = when {
            isLocalAlarm -> "ALARMA"
            mode == PlaybackMode.HLS_LIVE -> hlsLiveOffsetMs
                ?.let { TimeshiftWindow.formatDelay(it * 1_000L) }
                ?: "-00:00"
            else -> window.delayLabel(positionUs)
        }

    val hasTimeshift: Boolean
        get() = mode == PlaybackMode.TIMESHIFT && liveEdgeUs > windowStartUs

    val statusText: String
        get() = when {
            isLocalAlarm -> "To d'alarma local · atura'l manualment"
            errorMessage != null -> errorMessage
            isBuffering -> "Connectant…"
            isFallbackStation -> "Emissora de reserva"
            isAtLive -> "En directe"
            else -> "Retard ${delayLabel.removePrefix("-")}"
        }
}

class PlaybackStateStore {
    private val _state = MutableStateFlow(PlaybackSnapshot())
    val state: StateFlow<PlaybackSnapshot> = _state.asStateFlow()

    fun update(transform: (PlaybackSnapshot) -> PlaybackSnapshot) {
        _state.update(transform)
    }

    fun set(value: PlaybackSnapshot) {
        _state.value = value
    }

    fun reset() {
        _state.value = PlaybackSnapshot()
    }
}
