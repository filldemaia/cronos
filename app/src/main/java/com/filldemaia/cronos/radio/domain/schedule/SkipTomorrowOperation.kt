package com.filldemaia.cronos.radio.domain.schedule

import com.filldemaia.cronos.radio.data.model.Schedule
import java.time.Instant
import java.time.ZoneId

internal object SkipTomorrowOperation {
    suspend fun execute(
        current: Schedule?,
        now: Instant,
        zone: ZoneId,
        save: suspend (Schedule) -> Unit,
        arm: suspend (Schedule, Instant) -> Boolean
    ): Boolean {
        if (current == null || current.isOneShot || !current.enabled) return false
        val nextUnskipped = NextOccurrenceCalculator.next(current.copy(skipDate = null), now, zone) ?: return false
        val updated = current.copy(skipDate = nextUnskipped.atZone(zone).toLocalDate().toString())
        val next = NextOccurrenceCalculator.next(updated, now, zone) ?: return false
        save(updated)
        return arm(updated, next)
    }
}

internal fun Schedule.withEnabledState(enabled: Boolean): Schedule =
    copy(enabled = enabled, skipDate = if (enabled) null else skipDate)

internal fun skippedOccurrence(schedule: Schedule, now: Instant, zone: ZoneId): Instant? {
    if (!schedule.enabled || schedule.isOneShot || schedule.skipDate == null) return null
    val next = NextOccurrenceCalculator.next(schedule.copy(skipDate = null), now, zone) ?: return null
    return next.takeIf { it.atZone(zone).toLocalDate().toString() == schedule.skipDate }
}

internal object RestoreSkippedOperation {
    suspend fun execute(
        current: Schedule?, now: Instant, zone: ZoneId,
        save: suspend (Schedule) -> Unit,
        arm: suspend (Schedule, Instant) -> Boolean
    ): Boolean {
        val next = current?.let { skippedOccurrence(it, now, zone) } ?: return false
        val updated = current.copy(skipDate = null)
        save(updated)
        return arm(updated, next)
    }
}
