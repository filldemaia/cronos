package com.filldemaia.cronos.radio.domain.playback

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Buffer circular binari sobre disc. Els offsets que fa servir el reproductor són
 * "virtuals" i creixen sempre (no fan wrap): així una posició de reproducció no
 * canvia de significat quan l'anell sobreescriu les dades antigues.
 *
 * Sempre es conserva com a màxim [capacityBytes]. Quan s'escriu més enllà de la
 * capacitat, les dades més antigues queden invalidades.
 */
class DiskRingBuffer(
    val file: File,
    val capacityBytes: Long
) : Closeable {

    init {
        require(capacityBytes > 0) { "La capacitat ha de ser positiva" }
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
    }

    private val fileAccess = RandomAccessFile(file, "rw").apply {
        setLength(capacityBytes)
    }
    private val lock = ReentrantLock()
    private val dataAvailable = lock.newCondition()
    private var totalWritten = 0L
    private var closed = false
    private var deleteOnClose = true

    val writeOffset: Long
        get() = lock.withLock { totalWritten }

    val oldestOffset: Long
        get() = lock.withLock { oldestLocked() }

    val availableBytes: Long
        get() = lock.withLock { totalWritten - oldestLocked() }

    fun append(data: ByteArray, length: Int = data.size) {
        if (length <= 0) return
        require(length <= data.size)
        lock.withLock {
            check(!closed) { "Buffer tancat" }
            var sourceOffset = 0
            var remaining = length
            if (remaining > capacityBytes) {
                sourceOffset = (remaining - capacityBytes).toInt()
                remaining = capacityBytes.toInt()
            }
            var virtual = totalWritten + (length - remaining)
            while (remaining > 0) {
                val physical = (virtual % capacityBytes).toInt()
                val chunk = minOf(remaining, capacityBytes.toInt() - physical)
                fileAccess.seek(physical.toLong())
                fileAccess.write(data, sourceOffset, chunk)
                sourceOffset += chunk
                virtual += chunk
                remaining -= chunk
            }
            totalWritten += length
            dataAvailable.signalAll()
        }
    }

    /**
     * Llegeix sense bloquejar. Retorna 0 si no hi ha dades noves en aquest offset.
     * Llança [BufferOverrunException] si la posició ja ha estat sobreescrita.
     */
    fun read(offset: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int {
        lock.withLock {
            if (offset < oldestLocked()) throw BufferOverrunException(offset, oldestLocked())
            if (offset >= totalWritten) return 0
            val max = minOf(length.toLong(), totalWritten - offset).toInt()
            var virtual = offset
            var read = 0
            while (read < max) {
                val physical = (virtual % capacityBytes).toInt()
                val chunk = minOf(max - read, capacityBytes.toInt() - physical)
                fileAccess.seek(physical.toLong())
                fileAccess.readFully(destination, destinationOffset + read, chunk)
                virtual += chunk
                read += chunk
            }
            return read
        }
    }

    /**
     * Lectura pensada per al thread d'ExoPlayer: espera fins que hi hagi dades
     * noves al directe. Retorna -1 quan el buffer s'ha tancat, 0 en timeout.
     */
    fun awaitRead(
        offset: Long,
        destination: ByteArray,
        destinationOffset: Int,
        length: Int,
        waitMillis: Long = 1_000
    ): Int {
        lock.withLock {
            if (offset < oldestLocked()) throw BufferOverrunException(offset, oldestLocked())
            while (!closed && offset >= totalWritten) {
                if (!dataAvailable.await(waitMillis, TimeUnit.MILLISECONDS)) return 0
            }
            if (closed && offset >= totalWritten) return -1
            if (offset < oldestLocked()) throw BufferOverrunException(offset, oldestLocked())
            val max = minOf(length.toLong(), totalWritten - offset).toInt()
            var virtual = offset
            var read = 0
            while (read < max) {
                val physical = (virtual % capacityBytes).toInt()
                val chunk = minOf(max - read, capacityBytes.toInt() - physical)
                fileAccess.seek(physical.toLong())
                fileAccess.readFully(destination, destinationOffset + read, chunk)
                virtual += chunk
                read += chunk
            }
            return read
        }
    }

    fun close(deleteFile: Boolean = deleteOnClose) {
        lock.withLock {
            if (closed) return
            closed = true
            deleteOnClose = deleteFile
            dataAvailable.signalAll()
            runCatching { fileAccess.close() }
        }
        if (deleteFile) runCatching { file.delete() }
    }

    override fun close() = close(deleteOnClose)

    private fun oldestLocked(): Long = (totalWritten - capacityBytes).coerceAtLeast(0)

    class BufferOverrunException(val requested: Long, val oldest: Long) :
        IllegalStateException("Posició $requested anterior al buffer disponible ($oldest)")
}
