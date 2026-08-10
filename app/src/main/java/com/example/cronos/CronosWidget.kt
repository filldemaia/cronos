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

/**
 * Widget "estil Apple": una targeta arrodonida que canvia de color
 * segons el moment del dia, igual que la pantalla principal de l'app.
 */
class CronosWidgetApple : AppWidgetProvider() {

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
            val componentName = ComponentName(context, CronosWidgetApple::class.java)
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

        val intent = Intent(context, CronosWidgetApple::class.java).apply {
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
        val intent = Intent(context, CronosWidgetApple::class.java).apply {
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
        private const val TAG = "CronosWidgetApple"
        private const val ACTION_UPDATE_WIDGET = "com.example.cronos.ACTION_UPDATE_WIDGET_APPLE"
        private const val REQUEST_CODE = 22345

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_apple)

            val timeInCatalan = CatalanTimeFormatter.getCurrentTimeInCatalan()
            val palette = WidgetColors.getPaletteForCurrentHour()

            views.setTextViewText(R.id.appleWidgetTime, timeInCatalan)
            views.setTextColor(R.id.appleWidgetTime, palette.textColor)
            views.setInt(R.id.appleWidgetRoot, "setBackgroundResource", palette.backgroundDrawableRes)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
