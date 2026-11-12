package com.filldemaia.cronos.radio.playback

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.filldemaia.cronos.R
import com.filldemaia.cronos.radio.data.model.ReliabilityReport
import com.filldemaia.cronos.radio.reliability.notificationIssue
import com.filldemaia.cronos.radio.data.model.Schedule
import com.filldemaia.cronos.MainActivity

// Totes les crides a notify() estan protegides per canNotify() i runCatching,
// però lint no ho pot inferir i cal silenciar el fals positiu.
@SuppressLint("MissingPermission")
class NotificationHelper(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    fun createChannels() {
        val system = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PLAYBACK,
                context.getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_playback_desc)
                setShowBadge(false)
            }
        )
        // Elimina el canal d'avisos "alarma programada" de versions anteriors.
        // Ara les alarmes només viuen a la pantalla d'Alarmes.
        runCatching { system.deleteNotificationChannel(LEGACY_CHANNEL_SCHEDULE) }
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RELIABILITY,
                context.getString(R.string.notification_channel_reliability),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_reliability_desc)
            }
        )
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM_FALLBACK,
                context.getString(R.string.notification_alarm_fallback),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(true)
                description = "Avis de l'àudio d'alarma local"
            }
        )
        // Últim recurs: si el servei de reproducció no pot arrencar (per exemple,
        // el fabricant bloqueja l'inici en segon pla), aquest canal sona amb el
        // to d'alarma del sistema encara que l'app mori tot seguit.
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM_SAFETY,
                "Alarma d'emergència",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = "To d'alarma si la reproducció no pot arrencar"
            }
        )
    }

    fun playbackNotification(
        snapshot: PlaybackSnapshot,
        mediaSessionToken: android.support.v4.media.session.MediaSessionCompat.Token?
    ): Notification {
        val stationName = snapshot.station?.name ?: "Cronos"
        val title = if (snapshot.isLocalAlarm) "ALARMA · $stationName" else stationName
        val subtitle = when {
            snapshot.isLocalAlarm -> "El directe ha fallat. Atura el so manualment."
            snapshot.errorMessage != null -> snapshot.errorMessage
            snapshot.isFallbackStation -> "Emissora de reserva · ${snapshot.statusText}"
            snapshot.isBuffering -> "Connectant…"
            snapshot.isAtLive -> "En directe"
            else -> "${snapshot.delayLabel} respecte al directe"
        }
        val channel = if (snapshot.isLocalAlarm) CHANNEL_ALARM_FALLBACK else CHANNEL_PLAYBACK
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_radio)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(openAppIntent())
            .setCategory(if (snapshot.isLocalAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(snapshot.isPlaying || snapshot.isLocalAlarm)
            .setAutoCancel(false)
            .setSilent(!snapshot.isLocalAlarm)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (snapshot.isLocalAlarm) {
            builder.addAction(
                R.drawable.ic_stop_alarm,
                "Atura l'alarma",
                servicePendingIntent(PlaybackCommands.ACTION_STOP_LOCAL_ALARM, 11)
            )
        } else {
            builder.addAction(
                R.drawable.ic_replay,
                "-${snapshot.skipBackSeconds} s",
                servicePendingIntent(
                    PlaybackCommands.ACTION_SEEK_RELATIVE,
                    12,
                    -snapshot.skipBackSeconds * 1_000_000L
                )
            )
            builder.addAction(
                if (snapshot.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (snapshot.isPlaying) "Pausa" else "Reprodueix",
                servicePendingIntent(PlaybackCommands.ACTION_TOGGLE_PLAY_PAUSE, 13)
            )
            builder.addAction(
                R.drawable.ic_live,
                "Directe",
                servicePendingIntent(PlaybackCommands.ACTION_GO_LIVE, 14)
            )
            builder.addAction(
                R.drawable.ic_stop_alarm,
                "Atura",
                servicePendingIntent(PlaybackCommands.ACTION_STOP, 15)
            )
            if (mediaSessionToken != null) {
                builder.setStyle(
                    MediaStyle()
                        .setMediaSession(mediaSessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )
            }
            if (snapshot.hasTimeshift) {
                builder.setProgress(1000, (snapshot.window.progress(snapshot.positionUs) * 1000).toInt(), false)
            }
        }
        return builder.build()
    }

    fun postPlayback(
        snapshot: PlaybackSnapshot,
        mediaSessionToken: android.support.v4.media.session.MediaSessionCompat.Token?
    ) {
        if (!canNotify()) return
        runCatching {
            manager.notify(PlaybackCommands.NOTIFICATION_ID, playbackNotification(snapshot, mediaSessionToken))
        }
    }

    fun cancelPlayback() {
        manager.cancel(PlaybackCommands.NOTIFICATION_ID)
    }

    /**
     * Avís d'emergència amb el to d'alarma del sistema. Només s'envia si el
     * servei de reproducció no ha pogut arrencar; no és cap recordatori de
     * programació.
     */
    fun postAlarmFallback(schedule: Schedule) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ALARM_SAFETY)
            .setSmallIcon(R.drawable.ic_stat_radio)
            .setContentTitle("ALARMA Cronos · ${schedule.timeLabel()}")
            .setContentText("No s'ha pogut iniciar la ràdio. Atura aquest avís.")
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        runCatching { manager.notify(ALARM_SAFETY_ID, notification) }
    }

    fun postRearmFailure(schedule: Schedule) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_RELIABILITY)
            .setSmallIcon(R.drawable.ic_stat_radio)
            .setContentTitle("No s'ha pogut rearmar l'alarma")
            .setContentText("Revisa la programació de ${schedule.timeLabel()}.")
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(REARM_FAILURE_ID, notification)
    }

    fun updateReliability(
        report: ReliabilityReport,
        activeSchedules: Int
    ) {
        val issue = notificationIssue(report)
        if (activeSchedules == 0 || issue == null) {
            manager.cancel(RELIABILITY_ID)
            return
        }
        if (!canNotify()) return
        val text = issue.detail
        val notification = NotificationCompat.Builder(context, CHANNEL_RELIABILITY)
            .setSmallIcon(R.drawable.ic_stat_radio)
            .setContentTitle("Una alarma de Cronos no està garantida")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${report.summary}\n\n$text"))
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(RELIABILITY_ID, notification) }
    }

    fun cancelReliabilityWarning() {
        manager.cancel(RELIABILITY_ID)
    }

    fun canNotify(): Boolean = runCatching { manager.areNotificationsEnabled() }.getOrDefault(false)

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            9000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun servicePendingIntent(
        action: String,
        requestCode: Int,
        seekUs: Long? = null
    ): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).apply {
            this.action = action
            seekUs?.let { putExtra(PlaybackCommands.EXTRA_SEEK_US, it) }
        }
        return PendingIntent.getForegroundService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_PLAYBACK = "playback"
        private const val LEGACY_CHANNEL_SCHEDULE = "schedule_confirmations"
        const val CHANNEL_RELIABILITY = "alarm_reliability"
        const val CHANNEL_ALARM_FALLBACK = "alarm_fallback"
        const val CHANNEL_ALARM_SAFETY = "alarm_safety"
        const val RELIABILITY_ID = 4101
        const val ALARM_SAFETY_ID = 4102
        private const val REARM_FAILURE_ID = 4103
    }
}
