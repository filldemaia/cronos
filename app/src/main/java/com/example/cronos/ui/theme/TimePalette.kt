package com.example.cronos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.cronos.R
import java.util.Calendar

/**
 * Paleta segons el moment del dia. Font de veritat única per als
 * colors de l'app: la pantalla principal en fa servir el gradient i
 * els widgets la targeta arrodonida + el color de text, de manera que
 * app i widgets sempre coincideixen en la mateixa franja horària.
 */
@Immutable
data class TimePalette(
    // Fons (gradient vertical) de la pantalla principal
    val gradientTop: Color,
    val gradientMiddle: Color,
    val gradientBottom: Color,
    // Widgets: fons arrodonit i color de text per franja
    val widgetBackgroundRes: Int,
    val widgetTextColor: Int,
)

val MorningPalette = TimePalette(
    gradientTop = Color(0xFFFFE5B4),
    gradientMiddle = Color(0xFFFFB6C1),
    gradientBottom = Color(0xFF87CEEB),
    widgetBackgroundRes = R.drawable.widget_bg_mati,
    widgetTextColor = 0xFF01579B.toInt(),
)

val MiddayPalette = TimePalette(
    gradientTop = Color(0xFFFFE5B4),
    gradientMiddle = Color(0xFFFFB6C1),
    gradientBottom = Color(0xFF87CEEB),
    widgetBackgroundRes = R.drawable.widget_bg_migdia,
    widgetTextColor = 0xFF4E342E.toInt(),
)

val AfternoonPalette = TimePalette(
    gradientTop = Color(0xFFFFA500),
    gradientMiddle = Color(0xFFFF6347),
    gradientBottom = Color(0xFFFF69B4),
    widgetBackgroundRes = R.drawable.widget_bg_vespre,
    widgetTextColor = 0xFFB71C1C.toInt(),
)

val EveningPalette = TimePalette(
    gradientTop = Color(0xFFFFA500),
    gradientMiddle = Color(0xFFFF6347),
    gradientBottom = Color(0xFFFF69B4),
    widgetBackgroundRes = R.drawable.widget_bg_vespre,
    widgetTextColor = 0xFFB71C1C.toInt(),
)

val NightPalette = TimePalette(
    gradientTop = Color(0xFF191970),
    gradientMiddle = Color(0xFF2F4F8F),
    gradientBottom = Color(0xFF483D8B),
    widgetBackgroundRes = R.drawable.widget_bg_nit,
    widgetTextColor = 0xFFFFFFFF.toInt(),
)

/** CompositionLocal que exposa la paleta del moment del dia actual. */
val LocalTimePalette = staticCompositionLocalOf { MorningPalette }

/**
 * Paleta per una hora concreta. Franges disjuntes i senceres:
 * 0-4 i 21-23 → nit, 5-11 → matí, 12-16 → migdia, 17-18 → tarda,
 * 19-20 → vespre.
 */
fun paletteForHour(hour: Int): TimePalette = when (hour) {
    in 5..11 -> MorningPalette
    in 12..16 -> MiddayPalette
    in 17..18 -> AfternoonPalette
    in 19..20 -> EveningPalette
    else -> NightPalette
}

@Composable
fun currentTimePalette(): TimePalette {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return paletteForHour(hour)
}