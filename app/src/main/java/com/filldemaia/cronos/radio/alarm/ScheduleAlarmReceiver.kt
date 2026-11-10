package com.filldemaia.cronos.radio.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.filldemaia.cronos.radio.RadioApp
import com.filldemaia.cronos.radio.data.local.AppJson
import com.filldemaia.cronos.radio.data.model.AlarmLog
import com.filldemaia.cronos.radio.data.model.AlarmOutcome
import com.filldemaia.cronos.radio.data.model.Schedule
import com.filldemaia.cronos.radio.playback.PlaybackCommands
import com.filldemaia.cronos.radio.playback.PlaybackService
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE_SCHEDULE) return

        val raw = intent.getStringExtra(PlaybackCommands.EXTRA_SCHEDULE_JSON) ?: return
        val schedule = runCatching {
            AppJson.decodeFromString<Schedule>(raw)
        }.getOrNull() ?: return
        val plannedAt = intent.getLongExtra(PlaybackCommands.EXTRA_PLANNED_AT, System.currentTimeMillis())
        val isTest = intent.getBooleanExtra(PlaybackCommands.EXTRA_IS_TEST, false)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Cronos:ScheduledAlarm"
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }

        val startIntent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackCommands.ACTION_START_SCHEDULE
            putExtra(PlaybackCommands.EXTRA_SCHEDULE_JSON, raw)
            putExtra(PlaybackCommands.EXTRA_PLANNED_AT, plannedAt)
            putExtra(PlaybackCommands.EXTRA_IS_TEST, isTest)
        }

        val app = context.applicationContext as RadioApp
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // 1. Rearmar SEMPRE primer. Si el servei falla, demà ha de sonar.
                runCatching { app.container.alarmScheduler.handleAlarmFired(schedule, isTest) }

                // 2. Arrencar la reproducció.
                try {
                    ContextCompat.startForegroundService(context, startIntent)
                } catch (error: Exception) {
                    // Si el sistema bloqueja l'inici, l'error queda registrat i
                    // intentem un avís d'alarma audible com a últim recurs.
                    runCatching {
                        app.container.reliabilityRepository.addLog(
                            AlarmLog(
                                id = UUID.randomUUID().toString(),
                                scheduleId = schedule.id,
                                scheduleLabel = schedule.timeLabel(),
                                stationName = schedule.stationId,
                                plannedAtMillis = plannedAt,
                                actualAtMillis = System.currentTimeMillis(),
                                outcome = AlarmOutcome.ERROR,
                                message = "No s'ha pogut iniciar el servei: ${error.message}",
                                isTest = isTest
                            )
                        )
                    }
                    runCatching { app.container.notificationHelper.postAlarmFallback(schedule) }
                }

                if (!isTest && isUserUnlocked(context)) {
                    runCatching { app.container.refreshReliabilityNotification() }
                }

                // 3. Donar marge perquè el servei adquireixi el seu propi wake lock.
                delay(WAKE_LOCK_GRACE_MS)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }

    private fun isUserUnlocked(context: Context): Boolean = runCatching {
        (context.getSystemService(Context.USER_SERVICE) as UserManager).isUserUnlocked
    }.getOrDefault(true)

    private companion object {
        const val WAKE_LOCK_TIMEOUT_MS = 120_000L
        const val WAKE_LOCK_GRACE_MS = 3_000L
    }
}
