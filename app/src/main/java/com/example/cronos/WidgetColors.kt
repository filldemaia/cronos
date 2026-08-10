package com.example.cronos

import android.graphics.Color
import java.util.Calendar

/**
 * Colors compartits pels widgets, basats en les mateixes franges horàries
 * que fa servir MainActivity.kt per pintar la pantalla principal.
 */
object WidgetColors {

data class Palette(
        val backgroundDrawableRes: Int,
        val textColor: Int
)

        /**
         * Retorna el fons (un drawable arrodonit) i el color de text
         * que toquen segons l'hora actual.
         */
    fun getPaletteForCurrentHour(): Palette {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..7 -> Palette(R.drawable.widget_bg_alba, Color.parseColor("#5D4037"))
        in 8..11 -> Palette(R.drawable.widget_bg_mati, Color.parseColor("#01579B"))
        in 12..16 -> Palette(R.drawable.widget_bg_migdia, Color.parseColor("#4E342E"))
        in 17..19 -> Palette(R.drawable.widget_bg_vespre, Color.parseColor("#B71C1C"))
            else -> Palette(R.drawable.widget_bg_nit, Color.WHITE)
    }
}
}
