package com.filldemaia.cronos.radio.reliability

import com.filldemaia.cronos.radio.data.model.ReliabilityCheck
import com.filldemaia.cronos.radio.data.model.ReliabilityReport

internal fun notificationIssue(report: ReliabilityReport): ReliabilityCheck? =
    report.blockingIssues.firstOrNull()
        ?: report.warnings.firstOrNull { !it.id.startsWith("armed_") }
