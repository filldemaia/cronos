package com.filldemaia.cronos.radio.domain.schedule

import com.filldemaia.cronos.radio.data.model.Schedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Càlcul pur de la propera engegada. No depèn d'Android i és la font de veritat
 * tant per l'ArmScheduler com per a la UI.
 *
 * Regles:
 *  - Es repeteix setmanalment segons [Schedule.repeatDays] (ISO 1..7).
 *  - Una programació d'un sol cop només s'arma si el seu instant és futur.
 *  - En canvi d'hora: una hora inexistent es desplaça endavant (com fa java.time);
 *    una hora ambigua s'interpreta amb el primer offset (hora d'estiu).
 *  - Si l'instant coincideix exactament amb "ara", s'agafa la setmana següent per
 *    no tornar a disparar immediatament.
 */
object NextOccurrenceCalculator {

    fun next(
        schedule: Schedule,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault()
    ): Instant? {
        if (!schedule.enabled) return null

        schedule.oneShotAtMillis?.let { at ->
            return if (at > now.toEpochMilli()) Instant.ofEpochMilli(at) else null
        }

        if (schedule.repeatDays.isEmpty()) return null

        val nowZoned = now.atZone(zone)
        for (dayOffset in 0..14) {
            val date = nowZoned.toLocalDate().plusDays(dayOffset.toLong())
            if (date.toString() == schedule.skipDate) continue
            if (!schedule.hasDay(date.dayOfWeek)) continue

            val candidate = date.atTime(schedule.hour, schedule.minute).atZone(zone).toInstant()
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    fun nextOneShot(
        hour: Int,
        minute: Int,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault()
    ): Instant {
        val nowZoned = now.atZone(zone)
        var candidate = nowZoned.toLocalDate()
            .atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            .atZone(zone)
        if (!candidate.toInstant().isAfter(now)) {
            candidate = nowZoned.toLocalDate().plusDays(1)
                .atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
                .atZone(zone)
        }
        return candidate.toInstant()
    }

    fun label(
        instant: Instant,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val target = instant.atZone(zone)
        val today = now.atZone(zone).toLocalDate()
        val dayDiff = ChronoUnit.DAYS.between(today, target.toLocalDate())
        val time = target.format(DateTimeFormatter.ofPattern("HH:mm"))
        return when (dayDiff) {
            0L -> "Avui a les $time"
            1L -> "Demà a les $time"
            in 2L..6L -> "${dayName(target.dayOfWeek)} a les $time"
            else -> target.format(
                DateTimeFormatter.ofPattern("d 'de' MMMM 'a les' HH:mm", Locale("ca"))
            )
        }
    }

    fun dayName(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Dilluns"
        DayOfWeek.TUESDAY -> "Dimarts"
        DayOfWeek.WEDNESDAY -> "Dimecres"
        DayOfWeek.THURSDAY -> "Dijous"
        DayOfWeek.FRIDAY -> "Divendres"
        DayOfWeek.SATURDAY -> "Dissabte"
        DayOfWeek.SUNDAY -> "Diumenge"
    }

    fun todayOrTomorrowAt(
        time: LocalTime,
        now: ZonedDateTime
    ): Instant {
        var candidate = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!candidate.toInstant().isAfter(now.toInstant())) {
            candidate = now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
        }
        return candidate.toInstant()
    }

    fun localDateAt(
        date: LocalDate,
        hour: Int,
        minute: Int,
        zone: ZoneId
    ): Instant = date.atTime(hour, minute).atZone(zone).toInstant()
}
