package com.filldemaia.cronos.radio.reliability

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmEvidenceTest {
    @Test fun existingPendingIntentDoesNotProveAlarmRegistered() {
        assertEquals(AlarmEvidence.UNKNOWN, alarmEvidence(true))
    }

    @Test fun missingPendingIntentIsNotArmed() {
        assertEquals(AlarmEvidence.MISSING_TOKEN, alarmEvidence(false))
    }
}
