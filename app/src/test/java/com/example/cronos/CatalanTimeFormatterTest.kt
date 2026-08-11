package com.example.cronos

import com.example.cronos.ui.theme.AfternoonPalette
import com.example.cronos.ui.theme.EveningPalette
import com.example.cronos.ui.theme.MiddayPalette
import com.example.cronos.ui.theme.MorningPalette
import com.example.cronos.ui.theme.NightPalette
import com.example.cronos.ui.theme.paletteForHour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la lògica de l'hora catalana i de les franges horàries.
 * Els casos principals surten del README (format complet tradicional).
 */
class CatalanTimeFormatterTest {

    // --- Exemples del README ---

    @Test
    fun `hora exacta de migdia`() {
        assertEquals("Són les dotze del migdia", CatalanTimeFormatter.formatTime(12, 0))
    }

    @Test
    fun `un quart de quatre de la tarda`() {
        assertEquals("És un quart de quatre de la tarda", CatalanTimeFormatter.formatTime(15, 15))
    }

    @Test
    fun `dos quarts de nou`() {
        assertEquals("Són dos quarts de nou de la nit", CatalanTimeFormatter.formatTime(20, 30))
    }

    @Test
    fun `tres quarts d'onze de la nit`() {
        assertEquals("Són tres quarts d'onze de la nit", CatalanTimeFormatter.formatTime(22, 45))
    }

    @Test
    fun `vuit i sis minuts del mati`() {
        assertEquals("Són les vuit i sis minuts del matí", CatalanTimeFormatter.formatTime(8, 6))
    }

    @Test
    fun `set minuts per les tres de la tarda`() {
        assertEquals("Falten set minuts per les tres de la tarda", CatalanTimeFormatter.formatTime(14, 53))
    }

    // --- Casos límit ---

    @Test
    fun `mitjanit exacta`() {
        assertEquals("Són les dotze de la nit", CatalanTimeFormatter.formatTime(0, 0))
    }

    @Test
    fun `la una de migdia`() {
        assertEquals("És la una del migdia", CatalanTimeFormatter.formatTime(13, 0))
    }

    @Test
    fun `un minut abans de mitjanit`() {
        assertEquals("Falta un minut per les dotze de la nit", CatalanTimeFormatter.formatTime(23, 59))
    }

    @Test
    fun `dos minuts abans de mitjanit`() {
        assertEquals("Falten dos minuts per les dotze de la nit", CatalanTimeFormatter.formatTime(23, 58))
    }

    // --- Altres casos de la graella completa ---

    @Test
    fun `dos quarts d'onze del mati`() {
        assertEquals("Són dos quarts d'onze del matí", CatalanTimeFormatter.formatTime(10, 30))
    }

    @Test
    fun `set minuts per un quart de nou del mati`() {
        assertEquals("Falten set minuts per un quart de nou del matí", CatalanTimeFormatter.formatTime(8, 8))
    }

    @Test
    fun `un quart de set del mati`() {
        assertEquals("És un quart de set del matí", CatalanTimeFormatter.formatTime(6, 15))
    }

    @Test
    fun `tres quarts de sis del mati`() {
        assertEquals("Són tres quarts de sis del matí", CatalanTimeFormatter.formatTime(5, 45))
    }

    @Test
    fun `un quart i set minuts de cinc de la tarda`() {
        assertEquals("És un quart i set minuts de cinc de la tarda", CatalanTimeFormatter.formatTime(16, 22))
    }

    @Test
    fun `casos varis de la graella no retornen text buit`() {
        assertNotEquals("", CatalanTimeFormatter.formatTime(2, 3))
        assertNotEquals("", CatalanTimeFormatter.formatTime(2, 7))
        assertNotEquals("", CatalanTimeFormatter.formatTime(9, 38))
        assertNotEquals("", CatalanTimeFormatter.formatTime(18, 12))
    }

    // --- Segons per escrit (opcional) ---

    @Test
    fun `zero segons equival a no mostrar segons`() {
        assertEquals(CatalanTimeFormatter.formatTime(12, 0), CatalanTimeFormatter.formatTime(12, 0, 0))
        assertEquals(CatalanTimeFormatter.formatTime(14, 53), CatalanTimeFormatter.formatTime(14, 53, 0))
    }

    @Test
    fun `segons a una hora exacta`() {
        assertEquals(
            "Són les dotze i trenta segons del migdia",
            CatalanTimeFormatter.formatTime(12, 0, 30)
        )
    }

    @Test
    fun `un segon singular`() {
        assertEquals("Són les vuit i un segon del matí", CatalanTimeFormatter.formatTime(8, 0, 1))
    }

    @Test
    fun `segons amb minuts que falten`() {
        assertEquals(
            "Falten set minuts i tres segons per les tres de la tarda",
            CatalanTimeFormatter.formatTime(14, 53, 3)
        )
    }

    @Test
    fun `segons en un quart exacte`() {
        assertEquals(
            "És un quart i quaranta-cinc segons de deu del matí",
            CatalanTimeFormatter.formatTime(9, 15, 45)
        )
    }

    @Test
    fun `segons en tres quarts exactes`() {
        assertEquals(
            "Són tres quarts i vint segons de deu del matí",
            CatalanTimeFormatter.formatTime(9, 45, 20)
        )
    }

    @Test
    fun `exemple del usuari amb sis minuts i tres segons`() {
        assertEquals(
            "Falten sis minuts i tres segons per tres quarts de deu de la nit",
            CatalanTimeFormatter.formatTime(21, 39, 3)
        )
    }
}

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