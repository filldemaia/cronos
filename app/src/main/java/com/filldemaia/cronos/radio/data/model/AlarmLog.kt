package com.filldemaia.cronos.radio.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AlarmOutcome {
    SUCCESS,
    FALLBACK_STATION,
    LOCAL_ALARM,
    ERROR
}

@Serializable
data class AlarmLog(
    val id: String,
    val scheduleId: String,
    val scheduleLabel: String,
    val stationName: String,
    val plannedAtMillis: Long,
    val actualAtMillis: Long,
    val outcome: AlarmOutcome,
    val message: String,
    val isTest: Boolean = false
)
