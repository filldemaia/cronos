package com.filldemaia.cronos.radio.domain.schedule

import com.filldemaia.cronos.radio.data.model.Schedule
import java.time.Instant
import java.time.ZoneId

data class PlannedAlarm(
    val schedule: Schedule,
    val nextAt: Instant
)

object NextAlarmPlanner {

    fun plan(
        schedules: List<Schedule>,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PlannedAlarm> = schedules
        .asSequence()
        .filter { it.enabled }
        .mapNotNull { schedule ->
            NextOccurrenceCalculator.next(schedule, now, zone)
                ?.let { PlannedAlarm(schedule, it) }
        }
        .sortedBy { it.nextAt }
        .toList()

    fun nextForStation(
        schedules: List<Schedule>,
        stationId: String,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault()
    ): PlannedAlarm? = plan(schedules, now, zone)
        .firstOrNull { it.schedule.stationId == stationId }
}
