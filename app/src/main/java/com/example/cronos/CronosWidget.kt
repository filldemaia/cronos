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
import com.example.cronos.R

/**
 * Widget per mostrar l'hora catalana a la pantalla d'inici
 * S'actualitza automàticament cada minut
 */
class CronosWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate cridat per ${appWidgetIds.size} widgets")
        
        // Actualitzar tots els widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        
        // Programar la propera actualització
        scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Widget activat - programant actualitzacions")
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Widget desactivat - cancel·lant actualitzacions")
        cancelUpdates(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        Log.d(TAG, "onReceive: ${intent.action}")
        
        when (intent.action) {
            ACTION_UPDATE_WIDGET -> {
                Log.d(TAG, "Actualitzant widget per alarma")
                
                // Obtenir tots els widgets i actualitzar-los
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, CronosWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                Log.d(TAG, "Actualitzant ${appWidgetIds.size} widgets")
                
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
                
                // Programar la propera actualització
                scheduleNextUpdate(context)
            }
        }
    }

    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e(TAG, "No s'ha pogut obtenir AlarmManager")
            return
        }

        val intent = Intent(context, CronosWidget::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel·lar qualsevol alarma prèvia
        alarmManager.cancel(pendingIntent)

        // Calcular el temps fins al proper minut
        val now = System.currentTimeMillis()
        val nextMinute = ((now / 60000) + 1) * 60000
        
        Log.d(TAG, "Programant alarma per: ${java.util.Date(nextMinute)}")

        // Programar l'alarma segons la versió d'Android
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ - necessita permís especial
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMinute,
                        pendingIntent
                    )
                    Log.d(TAG, "Alarma exacta programada (Android 12+)")
                } else {
                    Log.w(TAG, "No es pot programar alarma exacta - cal permís manual")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMinute,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6+
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMinute,
                    pendingIntent
                )
                Log.d(TAG, "Alarma exacta programada (Android 6+)")
            } else {
                // Android 5 i anteriors
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextMinute,
                    pendingIntent
                )
                Log.d(TAG, "Alarma exacta programada")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error programant alarma: ${e.message}")
            // Intentar amb alarma no exacta com a fallback
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextMinute,
                pendingIntent
            )
        }
    }

    private fun cancelUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, CronosWidget::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager?.cancel(pendingIntent)
        Log.d(TAG, "Actualitzacions cancel·lades")
    }

    companion object {
        private const val TAG = "HoracatWidget"
        private const val ACTION_UPDATE_WIDGET = "com.example.horacat.ACTION_UPDATE_WIDGET"
        private const val REQUEST_CODE = 12345

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Utilitzar CatalanTimeFormatter per obtenir l'hora en català
            val timeInCatalan = CatalanTimeFormatter.getCurrentTimeInCatalan()
            views.setTextViewText(R.id.widgetTimeTextView, timeInCatalan)
            
            Log.d(TAG, "Widget actualitzat: $timeInCatalan")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
