package com.filldemaia.cronos.radio.domain.schedule

import com.filldemaia.cronos.radio.data.model.Schedule
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SkipTomorrowTest {
    private val zone = ZoneId.of("Europe/Madrid")

    @Test fun skipOneDayDoesNotDisableRepeatingAlarm() {
        val schedule = Schedule(hour = 7, minute = 0, stationId = "radio", repeatDays = (1..7).toList(), skipDate = "2026-11-10")
        val now = Instant.parse("2026-11-09T20:00:00Z")
        assertEquals(Instant.parse("2026-11-11T06:00:00Z"), NextOccurrenceCalculator.next(schedule, now, zone))
    }

    @Test fun weeklyAlarmSkipsToFollowingWeek() {
        val schedule = Schedule(hour = 7, minute = 0, stationId = "radio", repeatDays = listOf(2), skipDate = "2026-11-10")
        assertEquals(Instant.parse("2026-11-17T06:00:00Z"), NextOccurrenceCalculator.next(schedule, Instant.parse("2026-11-09T20:00:00Z"), zone))
    }
}
