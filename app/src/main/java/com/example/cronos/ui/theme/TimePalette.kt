package com.example.cronos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import java.util.Calendar

/**
 * Paleta segons el moment del dia. Manté els colors canviants
 * que tenia l'app original: cada franja té la seva pròpia
 * personalitat cromàtica i les targetes en porten el pes.
 */
@Immutable
data class TimePalette(
    val momentLabel: String,
    // Fons (gradient vertical)
    val gradientTop: Color,
    val gradientMiddle: Color,
    val gradientBottom: Color,
    // Targetes
    val dateCard: Color,
    val mainCard: Color,
    val digitalCard: Color,
    // Text sobre les targetes
    val textOnCards: Color,
)

val MorningPalette = TimePalette(
    momentLabel = "Matí",
    gradientTop = Color(0xFFFFE5B4),
    gradientMiddle = Color(0xFFFFB6C1),
    gradientBottom = Color(0xFF87CEEB),
    dateCard = Color(0xFFE3F2FD),
    mainCard = Color(0xFFBBDEFB),
    digitalCard = Color(0xFF90CAF9),
    textOnCards = Color(0xFF01579B),
)

val MiddayPalette = TimePalette(
    momentLabel = "Migdia",
    gradientTop = Color(0xFFFFE5B4),
    gradientMiddle = Color(0xFFFFB6C1),
    gradientBottom = Color(0xFF87CEEB),
    dateCard = Color(0xFFFFF9C4),
    mainCard = Color(0xFFFFF59D),
    digitalCard = Color(0xFFFFF176),
    textOnCards = Color(0xFF4E342E),
)

val AfternoonPalette = TimePalette(
    momentLabel = "Tarda",
    gradientTop = Color(0xFFFFA500),
    gradientMiddle = Color(0xFFFF6347),
    gradientBottom = Color(0xFFFF69B4),
    dateCard = Color(0xFFFFEBEE),
    mainCard = Color(0xFFFFCDD2),
    digitalCard = Color(0xFFEF9A9A),
    textOnCards = Color(0xFFB71C1C),
)

val EveningPalette = TimePalette(
    momentLabel = "Vespre",
    gradientTop = Color(0xFFFFA500),
    gradientMiddle = Color(0xFFFF6347),
    gradientBottom = Color(0xFFFF69B4),
    dateCard = Color(0xFFFFEBEE),
    mainCard = Color(0xFFFFCDD2),
    digitalCard = Color(0xFFEF9A9A),
    textOnCards = Color(0xFFB71C1C),
)

val NightPalette = TimePalette(
    momentLabel = "Nit",
    gradientTop = Color(0xFF191970),
    gradientMiddle = Color(0xFF2F4F8F),
    gradientBottom = Color(0xFF483D8B),
    dateCard = Color(0xFF3F51B5),
    mainCard = Color(0xFF303F9F),
    digitalCard = Color(0xFF1A237E),
    textOnCards = Color.White,
)

/** CompositionLocal que exposa la paleta del moment del dia actual. */
val LocalTimePalette = staticCompositionLocalOf { MorningPalette }

fun paletteForHour(hour: Int): TimePalette = when (hour) {
    in 5..7 -> MorningPalette
    in 8..11 -> MorningPalette
    in 12..16 -> MiddayPalette
    in 17..19 -> AfternoonPalette
    in 19..20 -> EveningPalette
    else -> NightPalette
}

@Composable
fun currentTimePalette(): TimePalette {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return paletteForHour(hour)
}
