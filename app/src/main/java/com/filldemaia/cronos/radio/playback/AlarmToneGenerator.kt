package com.filldemaia.cronos.radio.playback

import android.content.Context
import android.os.Build
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

/**
 * Genera un to d'alarma local, sense xarxa ni fitxers externs.
 *
 * El fitxer és un WAV PCM mono de 16 bits amb dos tons alterns i silencis
 * curts. El reproductor el fa sonar en bucle fins que l'usuari l'atura.
 *
 * Es desa a l'emmagatzematge protegit pel dispositiu perquè estigui disponible
 * abans del primer desbloqueig (alarma després d'un reinici).
 */
object AlarmToneGenerator {

    private const val SAMPLE_RATE = 44_100
    private const val FILE_NAME = "alarm_tone_v2.wav"

    fun ensureFile(context: Context): File {
        val storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        val file = File(storage.filesDir, FILE_NAME)
        if (file.exists() && file.length() > 44) return file
        file.parentFile?.mkdirs()
        val samples = buildSamples()
        writeWav(file, samples)
        return file
    }

    private fun buildSamples(): ShortArray {
        // Patró de 2,4 s: to greu, pausa, to agut, pausa. Es repeteix en bucle.
        val segments = listOf(
            Tone(880.0, 0.45),
            Silence(0.15),
            Tone(1174.66, 0.45),
            Silence(0.15),
            Tone(880.0, 0.45),
            Silence(0.15),
            Tone(1318.51, 0.45),
            Silence(0.15)
        )
        val total = segments.sumOf { (it.duration * SAMPLE_RATE).toInt() }
        val samples = ShortArray(total)
        var cursor = 0
        for (segment in segments) {
            val count = (segment.duration * SAMPLE_RATE).toInt()
            when (segment) {
                is Tone -> {
                    for (i in 0 until count) {
                        val envelope = minOf(1.0, i / (SAMPLE_RATE * 0.02), (count - i) / (SAMPLE_RATE * 0.02))
                        val value = sin(2.0 * PI * segment.frequency * i / SAMPLE_RATE) * envelope * 0.72
                        samples[cursor + i] = (value * Short.MAX_VALUE).toInt().toShort()
                    }
                }
                is Silence -> Unit
            }
            cursor += count
        }
        return samples
    }

    private fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        RandomAccessFile(file, "rw").use { out ->
            out.setLength(0)
            out.writeBytes("RIFF")
            out.writeIntLe(36 + dataSize)
            out.writeBytes("WAVE")
            out.writeBytes("fmt ")
            out.writeIntLe(16)
            out.writeShortLe(1) // PCM
            out.writeShortLe(1) // mono
            out.writeIntLe(SAMPLE_RATE)
            out.writeIntLe(SAMPLE_RATE * 2)
            out.writeShortLe(2)
            out.writeShortLe(16)
            out.writeBytes("data")
            out.writeIntLe(dataSize)
            for (sample in samples) out.writeShortLe(sample.toInt())
        }
    }

    private fun RandomAccessFile.writeIntLe(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 24) and 0xFF)
    }

    private fun RandomAccessFile.writeShortLe(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }

    private sealed interface Segment {
        val duration: Double
    }

    private data class Tone(val frequency: Double, override val duration: Double) : Segment
    private data class Silence(override val duration: Double) : Segment
}
