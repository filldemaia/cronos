package com.filldemaia.cronos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import kotlin.math.roundToInt

/**
 * RemoteViews cannot host a custom TextView with a translucent overlay behind
 * the glyphs. Only the text is drawn onto a transparent bitmap, so the flag
 * and its radial vignette stay untouched below it.
 */
internal object WidgetTextBitmap {
    fun render(
        context: Context,
        text: String,
        requestedSp: Float,
        widthDp: Float,
        heightDp: Float
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val widthPx = (widthDp * metrics.density).roundToInt().coerceAtLeast(1)
        val heightPx = (heightDp * metrics.density).roundToInt().coerceAtLeast(1)
        // Both orientations together remain below the launcher's bitmap budget.
        val maxPixels = (metrics.widthPixels.toLong() * metrics.heightPixels / 2)
            .coerceIn(1, 1_000_000)
        val (bitmapWidth, bitmapHeight) = WidgetBitmapSize.fit(widthPx, heightPx, maxPixels)
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        bitmap.density = Bitmap.DENSITY_NONE
        val inset = 3f * metrics.density
        val sizeSp = WidgetTextFitter.time(context, text, requestedSp, widthDp - 6f, heightDp - 6f)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
            setShadowLayer(3f * metrics.density, 0f, 1f * metrics.density, context.getColor(R.color.widget_senyera_shadow))
        }
        val layout = StaticLayout.Builder.obtain(
            text, 0, text.length, paint, (widthPx - 2 * inset).toInt().coerceAtLeast(1)
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, WidgetTextFitter.LINE_SPACING)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()
        val canvas = Canvas(bitmap)
        canvas.scale(bitmapWidth.toFloat() / widthPx, bitmapHeight.toFloat() / heightPx)
        canvas.translate(inset, ((heightPx - layout.height) / 2f).coerceAtLeast(inset))
        paint.color = context.getColor(R.color.widget_senyera_text)
        layout.draw(canvas)
        return bitmap
    }
}
