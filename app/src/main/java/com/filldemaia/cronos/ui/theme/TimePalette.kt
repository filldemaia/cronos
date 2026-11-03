package com.filldemaia.cronos.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.filldemaia.cronos.R
import com.filldemaia.cronos.SettingsRepository
import com.filldemaia.cronos.SolarCalculator
import com.filldemaia.cronos.SunTimes
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
    // Widgets: fons arrodonit de la franja horària
    val widgetBackgroundRes: Int,
    // Si el fons és fosc, els textos porten ombra per llegir-se
    val textShadow: Boolean = false,
)

val MorningPalette = TimePalette(
    gradientTop = Color(0xFFFFE5B4),
    gradientMiddle = Color(0xFFFFB6C1),
    gradientBottom = Color(0xFF87CEEB),
    widgetBackgroundRes = R.drawable.widget_bg_mati,
)

val MiddayPalette = TimePalette(
    gradientTop = Color(0xFFFFE5B4),
    gradientMiddle = Color(0xFFFFB6C1),
    gradientBottom = Color(0xFF87CEEB),
    widgetBackgroundRes = R.drawable.widget_bg_migdia,
)

val AfternoonPalette = TimePalette(
    gradientTop = Color(0xFFFFA500),
    gradientMiddle = Color(0xFFFF6347),
    gradientBottom = Color(0xFFFF69B4),
    widgetBackgroundRes = R.drawable.widget_bg_vespre,
)

val EveningPalette = TimePalette(
    gradientTop = Color(0xFFFFA500),
    gradientMiddle = Color(0xFFFF6347),
    gradientBottom = Color(0xFFFF69B4),
    widgetBackgroundRes = R.drawable.widget_bg_vespre,
)

val NightPalette = TimePalette(
    gradientTop = Color(0xFF191970),
    gradientMiddle = Color(0xFF2F4F8F),
    gradientBottom = Color(0xFF483D8B),
    widgetBackgroundRes = R.drawable.widget_bg_nit,
    textShadow = true,
)

/** CompositionLocal que exposa la paleta del moment del dia actual. */
val LocalTimePalette = staticCompositionLocalOf { MorningPalette }

/** Fase del dia que condueix la paleta de colors. */
enum class DayPhase { MORNING, MIDDAY, AFTERNOON, EVENING, NIGHT }

/**
 * Fase del dia a partir de la sortida i la posta de sol. La llum del
 * dia es reparteix en quatre franges iguals (matí, migdia, tarda i
 * vespre) entre la sortida i la posta; la nit cobreix la resta. Així,
 * els canvis segueixen el sol real i no una taula fixa d'hores: a
 * l'hivern la nit comença cap a les 5-6 i a l'estiu cap a les 9-10.
 *
 * [minutesSinceMidnight] són minuts de rellotge local.
 */
fun phaseFor(sunTimes: SunTimes, minutesSinceMidnight: Int): DayPhase {
    val sunrise = sunTimes.sunriseMinutes
    val sunset = sunTimes.sunsetMinutes
    val daylight = (sunset - sunrise).toDouble()

    return when {
        // Nit polar o valors invàlids: tot el dia fosc.
        daylight <= 0 -> DayPhase.NIGHT
        minutesSinceMidnight < sunrise || minutesSinceMidnight >= sunset -> DayPhase.NIGHT
        minutesSinceMidnight < sunrise + daylight * 0.25 -> DayPhase.MORNING
        minutesSinceMidnight < sunrise + daylight * 0.50 -> DayPhase.MIDDAY
        minutesSinceMidnight < sunrise + daylight * 0.75 -> DayPhase.AFTERNOON
        else -> DayPhase.EVENING
    }
}

/** Paleta corresponent a una fase del dia. */
fun paletteForPhase(phase: DayPhase): TimePalette = when (phase) {
    DayPhase.MORNING -> MorningPalette
    DayPhase.MIDDAY -> MiddayPalette
    DayPhase.AFTERNOON -> AfternoonPalette
    DayPhase.EVENING -> EveningPalette
    DayPhase.NIGHT -> NightPalette
}

/**
 * Paleta per a l'instant actual, calculant la sortida i la posta de sol
 * amb la ubicació desada a [settings]. És el punt d'entrada comú de
 * l'app i dels widgets.
 */
fun paletteForNow(settings: SettingsRepository): TimePalette {
    val now = Calendar.getInstance()
    val sunTimes = SolarCalculator.sunTimes(
        now, settings.locationLatitude, settings.locationLongitude,
    )
    val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    return paletteForPhase(phaseFor(sunTimes, minutes))
}

