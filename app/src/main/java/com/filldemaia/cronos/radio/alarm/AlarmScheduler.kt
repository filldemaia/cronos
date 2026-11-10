package com.filldemaia.cronos.radio.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.UserManager
import androidx.core.net.toUri
import com.filldemaia.cronos.radio.data.local.AppJson
import com.filldemaia.cronos.radio.data.local.DeviceProtectedStore
import com.filldemaia.cronos.radio.data.model.Schedule
import com.filldemaia.cronos.radio.data.repository.ScheduleRepository
import com.filldemaia.cronos.radio.data.repository.SettingsRepository
import com.filldemaia.cronos.radio.data.repository.StationRepository
import com.filldemaia.cronos.radio.domain.schedule.NextOccurrenceCalculator
import com.filldemaia.cronos.radio.playback.PlaybackCommands
import com.filldemaia.cronos.MainActivity
import com.filldemaia.cronos.radio.util.PermissionManager
import java.time.Instant
import kotlinx.serialization.encodeToString

data class ArmSummary(
    val armed: Int,
    val failed: Int,
    val nextAt: Instant?
)

class AlarmScheduler(
    private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val stationRepository: StationRepository,
    private val settingsRepository: SettingsRepository,
    private val deviceProtectedStore: DeviceProtectedStore
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Rearma totes les programacions. És segur cridar-ho abans del desbloqueig:
     * si DataStore encara no és accessible (direct boot), es fa servir la còpia
     * a l'emmagatzematge protegit pel dispositiu.
     */
    suspend fun armAll(now: Instant = Instant.now()): ArmSummary {
        val fromDataStore = if (userUnlocked()) {
            runCatching { scheduleRepository.current() }.getOrNull()
        } else {
            null
        }
        val schedules = fromDataStore ?: deviceProtectedStore.loadSchedules()

        if (fromDataStore != null) {
            runCatching { deviceProtectedStore.saveSchedules(fromDataStore) }
            runCatching { stationRepository.mirrorStations() }
            runCatching { deviceProtectedStore.saveSettings(settingsRepository.current()) }
        }

        if (!PermissionManager.exactAlarmGranted(context)) {
            return ArmSummary(
                armed = 0,
                failed = schedules.count { it.enabled },
                nextAt = null
            )
        }

        var armed = 0
        var failed = 0
        var nextOverall: Instant? = null
        for (schedule in schedules) {
            if (!schedule.enabled) {
                cancel(schedule.id)
                continue
            }
            val next = NextOccurrenceCalculator.next(schedule, now)
            if (next == null) {
                if (schedule.isOneShot) {
                    runCatching { scheduleRepository.save(schedule.copy(enabled = false)) }
                    runCatching { deviceProtectedStore.updateSchedule(schedule.copy(enabled = false)) }
                }
                failed++
                continue
            }
            val ok = arm(schedule, next, now)
            if (ok) {
                armed++
                if (nextOverall == null || next.isBefore(nextOverall)) nextOverall = next
            } else {
                failed++
            }
        }
        return ArmSummary(armed, failed, nextOverall)
    }

    suspend fun arm(
        schedule: Schedule,
        nextAt: Instant,
        now: Instant = Instant.now()
    ): Boolean {
        if (!PermissionManager.exactAlarmGranted(context)) return false
        return runCatching {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextAt.toEpochMilli(), showIntent()),
                operationFor(schedule, nextAt, isTest = false)
            )
            true
        }.getOrDefault(false)
    }

    /**
     * Prova manual: programa una alarma real d'aquí a uns segons. Passa pel
     * mateix BroadcastReceiver, servei, wake lock i cadena de fallbacks.
     */
    fun scheduleTestNow(
        schedule: Schedule,
        now: Instant = Instant.now(),
        secondsFromNow: Long = 5
    ): Boolean {
        if (!PermissionManager.exactAlarmGranted(context)) return false
        val triggerAt = now.plusSeconds(secondsFromNow.coerceAtLeast(1))
        // Un id diferent garanteix que la prova no substitueixi l'alarma real
        // (PendingIntent identity inclou el requestCode i la data).
        val testSchedule = schedule.copy(
            id = "test-${schedule.id}-${System.currentTimeMillis()}",
            enabled = true
        )
        return runCatching {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt.toEpochMilli(), showIntent()),
                operationFor(testSchedule, triggerAt, isTest = true)
            )
            true
        }.getOrDefault(false)
    }

    /**
     * S'executa quan l'alarma ha disparat. El rearmament s'ha de fer sempre,
     * abans d'intentar reproduir res: si el servei no pot arrencar, demà ha de
     * tornar a sonar igualment.
     */
    suspend fun handleAlarmFired(schedule: Schedule, isTest: Boolean) {
        if (isTest) return
        // En direct boot DataStore encara no és llegible: fem servir la còpia
        // que ha arribat dins de l'Intent.
        val current = if (userUnlocked()) {
            runCatching { scheduleRepository.find(schedule.id) }.getOrNull()
        } else {
            null
        } ?: deviceProtectedStore.loadSchedules().firstOrNull { it.id == schedule.id }
            ?: schedule
        if (!current.enabled) return
        if (current.isOneShot) {
            val disabled = current.copy(enabled = false)
            if (userUnlocked()) runCatching { scheduleRepository.save(disabled) }
            runCatching { deviceProtectedStore.updateSchedule(disabled) }
            return
        }
        val next = NextOccurrenceCalculator.next(current, Instant.now().plusMillis(500))
        if (next != null) {
            arm(current, next)
        } else {
            // Si no hi ha pròxima execució, no deixem l'alarma armada a mitges.
            cancel(current.id)
        }
    }

    private fun userUnlocked(): Boolean = runCatching {
        val manager = context.getSystemService(Context.USER_SERVICE) as UserManager
        manager.isUserUnlocked
    }.getOrDefault(true)

    fun cancel(scheduleId: String) {
        val intent = receiverIntent(scheduleId)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(scheduleId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    fun isArmed(scheduleId: String): Boolean {
        val intent = receiverIntent(scheduleId)
        return PendingIntent.getBroadcast(
            context,
            requestCode(scheduleId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
    }

    fun clearTestAlarm(scheduleId: String) {
        cancel(scheduleId)
    }

    fun operationFor(
        schedule: Schedule,
        plannedAt: Instant,
        isTest: Boolean
    ): PendingIntent {
        val intent = receiverIntent(schedule.id).apply {
            putExtra(PlaybackCommands.EXTRA_SCHEDULE_JSON, AppJson.encodeToString(schedule))
            putExtra(PlaybackCommands.EXTRA_PLANNED_AT, plannedAt.toEpochMilli())
            putExtra(PlaybackCommands.EXTRA_IS_TEST, isTest)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(schedule.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun receiverIntent(scheduleId: String): Intent =
        Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_FIRE_SCHEDULE
            data = "radiocatalana://schedule/$scheduleId".toUri()
        }

    private fun showIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            SHOW_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(scheduleId: String): Int = 20_000 + (scheduleId.hashCode() and 0x3FFFFF)

    companion object {
        const val ACTION_FIRE_SCHEDULE = "com.filldemaia.cronos.radio.action.FIRE_SCHEDULE"
        private const val SHOW_REQUEST_CODE = 7100
    }
}
