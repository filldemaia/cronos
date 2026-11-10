package com.filldemaia.cronos.radio.playback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAlarmFallbackTest {
    @Test fun generatorFailureStartsSystemTone() = runBlocking {
        val events = mutableListOf<String>()
        val started = LocalAlarmFallback.start(
            generated = { events += "generate"; error("disk") },
            system = { events += "system" },
            emergency = { events += "emergency" }
        )
        assertTrue(started)
        assertEquals(listOf("generate", "system"), events)
    }

    @Test fun generatedPlayerFailureStartsSystemTone() = runBlocking {
        val events = mutableListOf<String>()
        val started = LocalAlarmFallback.start(
            generated = { events += "generate"; events += "player"; error("player") },
            system = { events += "system" },
            emergency = { events += "emergency" }
        )
        assertTrue(started)
        assertEquals(listOf("generate", "player", "system"), events)
    }

    @Test fun bothPlayersFailTriggersEmergency() = runBlocking {
        val events = mutableListOf<String>()
        val started = LocalAlarmFallback.start(
            generated = { events += "generated"; error("disk") },
            system = { events += "system"; error("player") },
            emergency = { events += "emergency" }
        )
        assertFalse(started)
        assertEquals(listOf("generated", "system", "emergency"), events)
    }
}
