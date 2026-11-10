package com.filldemaia.cronos.radio.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.filldemaia.cronos.radio.data.model.PermissionStatus

object PermissionManager {

    fun exactAlarmGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun notificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun batteryOptimizationIgnored(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun manufacturer(): String = Build.MANUFACTURER.orEmpty()

    fun isKnownOem(): Boolean {
        val m = manufacturer().lowercase()
        return listOf("xiaomi", "redmi", "samsung", "huawei", "honor", "oneplus", "oppo", "realme", "vivo", "asus", "meizu", "motorola")
            .any { m.contains(it) }
    }

    fun status(context: Context, oemAcknowledged: Boolean): PermissionStatus = PermissionStatus(
        exactAlarms = exactAlarmGranted(context),
        notifications = notificationsGranted(context),
        batteryOptimizationIgnored = batteryOptimizationIgnored(context),
        autoStartAcknowledged = oemAcknowledged || !isKnownOem(),
        manufacturer = manufacturer()
    )

    fun openExactAlarmSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return safeStart(
            context,
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            }
        )
    }

    // L'exempció de bateria és imprescindible perquè les alarmes exactes
    // arrenquin amb el dispositiu en repòs; l'usuari sempre decideix.
    @SuppressLint("BatteryLife")
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
        if (safeStart(context, direct)) return true
        return safeStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    fun openNotificationSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        return safeStart(context, intent) || safeStart(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
        )
    }

    fun openAppSettings(context: Context): Boolean = safeStart(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
    )

    /** Obre el panell de volum/sons del sistema. */
    fun openSoundSettings(context: Context): Boolean {
        val direct = Intent(Settings.ACTION_SOUND_SETTINGS)
        if (safeStart(context, direct)) return true
        return safeStart(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
        )
    }

    /** Obre la configuració de No molestar. */
    fun openDndSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val direct = Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
            if (safeStart(context, direct)) return true
        }
        return safeStart(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
        )
    }

    /** Volum del flux d'alarma. Si és 0, cap alarma no se sentirà. */
    fun alarmVolume(context: Context): Int = runCatching {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.getStreamVolume(AudioManager.STREAM_ALARM)
    }.getOrDefault(1)

    /**
     * No molestar pot silenciar el àudio de mitjans. Les alarmes programades
     * fan servir el flux d'alarma (USAGE_ALARM), que normalment hi passa per
     * sobre, però el mode "silenci total" també les pot mutar.
     */
    fun dndBlocksAlarms(context: Context): Boolean? = runCatching {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        when (manager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE -> true
            NotificationManager.INTERRUPTION_FILTER_ALL -> false
            else -> null
        }
    }.getOrNull()

    fun openAutoStartSettings(context: Context): Boolean {
        val manufacturer = manufacturer().lowercase()
        val candidates = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> listOf(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            )
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                ),
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            )
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> listOf(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                ),
                ComponentName(
                    "com.oplus.safecenter",
                    "com.oplus.safecenter.startupapp.StartupAppListActivity"
                ),
                ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"
                )
            )
            manufacturer.contains("vivo") -> listOf(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                ),
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            )
            manufacturer.contains("oneplus") -> listOf(
                ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            )
            manufacturer.contains("samsung") -> listOf(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                ),
                ComponentName(
                    "com.samsung.android.sm",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            )
            else -> emptyList()
        }
        for (component in candidates) {
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                if (safeStart(context, intent)) return true
            }
        }
        return openAppSettings(context)
    }

    fun openDontKillMyApp(context: Context): Boolean {
        val manufacturer = manufacturer().lowercase().ifBlank { "generic" }
        return safeStart(
            context,
            Intent(Intent.ACTION_VIEW, "https://dontkillmyapp.com/$manufacturer".toUri())
        )
    }

    fun openUrl(context: Context, url: String): Boolean = safeStart(context, Intent(Intent.ACTION_VIEW, url.toUri()))

    fun notificationChannelEnabled(context: Context, channelId: String): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(channelId) ?: return false
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun safeStart(context: Context, intent: Intent): Boolean = runCatching {
        if (intent.component != null &&
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null
        ) {
            return false
        }
        if (!(context is android.app.Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
