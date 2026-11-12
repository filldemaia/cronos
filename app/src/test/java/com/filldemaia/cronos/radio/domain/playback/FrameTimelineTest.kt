package com.filldemaia.cronos.radio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameTimelineTest {
    @Test fun durationLimitRemovesOnlyOlderFrames() {
        val timeline = FrameTimeline()
        repeat(6) { timeline.addFrame(it.toLong() * 100, 1_000_000) }
        timeline.pruneToDuration(3_000_000)
        assertEquals(listOf(3_000_000L, 4_000_000L, 5_000_000L), timeline.snapshot().map { it.startTimeUs })
        assertEquals(6_000_000L, timeline.latestTimeUs())
    }

    @Test fun shortRecordingKeepsEverything() {
        val timeline = FrameTimeline()
        repeat(2) { timeline.addFrame(it.toLong(), 1_000_000) }
        timeline.pruneToDuration(3_000_000)
        assertEquals(0L, timeline.firstFrame()?.startTimeUs)
    }
}
