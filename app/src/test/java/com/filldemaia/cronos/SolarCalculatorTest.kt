package com.filldemaia.cronos

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Validació del càlcul de la sortida i la posta de sol amb valors
 * coneguts de Barcelona en els solsticis. Es força la zona horària a
 * Europe/Madrid perquè el test sigui determinista independentment de
 * la màquina on corri.
 */
class SolarCalculatorTest {

    private val barcelonaLat = 41.3874
    private val barcelonaLon = 2.1686

    private fun calendarAt(month: Int, day: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

    @Test
    fun `al solstici d'estiu el sol surt aviat i es pon tard`() {
        val sun = SolarCalculator.sunTimes(calendarAt(6, 21), barcelonaLat, barcelonaLon)
        // Sortida cap a les 6:18 i posta cap a les 21:27 (hora local CEST).
        assertTrue(
            "sortida inesperada: ${sun.sunriseMinutes} min",
            sun.sunriseMinutes in 360..405,
        )
        assertTrue(
            "posta inesperada: ${sun.sunsetMinutes} min",
            sun.sunsetMinutes in 1270..1310,
        )
    }

    @Test
    fun `al solstici d'hivern el sol surt tard i es pon d'hora`() {
        val sun = SolarCalculator.sunTimes(calendarAt(12, 21), barcelonaLat, barcelonaLon)
        // Sortida cap a les 8:13 i posta cap a les 17:25 (hora local CET).
        assertTrue(
            "sortida inesperada: ${sun.sunriseMinutes} min",
            sun.sunriseMinutes in 475..520,
        )
        assertTrue(
            "posta inesperada: ${sun.sunsetMinutes} min",
            sun.sunsetMinutes in 1030..1070,
        )
    }
}
