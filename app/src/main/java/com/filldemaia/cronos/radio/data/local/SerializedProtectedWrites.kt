package com.filldemaia.cronos.radio.data.local

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes complete read-modify-write operations within the app process. */
internal class SerializedProtectedWrites(private val onError: (String, Exception) -> Unit) {
    private val mutex = Mutex()
    private val failures = mutableMapOf<String, String>()

    suspend fun write(name: String, action: () -> Unit) = mutex.withLock {
        try {
            action()
            synchronized(failures) { failures.remove(name) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            synchronized(failures) { failures[name] = error.message ?: error.javaClass.simpleName }
            onError(name, error)
            throw error
        }
    }

    fun errors(): Map<String, String> = synchronized(failures) { failures.toMap() }
}
