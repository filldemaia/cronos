package com.filldemaia.cronos.radio.reliability

internal enum class AlarmEvidence { MISSING_TOKEN, UNKNOWN }

internal fun alarmEvidence(hasToken: Boolean): AlarmEvidence =
    if (hasToken) AlarmEvidence.UNKNOWN else AlarmEvidence.MISSING_TOKEN
