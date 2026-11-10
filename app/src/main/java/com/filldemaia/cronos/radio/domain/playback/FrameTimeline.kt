package com.filldemaia.cronos.radio.domain.playback

data class FramePoint(
    val offset: Long,
    val startTimeUs: Long,
    val durationUs: Long
) {
    val endTimeUs: Long get() = startTimeUs + durationUs
}

/**
 * Índex temporal de fotogrames d'àudio. Relaciona el temps amb l'offset virtual
 * del buffer i permet fer seek a un fotograma alineat.
 */
class FrameTimeline {

    private val frames = ArrayDeque<FramePoint>()
    private var totalDurationUs = 0L

    @Synchronized
    fun addFrame(offset: Long, durationUs: Long): FramePoint {
        require(durationUs > 0)
        val point = FramePoint(offset, totalDurationUs, durationUs)
        frames.addLast(point)
        totalDurationUs += durationUs
        return point
    }

    @Synchronized
    fun latestTimeUs(): Long = totalDurationUs

    @Synchronized
    fun firstFrame(): FramePoint? = frames.firstOrNull()

    @Synchronized
    fun lastFrame(): FramePoint? = frames.lastOrNull()

    @Synchronized
    fun size(): Int = frames.size

    @Synchronized
    fun frameAtOrBefore(timeUs: Long): FramePoint? {
        if (frames.isEmpty()) return null
        val target = timeUs.coerceAtLeast(0)
        var low = 0
        var high = frames.size - 1
        var result = frames.first()
        val list = frames.toList()
        while (low <= high) {
            val mid = (low + high) ushr 1
            val frame = list[mid]
            if (frame.startTimeUs <= target) {
                result = frame
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    @Synchronized
    fun frameAtOrAfter(timeUs: Long): FramePoint? {
        val list = frames.toList()
        return list.firstOrNull { it.startTimeUs >= timeUs } ?: list.lastOrNull()
    }

    @Synchronized
    fun timeAtOffset(offset: Long): Long? {
        if (frames.isEmpty()) return null
        var low = 0
        var high = frames.size - 1
        var result: FramePoint? = frames.first()
        val list = frames.toList()
        while (low <= high) {
            val mid = (low + high) ushr 1
            val frame = list[mid]
            if (frame.offset <= offset) {
                result = frame
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        val frame = result ?: return null
        val deltaBytes = (offset - frame.offset).coerceAtLeast(0)
        // Aproximació dins del fotograma per la barra de progrés; el seek sempre
        // s'alinea al fotograma anterior.
        return frame.startTimeUs + deltaBytes * 0L
    }

    /**
     * Elimina els fotogrames que ja han estat sobreescrits. Es conserva el darrer
     * fotograma anterior a [oldestOffset] per poder començar-hi exactament.
     */
    @Synchronized
    fun pruneBefore(oldestOffset: Long) {
        while (frames.size > 1) {
            val second = frames.elementAt(1)
            if (second.offset <= oldestOffset && second.offset < frames.last().offset) {
                frames.removeFirst()
            } else {
                break
            }
        }
    }

    @Synchronized
    fun clear() {
        frames.clear()
        totalDurationUs = 0L
    }

    @Synchronized
    fun snapshot(): List<FramePoint> = frames.toList()
}
