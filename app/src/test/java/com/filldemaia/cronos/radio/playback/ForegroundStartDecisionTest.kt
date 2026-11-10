package com.filldemaia.cronos.radio.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundStartDecisionTest {
    @Test fun failureReportsAndCallerContinues() {
        val events = mutableListOf<String>()
        ForegroundStartDecision.proceed({ events += "foreground"; error("denied") }, { events += "warning" })
        events += "playback"
        assertEquals(listOf("foreground", "warning", "playback"), events)
    }

    @Test fun successDoesNotWarn() {
        val events = mutableListOf<String>()
        ForegroundStartDecision.proceed({ events += "foreground" }, { events += "warning" })
        events += "playback"
        assertEquals(listOf("foreground", "playback"), events)
    }
}
