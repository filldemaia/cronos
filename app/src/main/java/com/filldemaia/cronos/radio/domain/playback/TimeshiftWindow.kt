package com.filldemaia.cronos.radio.domain.playback

/**
 * Finestra reproduïble: des del fotograma més antic que encara és al disc fins
 * al final de l'últim fotograma complet (el directe).
 */
data class TimeshiftWindow(
    val startUs: Long,
    val liveEdgeUs: Long
) {
    init {
        require(liveEdgeUs >= startUs)
    }

    val durationUs: Long get() = (liveEdgeUs - startUs).coerceAtLeast(0)
    val isEmpty: Boolean get() = durationUs <= 0

    fun clampSeek(targetUs: Long): Long = when {
        isEmpty -> startUs
        targetUs < startUs -> startUs
        targetUs > liveEdgeUs -> liveEdgeUs
        else -> targetUs
    }

    fun canSeekTo(targetUs: Long): Boolean = !isEmpty && targetUs in startUs..liveEdgeUs

    fun progress(positionUs: Long): Float {
        if (isEmpty) return 0f
        return ((positionUs - startUs).toDouble() / durationUs.toDouble())
            .coerceIn(0.0, 1.0)
            .toFloat()
    }

    fun delayUs(positionUs: Long): Long = (liveEdgeUs - clampSeek(positionUs)).coerceAtLeast(0)

    fun isAtLive(positionUs: Long, toleranceUs: Long = 2_000_000): Boolean =
        delayUs(positionUs) <= toleranceUs

    fun delayLabel(positionUs: Long): String = formatDelay(delayUs(positionUs))

    companion object {
        fun formatDelay(delayUs: Long): String {
            val totalSeconds = (delayUs.coerceAtLeast(0) / 1_000_000)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "-%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "-%02d:%02d".format(minutes, seconds)
            }
        }
    }
}
