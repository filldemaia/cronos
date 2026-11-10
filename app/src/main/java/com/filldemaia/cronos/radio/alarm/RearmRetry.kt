package com.filldemaia.cronos.radio.alarm

import kotlinx.coroutines.CancellationException

internal object RearmRetry {
    suspend fun run(arm: suspend () -> Boolean): Boolean {
        repeat(2) {
            try {
                if (arm()) return true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A second attempt also covers transient scheduler failures.
            }
        }
        return false
    }
}
