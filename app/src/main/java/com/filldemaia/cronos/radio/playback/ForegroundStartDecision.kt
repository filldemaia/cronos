package com.filldemaia.cronos.radio.playback

internal object ForegroundStartDecision {
    fun proceed(startForeground: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            startForeground()
        } catch (error: Exception) {
            onFailure(error)
        }
    }
}
