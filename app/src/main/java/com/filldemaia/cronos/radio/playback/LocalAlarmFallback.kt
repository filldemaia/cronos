package com.filldemaia.cronos.radio.playback

import kotlinx.coroutines.CancellationException

internal object LocalAlarmFallback {
    suspend fun start(
        generated: suspend () -> Unit,
        system: suspend () -> Unit,
        emergency: () -> Unit
    ): Boolean {
        try {
            generated()
            return true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Fall back to the device alarm sound.
        }
        try {
            system()
            return true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emergency()
            return false
        }
    }
}
