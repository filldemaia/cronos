package com.filldemaia.cronos.radio.reliability

import com.filldemaia.cronos.radio.data.model.CheckLevel
import com.filldemaia.cronos.radio.data.model.ReliabilityCheck
import com.filldemaia.cronos.radio.data.model.ReliabilityLevel
import com.filldemaia.cronos.radio.data.model.ReliabilityReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationIssueTest {
    @Test fun unknownRegistrationAloneDoesNotNotify() {
        val check = ReliabilityCheck("armed_123", "Alarma", "Estat desconegut", CheckLevel.WARNING)
        assertNull(notificationIssue(ReliabilityReport(ReliabilityLevel.YELLOW, listOf(check))))
    }

    @Test fun actualArmFailureStillNotifies() {
        val check = ReliabilityCheck("armed_123", "Alarma", "Pendent d'armar", CheckLevel.ERROR)
        assertEquals(check, notificationIssue(ReliabilityReport(ReliabilityLevel.RED, listOf(check))))
    }

    @Test fun otherWarningsStillNotify() {
        val unknown = ReliabilityCheck("armed_123", "Alarma", "Estat desconegut", CheckLevel.WARNING)
        val dnd = ReliabilityCheck("dnd", "No molestar", "Revisa-ho", CheckLevel.WARNING)
        assertEquals(dnd, notificationIssue(ReliabilityReport(ReliabilityLevel.YELLOW, listOf(unknown, dnd))))
    }
}
