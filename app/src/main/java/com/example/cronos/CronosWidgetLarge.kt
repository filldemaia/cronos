package com.example.cronos

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Widget gran: mostra la data, l'hora en català i l'hora digital.
 * Pensat per ocupar més espai a la pantalla d'inici (mínim 4x2 cel·les).
 */
class CronosWidgetLarge : AppWidgetProvider() {

    override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate cridat per ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelUpdates(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CronosWidgetLarge::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            scheduleNextUpdate(context)
        }
    }

    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e(TAG, "No s'ha pogut obtenir AlarmManager")
            return
        }

        val intent = Intent(context, CronosWidgetLarge::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val now = System.currentTimeMillis()
        val nextMinute = ((now / 60000) + 1) * 60000

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextMinute,
                            pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextMinute,
                            pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMinute,
                        pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error programant alarma: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextMinute, pendingIntent)
        }
    }

    private fun cancelUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, CronosWidgetLarge::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
    }

    companion object {
        private const val TAG = "CronosWidgetLarge"
        private const val ACTION_UPDATE_WIDGET = "com.example.cronos.ACTION_UPDATE_WIDGET_LARGE"
        private const val REQUEST_CODE = 32345

        internal fun updateAppWidget(
                context: Context,
                appWidgetManager: AppWidgetManager,
                appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_large)

            val timeInCatalan = CatalanTimeFormatter.getCurrentTimeInCatalan()
            val now = Date()
            val digitalTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            val dateText = SimpleDateFormat("EEEE, d MMMM", Locale("ca", "ES")).format(now)

            views.setTextViewText(R.id.largeWidgetDate, dateText.replaceFirstChar { it.uppercase() })
            views.setTextViewText(R.id.largeWidgetCatalanTime, timeInCatalan)
            views.setTextViewText(R.id.largeWidgetDigitalTime, digitalTime)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
