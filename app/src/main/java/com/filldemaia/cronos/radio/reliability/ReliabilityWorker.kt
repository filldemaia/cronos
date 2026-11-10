package com.filldemaia.cronos.radio.reliability

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.filldemaia.cronos.radio.RadioApp
import java.time.Duration

/**
 * Manteniment periòdic: rearma les alarmes que el sistema o un fabricant hagi
 * pogut perdre i actualitza l'avís de fiabilitat. No substitueix AlarmManager.
 */
class ReliabilityWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RadioApp
        return runCatching {
            app.container.alarmScheduler.armAll()
            app.container.refreshReliabilityNotification()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val UNIQUE_NAME = "radio-catalana-reliability"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReliabilityWorker>(Duration.ofHours(6))
                .setInitialDelay(Duration.ofMinutes(30))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
