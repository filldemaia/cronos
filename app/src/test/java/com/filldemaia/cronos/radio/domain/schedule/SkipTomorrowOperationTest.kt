package com.filldemaia.cronos.radio.domain.schedule

import com.filldemaia.cronos.radio.data.model.Schedule
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipTomorrowOperationTest {
    @Test fun editedScheduleUsesCurrentZoneAndIsSavedBeforeArming() = runBlocking {
        val edited = Schedule(id = "1", hour = 9, minute = 0, stationId = "edited")
        val events = mutableListOf<String>()
        val ok = SkipTomorrowOperation.execute(edited, Instant.parse("2026-11-09T23:30:00Z"), ZoneId.of("Europe/Madrid"),
            save = { events += "saved:${it.stationId}:${it.skipDate}" },
            arm = { schedule, instant -> events += "armed:${schedule.hour}:$instant"; true })
        assertTrue(ok)
        assertEquals(listOf("saved:edited:2026-11-10", "armed:9:2026-11-11T08:00:00Z"), events)
    }

    @Test fun failedArmCannotConfirmSuccess() = runBlocking {
        val schedule = Schedule(hour = 7, minute = 0, stationId = "radio")
        assertFalse(SkipTomorrowOperation.execute(schedule, Instant.parse("2026-11-09T12:00:00Z"), ZoneId.of("UTC"), {}, { _, _ -> false }))
    }

    @Test fun afterMidnightSkipsThisMorningOnly() = runBlocking {
        val schedule = Schedule(hour = 7, minute = 0, stationId = "radio")
        var saved: Schedule? = null
        var armed: Instant? = null
        assertTrue(SkipTomorrowOperation.execute(schedule, Instant.parse("2026-11-09T23:30:00Z"), ZoneId.of("Europe/Madrid"),
            { saved = it }, { _, next -> armed = next; true }))
        assertEquals("2026-11-10", saved?.skipDate)
        assertEquals(Instant.parse("2026-11-11T06:00:00Z"), armed)
    }

    @Test fun beforeMidnightSkipsTomorrowMorning() = runBlocking {
        val schedule = Schedule(hour = 7, minute = 0, stationId = "radio")
        var saved: Schedule? = null
        assertTrue(SkipTomorrowOperation.execute(schedule, Instant.parse("2026-11-09T21:00:00Z"), ZoneId.of("Europe/Madrid"),
            { saved = it }, { _, _ -> true }))
        assertEquals("2026-11-10", saved?.skipDate)
    }

    @Test fun restoringSkippedMorningArmsToday() = runBlocking {
        val schedule = Schedule(hour = 7, minute = 0, stationId = "radio", skipDate = "2026-11-10")
        var saved: Schedule? = null
        var armed: Instant? = null
        assertTrue(RestoreSkippedOperation.execute(schedule, Instant.parse("2026-11-09T23:30:00Z"), ZoneId.of("Europe/Madrid"),
            { saved = it }, { _, next -> armed = next; true }))
        assertEquals(null, saved?.skipDate)
        assertEquals(Instant.parse("2026-11-10T06:00:00Z"), armed)
    }

    @Test fun enablingClearsOldSkippedDate() {
        val schedule = Schedule(hour = 7, minute = 0, stationId = "radio", enabled = false, skipDate = "2026-11-10")
        val enabled = schedule.withEnabledState(true)
        assertEquals(null, enabled.skipDate)
        assertEquals(Instant.parse("2026-11-10T06:00:00Z"),
            NextOccurrenceCalculator.next(enabled, Instant.parse("2026-11-09T23:30:00Z"), ZoneId.of("Europe/Madrid")))
    }
}
