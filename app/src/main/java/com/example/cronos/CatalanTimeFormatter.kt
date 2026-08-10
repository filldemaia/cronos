package com.example.cronos

import java.util.Calendar


/**
 * Classe utilitària per formatar l'hora en català tradicional
 * Centralitza tota la lògica per evitar duplicació de codi
 *
 * [formatTime] accepta uns segons opcionals: quan es mostren, la frase
 * afegeix "i N segons" al lloc corresponent (ex. "Falten sis minuts i
 * tres segons per tres quarts de deu de la nit") i canvia cada segon.
 */
object CatalanTimeFormatter {

    /**
     * Retorna l'hora actual en format català tradicional (al minut).
     * Pensat per als widgets, que s'actualitzen un cop per minut.
     */
    fun getCurrentTimeInCatalan(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return formatTime(hour, minute)
    }

    /**
     * Formata l'hora donada en català tradicional.
     * [seconds] només s'afegeix quan és > 0.
     */
    fun formatTime(hour: Int, minute: Int, seconds: Int = 0): String {
        return when (minute) {
            0 -> formatExactHour(hour, seconds)
            in 1..7 -> formatPastHour(hour, minute, seconds)
            in 8..14 -> formatBeforeQuarter(hour, minute, seconds)
            15 -> formatQuarter(hour, seconds)
            in 16..22 -> formatPastQuarter(hour, minute, seconds)
            in 23..29 -> formatBeforeHalf(hour, minute, seconds)
            30 -> formatHalf(hour, seconds)
            in 31..37 -> formatPastHalf(hour, minute, seconds)
            in 38..44 -> formatBeforeThreeQuarters(hour, minute, seconds)
            45 -> formatThreeQuarters(hour, seconds)
            in 46..52 -> formatPastThreeQuarters(hour, minute, seconds)
            in 53..59 -> formatBeforeNextHour(hour, minute, seconds)
            else -> ""
        }
    }

    /** Fragment " i N segons", o buit quan no cal mostrar segons. */
    private fun secondsFragment(seconds: Int): String =
        if (seconds > 0) " i ${getSecondString(seconds)}" else ""

    private fun formatExactHour(hour: Int, seconds: Int): String {
        val hourStr = getHourStartString(hour)
        val timeOfDay = getTimeOfDayString(hour)
        return "$hourStr${secondsFragment(seconds)} $timeOfDay"
    }

    private fun formatPastHour(hour: Int, minute: Int, seconds: Int): String {
        val hourStr = getHourStartString(hour)
        val minuteStr = getMinuteString(minute)
        val timeOfDay = getTimeOfDayString(hour)
        return "$hourStr i $minuteStr${secondsFragment(seconds)} $timeOfDay"
    }

    private fun formatBeforeQuarter(hour: Int, minute: Int, seconds: Int): String {
        val minutesLeft = 15 - minute
        val minuteStr = getMinutesLeftString(minutesLeft)
        val hourStr = getNextHourString(hour)
        return "$minuteStr${secondsFragment(seconds)} per un quart $hourStr"
    }

    private fun formatQuarter(hour: Int, seconds: Int): String {
        val hourStr = getNextHourString(hour)
        return "És un quart${secondsFragment(seconds)} $hourStr"
    }

    private fun formatPastQuarter(hour: Int, minute: Int, seconds: Int): String {
        val minutesPast = minute - 15
        val minuteStr = getMinuteString(minutesPast)
        val hourStr = getNextHourString(hour)
        return "És un quart i $minuteStr${secondsFragment(seconds)} $hourStr"
    }

    private fun formatBeforeHalf(hour: Int, minute: Int, seconds: Int): String {
        val minutesLeft = 30 - minute
        val minuteStr = getMinutesLeftString(minutesLeft)
        val hourStr = getNextHourString(hour)
        return "$minuteStr${secondsFragment(seconds)} per dos quarts $hourStr"
    }

    private fun formatHalf(hour: Int, seconds: Int): String {
        val hourStr = getNextHourString(hour)
        return "Són dos quarts${secondsFragment(seconds)} $hourStr"
    }

    private fun formatPastHalf(hour: Int, minute: Int, seconds: Int): String {
        val minutesPast = minute - 30
        val minuteStr = getMinuteString(minutesPast)
        val hourStr = getNextHourString(hour)
        return "Són dos quarts i $minuteStr${secondsFragment(seconds)} $hourStr"
    }

    private fun formatBeforeThreeQuarters(hour: Int, minute: Int, seconds: Int): String {
        val minutesLeft = 45 - minute
        val minuteStr = getMinutesLeftString(minutesLeft)
        val hourStr = getNextHourString(hour)
        return "$minuteStr${secondsFragment(seconds)} per tres quarts $hourStr"
    }

    private fun formatThreeQuarters(hour: Int, seconds: Int): String {
        val hourStr = getNextHourString(hour)
        return "Són tres quarts${secondsFragment(seconds)} $hourStr"
    }

    private fun formatPastThreeQuarters(hour: Int, minute: Int, seconds: Int): String {
        val minutesPast = minute - 45
        val minuteStr = getMinuteString(minutesPast)
        val hourStr = getNextHourString(hour)
        return "Són tres quarts i $minuteStr${secondsFragment(seconds)} $hourStr"
    }

    private fun formatBeforeNextHour(hour: Int, minute: Int, seconds: Int): String {
        val minutesLeft = 60 - minute
        val minuteStr = getMinutesLeftString(minutesLeft)
        val nextHour = (hour + 1) % 24
        val timeStr = getTimeOclockString(nextHour)
        val timeOfDay = getTimeOfDayString(nextHour)
        return "$minuteStr${secondsFragment(seconds)} per $timeStr $timeOfDay"
    }

    private fun getMinuteString(minutes: Int): String {
        return when (minutes) {
            1 -> "un minut"
            2 -> "dos minuts"
            3 -> "tres minuts"
            4 -> "quatre minuts"
            5 -> "cinc minuts"
            6 -> "sis minuts"
            7 -> "set minuts"
            else -> "$minutes minuts"
        }
    }

    private fun getMinutesLeftString(minutes: Int): String {
        return when (minutes) {
            1 -> "Falta un minut"
            2 -> "Falten dos minuts"
            3 -> "Falten tres minuts"
            4 -> "Falten quatre minuts"
            5 -> "Falten cinc minuts"
            6 -> "Falten sis minuts"
            7 -> "Falten set minuts"
            else -> "Falten $minutes minuts"
        }
    }

    private fun getSecondString(seconds: Int): String {
        return if (seconds == 1) "un segon" else "${numberWords(seconds)} segons"
    }

    /** Nombres de l'1 al 59 en català per escrit. */
    private fun numberWords(n: Int): String {
        val units = arrayOf(
            "", "un", "dos", "tres", "quatre", "cinc", "sis", "set", "vuit", "nou"
        )
        val teens = arrayOf(
            "deu", "onze", "dotze", "tretze", "catorze", "quinze",
            "setze", "disset", "divuit", "dinou"
        )
        return when {
            n in 1..9 -> units[n]
            n in 10..19 -> teens[n - 10]
            n in 20..29 -> if (n == 20) "vint" else "vint-i-${units[n - 20]}"
            n in 30..59 -> {
                val tens = when (n / 10) {
                    3 -> "trenta"
                    4 -> "quaranta"
                    5 -> "cinquanta"
                    else -> ""
                }
                if (n % 10 == 0) tens else "$tens-${units[n % 10]}"
            }
            else -> "$n"
        }
    }

    private fun getHourStartString(hour: Int): String {
        return when (hour) {
            0, 12 -> "Són les dotze"
            1, 13 -> "És la una"
            2, 14 -> "Són les dues"
            3, 15 -> "Són les tres"
            4, 16 -> "Són les quatre"
            5, 17 -> "Són les cinc"
            6, 18 -> "Són les sis"
            7, 19 -> "Són les set"
            8, 20 -> "Són les vuit"
            9, 21 -> "Són les nou"
            10, 22 -> "Són les deu"
            11, 23 -> "Són les onze"
            else -> ""
        }
    }

    private fun getNextHourString(hour: Int): String {
        val nextHour = if (hour == 23) 0 else hour + 1
        return when (nextHour) {
            0 -> "de dotze de la nit"
            1 -> "d'una de la matinada"
            2 -> "de dues de la matinada"
            3 -> "de tres de la matinada"
            4 -> "de quatre de la matinada"
            5 -> "de cinc de la matinada"
            6 -> "de sis del matí"
            7 -> "de set del matí"
            8 -> "de vuit del matí"
            9 -> "de nou del matí"
            10 -> "de deu del matí"
            11 -> "d'onze del matí"
            12 -> "de dotze del migdia"
            13 -> "d'una del migdia"
            14 -> "de dues del migdia"
            15 -> "de tres de la tarda"
            16 -> "de quatre de la tarda"
            17 -> "de cinc de la tarda"
            18 -> "de sis de la tarda"
            19 -> "de set del vespre"
            20 -> "de vuit del vespre"
            21 -> "de nou de la nit"
            22 -> "de deu de la nit"
            23 -> "d'onze de la nit"
            else -> ""
        }
    }

    private fun getTimeOclockString(hour: Int): String {
        return when (hour) {
            0, 12 -> "les dotze"
            1, 13 -> "la una"
            2, 14 -> "les dues"
            3, 15 -> "les tres"
            4, 16 -> "les quatre"
            5, 17 -> "les cinc"
            6, 18 -> "les sis"
            7, 19 -> "les set"
            8, 20 -> "les vuit"
            9, 21 -> "les nou"
            10, 22 -> "les deu"
            11, 23 -> "les onze"
            else -> ""
        }
    }

    private fun getTimeOfDayString(hour: Int): String {
        return when (hour) {
            0 -> "de la nit"
            in 1..5 -> "de la matinada"
            in 6..11 -> "del matí"
            in 12..14 -> "del migdia"
            in 15..18 -> "de la tarda"
            in 19..20 -> "del vespre"
            in 21..23 -> "de la nit"
            else -> ""
        }
    }
}