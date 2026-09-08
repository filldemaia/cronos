package com.filldemaia.cronos

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatació de la data en català, compartida entre la pantalla
 * principal i el widget gran per evitar duplicats.
 */
object CatalanDateFormatter {

    private val locale = Locale("ca", "ES")

    /** "Avui: dilluns, 10 d'agost del 2026" → "dilluns, 10 d'agost del 2026". */
    fun todayWithYear(date: Date = Date()): String =
        SimpleDateFormat("EEEE, d MMMM 'del 'yyyy", locale).format(date)

    /** "dilluns, 10 d'agost" (sense any, per al widget gran). */
    fun today(date: Date = Date()): String =
        SimpleDateFormat("EEEE, d MMMM", locale).format(date)
}
