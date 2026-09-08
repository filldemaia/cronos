package com.filldemaia.cronos

import com.filldemaia.cronos.ui.theme.AfternoonPalette
import com.filldemaia.cronos.ui.theme.EveningPalette
import com.filldemaia.cronos.ui.theme.MiddayPalette
import com.filldemaia.cronos.ui.theme.MorningPalette
import com.filldemaia.cronos.ui.theme.NightPalette
import com.filldemaia.cronos.ui.theme.paletteForHour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Franges horàries de la paleta: senceres, disjuntes i sense buits.
 */
class PaletteRangesTest {

    @Test
    fun `franges de mati de 6 a 11`() {
        (6..11).forEach { h -> assertEquals("hora $h", MorningPalette, paletteForHour(h)) }
    }

    @Test
    fun `franges de migdia de 12 a 16`() {
        (12..16).forEach { h -> assertEquals("hora $h", MiddayPalette, paletteForHour(h)) }
    }

    @Test
    fun `franges de tarda de 17 a 18`() {
        (17..18).forEach { h -> assertEquals("hora $h", AfternoonPalette, paletteForHour(h)) }
    }

    @Test
    fun `franges de vespre de 19 a 20`() {
        (19..20).forEach { h -> assertEquals("hora $h", EveningPalette, paletteForHour(h)) }
    }

    @Test
    fun `franges de nit a 0-5 i 21-23`() {
        (0..5).forEach { h -> assertEquals("hora $h", NightPalette, paletteForHour(h)) }
        (21..23).forEach { h -> assertEquals("hora $h", NightPalette, paletteForHour(h)) }
    }

    @Test
    fun `cada hora te una paleta valida`() {
        val paletes = setOf(MorningPalette, MiddayPalette, AfternoonPalette, EveningPalette, NightPalette)
        (0..23).forEach { h ->
            assertTrue("hora $h fora de les paletes conegudes", paletteForHour(h) in paletes)
        }
    }
}
