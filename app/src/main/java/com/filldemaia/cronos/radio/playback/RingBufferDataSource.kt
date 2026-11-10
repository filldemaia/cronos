package com.filldemaia.cronos.radio.playback

import android.net.Uri
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.core.net.toUri
import com.filldemaia.cronos.radio.domain.playback.DiskRingBuffer

/**
 * Font de dades d'ExoPlayer que llegeix del buffer circular de disc.
 *
 * [baseOffset] és l'offset virtual del primer byte que ha de veure el
 * reproductor. Com que el buffer mai es reutilitza amb offsets descendents, un
 * canvi de posició es fa creant una font nova amb un altre [baseOffset].
 */
@AndroidXOptIn(UnstableApi::class)
class RingBufferDataSource(
    private val buffer: DiskRingBuffer,
    private val baseOffset: Long
) : BaseDataSource(false) {

    private var currentOffset = baseOffset
    private var opened = false
    private var uri: Uri = STREAM_URI.toUri()

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        currentOffset = baseOffset + dataSpec.position
        opened = true
        transferStarted(dataSpec)
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        while (opened) {
            val read = runCatching {
                buffer.awaitRead(currentOffset, target, offset, length)
            }.getOrElse { error ->
                // Una sobreescriptura del buffer no ha de tombar el reproductor:
                // el servei reancora la reproducció al fotograma més antic.
                if (error is DiskRingBuffer.BufferOverrunException) {
                    throw BufferOverrunSignal(error)
                }
                throw error
            }
            when {
                read == -1 -> return C.RESULT_END_OF_INPUT
                read > 0 -> {
                    currentOffset += read
                    bytesTransferred(read)
                    return read
                }
                // Sense dades noves: seguim esperant. El reproductor no ha de
                // rebre un final de flux mentre el directe continuï obert.
                else -> continue
            }
        }
        return C.RESULT_END_OF_INPUT
    }

    override fun getUri(): Uri = uri

    override fun close() {
        opened = false
        transferEnded()
    }

    class BufferOverrunSignal(val overrun: DiskRingBuffer.BufferOverrunException) :
        RuntimeException(overrun.message, overrun)

    companion object {
        const val STREAM_URI = "radiocatalana-buffer://stream"
    }
}

@AndroidXOptIn(UnstableApi::class)
class RingBufferDataSourceFactory(
    private val buffer: DiskRingBuffer,
    private val baseOffset: Long
) : DataSource.Factory {
    override fun createDataSource(): DataSource = RingBufferDataSource(buffer, baseOffset)

    companion object {
        fun uri(): Uri = RingBufferDataSource.STREAM_URI.toUri()
    }
}
