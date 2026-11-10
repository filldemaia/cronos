package com.filldemaia.cronos.radio.playback

import com.filldemaia.cronos.radio.domain.playback.AudioFrameIndexer
import com.filldemaia.cronos.radio.domain.playback.DiskRingBuffer
import com.filldemaia.cronos.radio.domain.playback.FrameTimeline
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call

/**
 * Descarrega un stream progressiu i l'escriu, sense descodificar-lo, al buffer
 * circular. També indexa els fotogrames MP3/ADTS per poder-hi fer seeks
 * alineats.
 *
 * El fil de descàrrega és independent del fil d'ExoPlayer: en pausa la
 * reproducció s'atura, però la gravació continua.
 */
class StreamRecorder(
    private val url: String,
    private val buffer: DiskRingBuffer,
    private val timeline: FrameTimeline,
    private val indexer: AudioFrameIndexer,
    private val client: OkHttpClient,
    private val onFailure: (Throwable) -> Unit,
    private val onProgress: () -> Unit
) {

    private val stopped = AtomicBoolean(false)
    @Volatile private var call: Call? = null
    @Volatile private var thread: Thread? = null
    @Volatile var lastError: Throwable? = null
        private set

    val isRunning: Boolean
        get() = thread?.isAlive == true && !stopped.get()

    fun start() {
        if (thread != null) return
        thread = Thread({ runLoop() }, "Cronos-Recorder").apply {
            isDaemon = true
            start()
        }
    }

    private fun runLoop() {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Icy-MetaData", "0")
                .header("Accept", "*/*")
                .build()
            val activeCall = client.newCall(request)
            call = activeCall
            activeCall.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("El servidor ha respost ${response.code}")
                }
                val body = response.body ?: throw IOException("Resposta sense contingut")
                // Timeout de lectura: si el servidor deixa d'enviar dades, volem
                // passar al següent intent en lloc de quedar-nos en silenci.
                body.source().use { source ->
                    val chunk = ByteArray(CHUNK_BYTES)
                    while (!stopped.get()) {
                        val read = source.read(chunk)
                        if (read == -1) throw IOException("El directe ha tancat la connexió")
                        if (read == 0) continue
                        val streamOffset = buffer.writeOffset
                        val frames = indexer.feed(chunk, read)
                        buffer.append(chunk, read)
                        if (frames.isNotEmpty()) {
                            for (frame in frames) {
                                timeline.addFrame(frame.offset, frame.durationUs)
                            }
                            timeline.pruneBefore(buffer.oldestOffset)
                        }
                        // L'offset de referència no s'usa per reproduir, però ajuda
                        // a detectar que l'índex i el buffer van sincronitzats.
                        if (streamOffset < buffer.oldestOffset) {
                            throw IOException("El buffer ha quedat massa enrere")
                        }
                        onProgress()
                    }
                }
            }
        } catch (error: Throwable) {
            lastError = error
            if (!stopped.get()) onFailure(error)
        }
    }

    fun stop() {
        if (!stopped.compareAndSet(false, true)) return
        runCatching { call?.cancel() }
        runCatching { thread?.interrupt() }
    }

    private companion object {
        const val CHUNK_BYTES = 16 * 1024
        const val USER_AGENT = "Cronos/1.0 (Android)"
    }
}
