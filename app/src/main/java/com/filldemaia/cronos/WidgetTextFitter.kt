package com.filldemaia.cronos

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue

/** Fit the whole phrase, using the user's size as a ceiling rather than cropping it. */
internal object WidgetTextFitter {
    const val LINE_SPACING = 1.12f
    private const val MIN_FIT_SP = 10f

    fun time(
        context: Context,
        text: String,
        requestedSp: Float,
        widthDp: Float,
        heightDp: Float
    ): Float {
        var size = requestedSp
        while (size > MIN_FIT_SP &&
            height(context, text, size, widthDp) > heightDp * context.resources.displayMetrics.density
        ) {
            size = (size - 0.5f).coerceAtLeast(MIN_FIT_SP)
        }
        return size
    }

    fun dateAndTime(
        context: Context,
        date: String,
        time: String,
        requestedSp: Float,
        widthDp: Float,
        heightDp: Float
    ): Pair<Float, Float> {
        val density = context.resources.displayMetrics.density
        var timeSize = requestedSp
        fun dateSize() = (timeSize * 0.6875f).coerceAtLeast(MIN_FIT_SP)
        while (timeSize > MIN_FIT_SP &&
            height(context, date, dateSize(), widthDp) +
                height(context, time, timeSize, widthDp) + 10f * density > heightDp * density
        ) {
            timeSize = (timeSize - 0.5f).coerceAtLeast(MIN_FIT_SP)
        }
        return dateSize() to timeSize
    }

    private fun height(context: Context, text: String, sizeSp: Float, widthDp: Float): Int {
        val metrics = context.resources.displayMetrics
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, (widthDp * metrics.density).toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, LINE_SPACING)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build().height
    }
}
