package com.filldemaia.cronos

import com.filldemaia.cronos.ui.theme.AfternoonPalette
import com.filldemaia.cronos.ui.theme.DayPhase
import com.filldemaia.cronos.ui.theme.EveningPalette
import com.filldemaia.cronos.ui.theme.MiddayPalette
import com.filldemaia.cronos.ui.theme.MorningPalette
import com.filldemaia.cronos.ui.theme.NightPalette
import com.filldemaia.cronos.ui.theme.paletteForPhase
import com.filldemaia.cronos.ui.theme.phaseFor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Franges de llum de la paleta a partir de la sortida i la posta de sol.
 * Les quatre franges de llum es reparteixen per igual entre tots dos
 * punts; la nit cobreix la resta.
 */
class PaletteRangesTest {

    // Dia de prova: sol de 8:00 (480 min) a 18:00 (1080 min), 600 min de llum.
    private val sun = SunTimes(sunriseMinutes = 480, sunsetMinutes = 1080)

    @Test
    fun `la nit cobreix abans de la sortida i des de la posta`() {
        assertEquals(DayPhase.NIGHT, phaseFor(sun, 0))
        assertEquals(DayPhase.NIGHT, phaseFor(sun, 479))
        assertEquals(DayPhase.NIGHT, phaseFor(sun, 1080))
        assertEquals(DayPhase.NIGHT, phaseFor(sun, 1439))
    }

    @Test
    fun `les quatre franges de llum es reparteixen entre sortida i posta`() {
        assertEquals(DayPhase.MORNING, phaseFor(sun, 480))
        assertEquals(DayPhase.MORNING, phaseFor(sun, 629))
        assertEquals(DayPhase.MIDDAY, phaseFor(sun, 630))
        assertEquals(DayPhase.MIDDAY, phaseFor(sun, 779))
        assertEquals(DayPhase.AFTERNOON, phaseFor(sun, 780))
        assertEquals(DayPhase.AFTERNOON, phaseFor(sun, 929))
        assertEquals(DayPhase.EVENING, phaseFor(sun, 930))
        assertEquals(DayPhase.EVENING, phaseFor(sun, 1079))
    }

    @Test
    fun `cada fase te la seva paleta`() {
        assertEquals(MorningPalette, paletteForPhase(DayPhase.MORNING))
        assertEquals(MiddayPalette, paletteForPhase(DayPhase.MIDDAY))
        assertEquals(AfternoonPalette, paletteForPhase(DayPhase.AFTERNOON))
        assertEquals(EveningPalette, paletteForPhase(DayPhase.EVENING))
        assertEquals(NightPalette, paletteForPhase(DayPhase.NIGHT))
    }

    @Test
    fun `sense llum diurna tot el dia es nit`() {
        val polarNight = SunTimes(sunriseMinutes = 0, sunsetMinutes = 0)
        (0..1439 step 7).forEach { m ->
            assertEquals("minut $m", DayPhase.NIGHT, phaseFor(polarNight, m))
        }
    }
}
