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
        val updated = current.copy(skipDate = now.atZone(zone).toLocalDate().plusDays(1).toString())
        val next = NextOccurrenceCalculator.next(updated, now, zone) ?: return false
        save(updated)
        return arm(updated, next)
    }
}
