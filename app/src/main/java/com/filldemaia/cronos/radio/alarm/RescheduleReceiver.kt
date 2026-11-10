package com.filldemaia.cronos.radio.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.filldemaia.cronos.radio.RadioApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Rearma les alarmes quan el sistema ho demana.
 *
 * `LOCKED_BOOT_COMPLETED` és la peça clau: Android l'entrega just després de
 * reiniciar, abans que l'usuari desbloquegi el telèfon. Els despertadors
 * `setAlarmClock` es perden en reiniciar, i sense aquest camí l'app no podia
 * tornar a armar-los fins al primer desbloqueig (per això una alarma de les
 * 06:00 no va sonar després d'un reinici a les 03:10).
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val lockedBoot = intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> Unit
            else -> return
        }

        val pendingResult = goAsync()
        val app = context.applicationContext as RadioApp
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.alarmScheduler.armAll()
                if (!lockedBoot) {
                    runCatching { app.container.refreshReliabilityNotification() }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
