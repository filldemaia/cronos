package com.filldemaia.cronos.radio.domain.schedule

import com.filldemaia.cronos.radio.data.model.Schedule
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DaylightSavingTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val alarm = Schedule(hour = 2, minute = 30, stationId = "radio", repeatDays = listOf(7))

    @Test fun nonexistentTimeMovesForward() {
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), NextOccurrenceCalculator.next(alarm, Instant.parse("2026-03-28T12:00:00Z"), zone))
    }

    @Test fun ambiguousTimeUsesFirstOffset() {
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), NextOccurrenceCalculator.next(alarm, Instant.parse("2026-10-24T12:00:00Z"), zone))
    }
}
