package com.filldemaia.cronos

import java.util.Calendar
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Hores de sortida i posta de sol d'un dia concret, en minuts des de
 * mitjanit **en hora local del dispositiu** (el mateix rellotge que fa
 * servir [CatalanTimeFormatter]).
 */
data class SunTimes(
    val sunriseMinutes: Int,
    val sunsetMinutes: Int,
)

/**
 * Càlcul astronòmic de la sortida i la posta de sol a partir de la
 * latitud, la longitud i la data. És matemàtica pura: sense xarxa, sense
 * permisos, sense taules d'estacions.
 *
 * Es basa en tres peces clàssiques:
 *  1. La **declinació solar** (com d'alt és el sol sobre l'equador) i
 *     l'**equació del temps** (la petita asimetria de l'òrbita terrestre),
 *     ambdues funció del dia de l'any.
 *  2. El **migdia solar** en minuts de rellotge local: 12:00 corregit per
 *     la longitud (distància al meridià de la zona horària) i per
 *     l'equació del temps.
 *  3. La **meitat del dia de llum** (angle horari), a partir de la latitud
 *     i la declinació.
 *
 * La data ve d'un [Calendar] en hora local: en fem servir el dia de l'any
 * (DAY_OF_YEAR) i el desplaçament horari actual (ZONE_OFFSET, que ja inclou
 * l'horari d'estiu), de manera que el resultat queda en minuts de rellotge
 * de paret, directament comparable amb HOUR_OF_DAY i MINUTE.
 */
object SolarCalculator {

    /** Zenit oficial de sortida/posta de sol (90°50'). */
    private const val ZENITH_DEG = 90.833

    private val RAD = Math.PI / 180.0

    fun sunTimes(date: Calendar, latitude: Double, longitude: Double): SunTimes {
        val dayOfYear = date.get(Calendar.DAY_OF_YEAR)
        // ZONE_OFFSET és l'offset base (sense horari d'estiu); cal sumar-hi
        // DST_OFFSET per tenir el desplaçament real respecte d'UTC.
        val tzOffsetHours =
            (date.get(Calendar.ZONE_OFFSET) + date.get(Calendar.DST_OFFSET)).toDouble() / 3_600_000.0

        val declination = solarDeclination(dayOfYear)
        val equationOfTime = equationOfTimeMinutes(dayOfYear)

        // Migdia solar en minuts de rellotge local.
        val solarNoon = 720.0 - 4.0 * longitude + 60.0 * tzOffsetHours - equationOfTime

        val halfDayMinutes = halfDayLengthMinutes(latitude, declination)

        return SunTimes(
            sunriseMinutes = normalize(solarNoon - halfDayMinutes).roundToInt(),
            sunsetMinutes = normalize(solarNoon + halfDayMinutes).roundToInt(),
        )
    }

    /** Declinació solar en graus (±23,45°). */
    private fun solarDeclination(dayOfYear: Int): Double {
        val angle = 360.0 / 365.0 * (284 + dayOfYear)
        return 23.45 * sin(angle * RAD)
    }

    /** Equació del temps en minuts (aproximació de primer ordre). */
    private fun equationOfTimeMinutes(dayOfYear: Int): Double {
        val b = 360.0 / 365.0 * (dayOfYear - 81) * RAD
        return 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
    }

    /** Meitat del dia de llum en minuts, a partir de l'angle horari. */
    private fun halfDayLengthMinutes(latitude: Double, declination: Double): Double {
        val cosHourAngle = (
            cos(ZENITH_DEG * RAD) - sin(latitude * RAD) * sin(declination * RAD)
        ) / (cos(latitude * RAD) * cos(declination * RAD))
        // A les nostres latituds sempre cau dins (-1,1); els extrems són
        // per a dia/nit polars (mai aquí, però per robustesa).
        val hourAngleDeg = when {
            cosHourAngle < -1.0 -> 180.0 // dia polar
            cosHourAngle > 1.0 -> 0.0    // nit polar
            else -> Math.toDegrees(acos(cosHourAngle))
        }
        // 15° equivalen a 1 hora, per tant 1° equival a 4 minuts.
        return hourAngleDeg * 4.0
    }

    private fun normalize(minutes: Double): Double =
        ((minutes % 1440.0) + 1440.0) % 1440.0
}
