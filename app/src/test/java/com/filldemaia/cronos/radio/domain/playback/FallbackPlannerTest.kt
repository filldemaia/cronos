package com.filldemaia.cronos.radio.domain.playback

import com.filldemaia.cronos.radio.data.model.Station
import org.junit.Assert.assertEquals
import org.junit.Test

class FallbackPlannerTest {
    @Test fun primaryAlternativesAndFallbackPrecedeLocalTone() {
        val primary = Station(id = "p", name = "Primary", urls = listOf("primary", "alternate"))
        val fallback = Station(id = "f", name = "Fallback", urls = listOf("fallback"))
        val attempts = FallbackPlanner.plan(primary, listOf(fallback), retriesPerUrl = 2)
        assertEquals(listOf(AttemptKind.PRIMARY_URL, AttemptKind.PRIMARY_URL, AttemptKind.ALTERNATIVE_URL, AttemptKind.ALTERNATIVE_URL, AttemptKind.FALLBACK_STATION, AttemptKind.LOCAL_TONE), attempts.map { it.kind })
        assertEquals(listOf(0L, 1000L, 0L, 1000L, 0L, 0L), attempts.map { it.delayBeforeMs })
    }
}
