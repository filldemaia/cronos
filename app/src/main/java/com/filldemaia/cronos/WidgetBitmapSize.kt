package com.filldemaia.cronos

import kotlin.math.sqrt

/** Bound the two orientation bitmaps before sending them to the launcher. */
internal object WidgetBitmapSize {
    fun fit(width: Int, height: Int, maxPixels: Long, maxEdge: Int = 1600): Pair<Int, Int> {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val budget = maxPixels.coerceAtLeast(1)
        val edge = maxEdge.coerceAtLeast(1)
        val scale = minOf(
            1.0, edge.toDouble() / w, edge.toDouble() / h,
            sqrt(budget.toDouble() / (w.toDouble() * h))
        )
        var fittedW = (w * scale).toInt().coerceAtLeast(1)
        var fittedH = (h * scale).toInt().coerceAtLeast(1)
        if (fittedW.toLong() * fittedH > budget) {
            if (fittedW >= fittedH) fittedW = (budget / fittedH).toInt().coerceAtLeast(1)
            else fittedH = (budget / fittedW).toInt().coerceAtLeast(1)
        }
        return fittedW to fittedH
    }
}
