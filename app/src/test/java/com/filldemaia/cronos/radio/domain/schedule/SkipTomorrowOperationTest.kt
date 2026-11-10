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
        assertEquals(listOf("saved:edited:2026-11-11", "armed:9:2026-11-10T08:00:00Z"), events)
    }

    @Test fun failedArmCannotConfirmSuccess() = runBlocking {
        val schedule = Schedule(hour = 7, minute = 0, stationId = "radio")
        assertFalse(SkipTomorrowOperation.execute(schedule, Instant.parse("2026-11-09T12:00:00Z"), ZoneId.of("UTC"), {}, { _, _ -> false }))
    }
}
