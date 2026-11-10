package com.filldemaia.cronos.radio.alarm

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RearmRetryTest {
    @Test fun retriesFailure() = runBlocking {
        var calls = 0
        assertTrue(RearmRetry.run { ++calls == 2 })
        assertEquals(2, calls)
    }

    @Test fun persistentFailureIsReported() = runBlocking {
        var calls = 0
        assertFalse(RearmRetry.run { calls++; false })
        assertEquals(2, calls)
    }

    @Test fun exceptionIsRetried() = runBlocking {
        var calls = 0
        assertTrue(RearmRetry.run { if (++calls == 1) error("failed") else true })
        assertEquals(2, calls)
    }
}
