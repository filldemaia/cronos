package com.example.cronos

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Programació centralitzada dels widgets.
 *
 * Una sola alarma *inexacta* per minut (setAndAllowWhileIdle) per a tota
 * l'app: quan sona, [CronosWidgetTickReceiver] actualitza els tres tipus
 * de widget actius i en programa el següent tick. Això evita alarmes
 * exactes (que requereixen permisos restringits i despertaven el
 * dispositiu exactament cada minut) i una alarma per cada widget.
 */
object WidgetUpdateScheduler {

    const val ACTION_TICK = "com.example.cronos.ACTION_WIDGET_TICK"

    private const val REQUEST_CODE = 24680
    private const val TAG = "WidgetUpdateScheduler"

    /** Programa el pròxim minut (o reprograma el tick pendent). */
    fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e(TAG, "No s'ha pogut obtenir AlarmManager")
            return
        }

        val intent = Intent(context, CronosWidgetTickReceiver::class.java).apply {
            action = ACTION_TICK
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
            // Inexacta amb allow-while-idle: s'aprofita per no despertar
            // el dispositiu exactament cada minut i no cal cap permís.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMinute,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Error programant alarma: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextMinute, pendingIntent)
        }
    }

    /** Cancel·la el tick programat (quan es treuen tots els widgets). */
    fun cancelUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        val intent = Intent(context, CronosWidgetTickReceiver::class.java).apply {
            action = ACTION_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /** Actualitza tots els widgets actius dels tres tipus. */
    fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAllOf(context, appWidgetManager, CronosWidget::class.java)
        updateAllOf(context, appWidgetManager, CronosWidgetApple::class.java)
        updateAllOf(context, appWidgetManager, CronosWidgetLarge::class.java)
    }

    /** Cert si encara queda algun widget actiu de qualsevol dels tres tipus. */
    fun anyWidgetsRemain(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return listOf(
            CronosWidget::class.java,
            CronosWidgetApple::class.java,
            CronosWidgetLarge::class.java,
        ).any { widgetClass ->
            appWidgetManager.getAppWidgetIds(ComponentName(context, widgetClass)).isNotEmpty()
        }
    }

    private fun updateAllOf(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetClass: Class<*>,
    ) {
        val componentName = ComponentName(context, widgetClass)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        for (appWidgetId in appWidgetIds) {
            when (widgetClass) {
                CronosWidget::class.java ->
                    CronosWidget.updateAppWidget(context, appWidgetManager, appWidgetId)
                CronosWidgetApple::class.java ->
                    CronosWidgetApple.updateAppWidget(context, appWidgetManager, appWidgetId)
                CronosWidgetLarge::class.java ->
                    CronosWidgetLarge.updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}