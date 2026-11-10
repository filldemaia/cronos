package com.filldemaia.cronos.radio.data.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * Una programació d'engegada.
 *
 * [repeatDays] fa servir els valors ISO 1 (dilluns) ... 7 (diumenge).
 * Si [oneShotAtMillis] no és null, la programació és d'un sol cop i [repeatDays] s'ignora.
 */
@Serializable
data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val repeatDays: List<Int> = (1..7).toList(),
    val oneShotAtMillis: Long? = null,
    val stationId: String,
    val enabled: Boolean = true,
    val volumePercent: Int = 80,
    val fadeInSeconds: Int = 20,
    val stopAfterMinutes: Int? = null,
    val fallbackStationIds: List<String> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val skipDate: String? = null
) {
    val isOneShot: Boolean get() = oneShotAtMillis != null
    val isRepeating: Boolean get() = oneShotAtMillis == null
    val localTime: LocalTime get() = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    val volume: Float get() = (volumePercent.coerceIn(0, 100) / 100f)

    fun timeLabel(): String = "%02d:%02d".format(hour, minute)

    fun daysLabel(): String {
        if (isOneShot) return "Un sol cop"
        if (repeatDays.isEmpty()) return "Cap dia"
        val all = (1..7).toList()
        if (repeatDays.sorted() == all) return "Cada dia"
        if (repeatDays.sorted() == listOf(1, 2, 3, 4, 5)) return "Feiners"
        if (repeatDays.sorted() == listOf(6, 7)) return "Cap de setmana"
        val names = mapOf(
            1 to "Dl", 2 to "Dt", 3 to "Dc", 4 to "Dj", 5 to "Dv", 6 to "Ds", 7 to "Dg"
        )
        return repeatDays.sorted().mapNotNull(names::get).joinToString(" ")
    }

    fun hasDay(day: DayOfWeek): Boolean = repeatDays.contains(day.value)
}
