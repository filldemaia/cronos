package com.filldemaia.cronos.radio.domain.playback

/**
 * Detecta fotogrames MP3 i AAC/ADTS dins del flux que s'està gravant i en
 * calcula l'offset i la durada. No descodifica ni modifica l'àudio: el buffer
 * conté exactament els bytes nets que arriben del servidor.
 *
 * L'indexador és l'encarregat que els seeks sempre caiguin a l'inici d'un
 * fotograma, cosa imprescindible perquè ExoPlayer no arrenqui amb àudio corrupte.
 */
class AudioFrameIndexer {

    enum class Container { UNKNOWN, MP3, ADTS }

    data class IndexedFrame(val offset: Long, val durationUs: Long)

    var container: Container = Container.UNKNOWN
        private set

    private var pending = ByteArray(0)
    private var pendingBaseOffset = 0L
    private var nextOffset = 0L
    private var cumulativeUs = 0L

    fun reset(nextStreamOffset: Long) {
        pending = ByteArray(0)
        pendingBaseOffset = nextStreamOffset
        nextOffset = nextStreamOffset
        cumulativeUs = 0L
        container = Container.UNKNOWN
    }

    fun feed(data: ByteArray, length: Int = data.size): List<IndexedFrame> {
        if (length <= 0) return emptyList()
        appendPending(data, length)
        val result = mutableListOf<IndexedFrame>()
        var cursor = 0
        var guard = 0
        while (cursor < pending.size && guard++ < MAX_FRAMES_PER_FEED) {
            if (container == Container.UNKNOWN) {
                val detected = detectContainer(cursor)
                if (detected == null) {
                    if (pending.size - cursor < 4) break
                    cursor += 1
                    continue
                }
                container = detected
            }

            val sync = findSync(cursor)
            if (sync < 0) {
                cursor = (pending.size - 2).coerceAtLeast(cursor)
                break
            }
            cursor = sync

            val header = parseHeader(cursor)
            if (header == null) {
                cursor += 1
                continue
            }
            if (pending.size - cursor < header.frameLength) break

            val offset = pendingBaseOffset + cursor
            nextOffset = offset + header.frameLength
            cumulativeUs += header.durationUs
            result += IndexedFrame(offset, header.durationUs)
            cursor += header.frameLength
        }

        if (cursor > 0) {
            pending = if (cursor >= pending.size) {
                ByteArray(0)
            } else {
                pending.copyOfRange(cursor, pending.size)
            }
            pendingBaseOffset += cursor
        }
        return result
    }

    private fun appendPending(data: ByteArray, length: Int) {
        if (pending.isEmpty()) {
            pending = data.copyOf(length)
        } else {
            val merged = ByteArray(pending.size + length)
            pending.copyInto(merged)
            data.copyInto(merged, pending.size, 0, length)
            pending = merged
        }
    }

    private fun detectContainer(start: Int): Container? {
        if (pending.size - start < 4) return null
        val b0 = pending[start].toInt() and 0xFF
        val b1 = pending[start + 1].toInt() and 0xFF
        if (b0 != 0xFF || (b1 and 0xF0) != 0xF0) return null
        val layer = (b1 ushr 1) and 0x03
        return if (layer == 0) {
            Container.ADTS
        } else {
            val header = parseMp3Header(start) ?: return null
            if (header.frameLength > 8) Container.MP3 else null
        }
    }

    private fun findSync(from: Int): Int {
        for (i in from until pending.size - 1) {
            val b0 = pending[i].toInt() and 0xFF
            val b1 = pending[i + 1].toInt() and 0xFF
            if (b0 != 0xFF) continue
            if (container == Container.ADTS && (b1 and 0xF6) == 0xF0) return i
            if (container == Container.MP3 && (b1 and 0xE0) == 0xE0) {
                if (parseMp3Header(i) != null) return i
            }
        }
        return -1
    }

    private fun parseHeader(start: Int): Header? = when (container) {
        Container.MP3 -> parseMp3Header(start)
        Container.ADTS -> parseAdtsHeader(start)
        Container.UNKNOWN -> null
    }

    private fun parseMp3Header(start: Int): Header? {
        if (pending.size - start < 4) return null
        val b0 = pending[start].toInt() and 0xFF
        val b1 = pending[start + 1].toInt() and 0xFF
        val b2 = pending[start + 2].toInt() and 0xFF
        val b3 = pending[start + 3].toInt() and 0xFF
        if (b0 != 0xFF || (b1 and 0xE0) != 0xE0) return null

        val versionBits = (b1 ushr 3) and 0x03
        val layerBits = (b1 ushr 1) and 0x03
        if (versionBits == 1 || layerBits == 0) return null

        val bitrateIndex = (b2 ushr 4) and 0x0F
        val sampleRateIndex = (b2 ushr 2) and 0x03
        if (bitrateIndex == 0 || bitrateIndex == 15 || sampleRateIndex == 3) return null

        val mpeg1 = versionBits == 3
        val layer = when (layerBits) { 3 -> 1; 2 -> 2; else -> 3 }
        val bitrate = MP3_BITRATES[if (mpeg1) 0 else 1][layer - 1]?.get(bitrateIndex) ?: return null
        if (bitrate == 0) return null
        var sampleRate = MP3_SAMPLE_RATES[sampleRateIndex]
        if (!mpeg1) sampleRate /= if (versionBits == 2) 2 else 4
        if (sampleRate <= 0) return null

        val padding = (b2 ushr 1) and 0x01
        val frameLength = when (layer) {
            1 -> ((12 * bitrate * 1000 / sampleRate) + padding) * 4
            2 -> (144 * bitrate * 1000 / sampleRate) + padding
            else -> {
                val coefficient = if (mpeg1) 144 else 72
                (coefficient * bitrate * 1000 / sampleRate) + padding
            }
        }
        if (frameLength !in 24..8192) return null

        val samples = when {
            layer == 1 -> 384
            layer == 2 -> 1152
            mpeg1 -> 1152
            else -> 576
        }
        val durationUs = samples * 1_000_000L / sampleRate
        // El byte de protecció CRC afegeix 2 bytes, però frameLength ja els inclou.
        if (b3 < 0) return null
        return Header(frameLength, durationUs)
    }

    private fun parseAdtsHeader(start: Int): Header? {
        if (pending.size - start < 7) return null
        val b0 = pending[start].toInt() and 0xFF
        val b1 = pending[start + 1].toInt() and 0xFF
        if (b0 != 0xFF || (b1 and 0xF6) != 0xF0) return null
        val protectionAbsent = b1 and 0x01
        val b2 = pending[start + 2].toInt() and 0xFF
        val b3 = pending[start + 3].toInt() and 0xFF
        val b4 = pending[start + 4].toInt() and 0xFF
        val b5 = pending[start + 5].toInt() and 0xFF
        val b6 = pending[start + 6].toInt() and 0xFF
        val sampleRateIndex = (b2 ushr 2) and 0x0F
        val sampleRate = ADTS_SAMPLE_RATES.getOrNull(sampleRateIndex) ?: return null
        val frameLength = ((b3 and 0x03) shl 11) or (b4 shl 3) or ((b5 and 0xE0) ushr 5)
        if (frameLength < 7) return null
        val headerLength = if (protectionAbsent == 1) 7 else 9
        if (frameLength < headerLength) return null
        val rawDataBlocks = (b6 and 0x03) + 1
        val durationUs = rawDataBlocks * 1024L * 1_000_000L / sampleRate
        return Header(frameLength, durationUs)
    }

    private data class Header(val frameLength: Int, val durationUs: Long)

    private companion object {
        const val MAX_FRAMES_PER_FEED = 100_000
        val MP3_SAMPLE_RATES = intArrayOf(44_100, 48_000, 32_000)
        val ADTS_SAMPLE_RATES = intArrayOf(
            96_000, 88_200, 64_000, 48_000, 44_100, 32_000, 24_000,
            22_050, 16_000, 12_000, 11_025, 8_000, 7_350
        )

        // [MPEG1/MPEG2, layer I/II/III, bitrate index]
        val MP3_BITRATES: Array<Array<IntArray?>> = arrayOf(
            arrayOf(
                intArrayOf(0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448),
                intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384),
                intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)
            ),
            arrayOf(
                intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256),
                intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160),
                intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)
            )
        )
    }
}
