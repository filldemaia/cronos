package com.example.horacat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var currentTimeTextView: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentTimeTextView = findViewById(R.id.currentTimeTextView)

        updateTimePeriodically()
    }

    private fun updateTimePeriodically() {
        val updateTimeTask = object : Runnable {
            override fun run() {
                val currentTime = getCurrentTimeInCatalan()
                currentTimeTextView.text = currentTime
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(updateTimeTask)
    }

    private fun getCurrentTimeInCatalan(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val hourStartString = getHourStartString(hour)
        val hourString = getHourString(hour)
        val timeOfDayString = getTimeOfDayString(hour)
        val minuteString = getMinuteString(minute)
        val timeOclock = getTimeOclockString(hour)
        val timeOfDayForNextHour = getTimeOfDayForNextHourString(hour)

        return when {
            minute == 0 && second == 0 -> "$hourStartString $timeOfDayString"
            minute > 0 && minute <= 7 -> "$hourStartString i $minuteString $timeOfDayString"
            minute >= 8 && minute < 15 -> "$minuteString per un quart $hourString"
            minute == 15 -> "És un quart $hourString"
            minute > 15 && minute <= 22 -> "És un quart i $minuteString $hourString"
            minute >= 23 && minute < 30 -> "$minuteString per dos quarts $hourString"
            minute == 30 -> "Són dos quarts $hourString"
            minute > 30 && minute <= 37 -> "Són dos quarts i $minuteString $hourString"
            minute >= 38 && minute < 45 -> "$minuteString per tres quarts $hourString"
            minute == 45 -> "Són tres quarts $hourString"
            minute >= 45 && minute < 52 -> "Són tres quarts i $minuteString $hourString"
            minute >= 53 && minute < 60 -> "$minuteString per $timeOclock $timeOfDayForNextHour"
            else -> ""
        }
    }

    private fun getMinuteString(minute: Int): String {
        return when (minute) {
            1 -> "un minut"
            2 -> "dos minuts"
            3 -> "tres minuts"
            4 -> "quatre minuts"
            5 -> "cinc minuts"
            6 -> "sis minuts"
            7 -> "set minuts"
            8 -> "Falten set minuts"
            9 -> "Falten sis minuts"
            10 -> "Falten cinc minuts"
            11 -> "Falten quatre minuts"
            12 -> "Falten tres minuts"
            13 -> "Falten dos minuts"
            14 -> "Falta un minut"

            16 -> "un minut"
            17 -> "dos minuts"
            18 -> "tres minuts"
            19 -> "quatre minuts"
            20 -> "cinc minuts"
            21 -> "sis minuts"
            22 -> "set minuts"
            23 -> "Falten set minuts"
            24 -> "Falten sis minuts"
            25 -> "Falten cinc minuts"
            26 -> "Falten quatre minuts"
            27 -> "Falten tres minuts"
            28 -> "Falten dos minuts"
            29 -> "Falta un minut"

            31 -> "un minut"
            32 -> "dos minuts"
            33 -> "tres minuts"
            34 -> "quatre minuts"
            35 -> "cinc minuts"
            36 -> "sis minuts"
            37 -> "set minuts"
            38 -> "Falten set minuts"
            39 -> "Falten sis minuts"
            40 -> "Falten cinc minuts"
            41 -> "Falten quatre minuts"
            42 -> "Falten tres minuts"
            43 -> "Falten dos minuts"
            44 -> "Falta un minut"

            46 -> "un minut"
            47 -> "dos minuts"
            48 -> "tres minuts"
            49 -> "quatre minuts"
            50 -> "cinc minuts"
            51 -> "sis minuts"
            52 -> "set minuts"
            53 -> "Falten set minuts"
            54 -> "Falten sis minuts"
            55 -> "Falten cinc minuts"
            56 -> "Falten quatre minuts"
            57 -> "Falten tres minuts"
            58 -> "Falten dos minuts"
            59 -> "Falta un minut"
            else -> ""
        }
    }

    private fun getHourString(hour: Int): String {
        return when (hour) {
            0 -> "d'una de la matinada"
            1 -> "de dues de la matinada"
            2 -> "de tres de la matinada"
            3 -> "de quatre de la matinada"
            4 -> "de cinc de la matinada"
            5 -> "de sis del matí"
            6 -> "de set del matí"
            7 -> "de vuit del matí"
            8 -> "de nou del matí"
            9 -> "de deu del matí"
            10 -> "d'onze del matí"
            11 -> "de dotze del migdia"
            12 -> "d'una del migdia"
            13 -> "de dues del migdia"
            14 -> "de tres de la tarda"
            15 -> "de quatre de la tarda"
            16 -> "de cinc de la tarda"
            17 -> "de sis de la tarda"
            18 -> "de set del vespre"
            19 -> "de vuit del vespre"
            20 -> "de nou de la nit"
            21 -> "de deu de la nit"
            22 -> "d'onze de la nit"
            23 -> "de dotze de la nit"
            else -> ""
        }
    }

    private fun getHourStartString(hour: Int): String {
        return when (hour) {
            0 -> "Són les dotze"
            1 -> "És la una"
            2 -> "Són les dues"
            3 -> "Són les tres"
            4 -> "Són les quatre"
            5 -> "Són les cinc"
            6 -> "Són les sis"
            7 -> "Són les set"
            8 -> "Són les vuit"
            9 -> "Són les nou"
            10 -> "Són les deu"
            11 -> "Són les onze"
            12 -> "Són les dotze"
            13 -> "És la una"
            14 -> "Són les dues"
            15 -> "Són les tres"
            16 -> "Són les quatre"
            17 -> "Són les cinc"
            18 -> "Són les sis"
            19 -> "Són les set"
            20 -> "Són les vuit"
            21 -> "Són les nou"
            22 -> "Són les deu"
            23 -> "Són les onze"
            else -> ""
        }
    }

    private fun getTimeOclockString(hour: Int): String {
        return when (hour) {
            0 -> "la una"
            1 -> "les dues"
            2 -> "les tres"
            3 -> "les quatre"
            4 -> "les cinc"
            5 -> "les sis"
            6 -> "les set"
            7 -> "les vuit"
            8 -> "les nou"
            9 -> "les deu"
            10 -> "les onze"
            11 -> "les dotze"
            12 -> "la una"
            13 -> "les dues"
            14 -> "les tres"
            15 -> "les quatre"
            16 -> "les cinc"
            17 -> "les sis"
            18 -> "les set"
            19 -> "les vuit"
            20 -> "les nou"
            21 -> "les deu"
            22 -> "les onze"
            23 -> "les dotze"
            else -> ""
        }
    }

    private fun getTimeOfDayString(hour: Int): String {
        return when (hour) {
            0 -> "de la nit"
            1 -> "de la matinada"
            2 -> "de la matinada"
            3 -> "de la matinada"
            4 -> "de la matinada"
            5 -> "de la matinada"
            6 -> "del matí"
            7 -> "del matí"
            8 -> "del matí"
            9 -> "del matí"
            10 -> "del matí"
            11 -> "del matí"
            12 -> "del migdia"
            13 -> "del migdia"
            14 -> "del migdia"
            15 -> "de la tarda"
            16 -> "de la tarda"
            17 -> "de la tarda"
            18 -> "de la tarda"
            19 -> "del vespre"
            20 -> "del vespre"
            21 -> "de la nit"
            22 -> "de la nit"
            23 -> "de la nit"
            else -> ""
        }
    }

    private fun getTimeOfDayForNextHourString(hour: Int): String {
        return when (hour) {
            0 -> "de la matinada"
            1 -> "de la matinada"
            2 -> "de la matinada"
            3 -> "de la matinada"
            4 -> "de la matinada"
            5 -> "del matí"
            6 -> "del matí"
            7 -> "del matí"
            8 -> "del matí"
            9 -> "del matí"
            10 -> "del matí"
            11 -> "del migdia"
            12 -> "del migdia"
            13 -> "del migdia"
            14 -> "de la tarda"
            15 -> "de la tarda"
            16 -> "de la tarda"
            17 -> "de la tarda"
            18 -> "del vespre"
            19 -> "del vespre"
            20 -> "de la nit"
            21 -> "de la nit"
            22 -> "de la nit"
            23 -> "de la nit"
            else -> ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}