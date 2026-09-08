package com.filldemaia.cronos

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Programació centralitzada dels widgets.
 *
 * Una sola alarma **exacta** per minut (setExactAndAllowWhileIdle) per a
 * tota l'app: quan sona, [CronosWidgetTickReceiver] actualitza els tres
 * tipus de widget actius i en programa el següent tick. És el patró que
 * fan servir els widgets-rellotge: un únic despert curt per minut és el
 * mínim consum possible, i el sistema encara l'ajorna (màx. ~1 cada 9
 * minuts) durant el Doze profund de nit, quan ningú mira el widget.
 *
 * Requereix el permís USE_EXACT_ALARM (auto-concedit des d'API 33); si no
 * està disponible, es fa servir una alarma inexacta com a fallback i el
 * widget pot anar una mica endarrerit en mode estalvi. A més, el sistema
 * manté un refresc de seguretat cada 30 minuts (updatePeriodMillis) que
 * recupera la cadena si el sistema ha aturat l'app i les alarmes.
 */
object WidgetUpdateScheduler {

    const val ACTION_TICK = "com.filldemaia.cronos.ACTION_WIDGET_TICK"

    private const val REQUEST_CODE = 24680
    private const val WINDOW_FALLBACK_MS = 10_000L
    private const val TAG = "WidgetUpdateScheduler"

    /** Tots els tipus de widget que publiquem. */
    private val WIDGET_CLASSES = listOf(
        CronosWidget::class.java,
        CronosWidgetApple::class.java,
        CronosWidgetLarge::class.java,
    )

    /** Programa el pròxim minut (o reprograma el tick pendent). */
    fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e(TAG, "No s'ha pogut obtenir AlarmManager")
            return
        }

        val pendingIntent = tickPendingIntent(context)

        alarmManager.cancel(pendingIntent)

        val now = System.currentTimeMillis()
        val nextMinute = ((now / 60000) + 1) * 60000

        // Android 12+: si l'usuari ha revocat les alarmes exactes, no
        // esperem la SecurityException — anem directament a la fallback.
        if (!canScheduleExactAlarms(context)) {
            Log.w(
                TAG,
                "Alarmes exactes no disponibles; fem servir setWindow " +
                    "(el widget pot anar una mica endarrerit en estalvi de bateria)"
            )
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP, nextMinute, WINDOW_FALLBACK_MS, pendingIntent
            )
            return
        }

        try {
            // Exacta + AllowWhileIdle: dispara just al canvi de minut i no
            // queda ajornada per l'estalvi de bateria ordinari (només el
            // Doze profund la limita, i és el comportament correcte).
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMinute,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Cursa entre la comprovació i la crida: fem el que podem.
            Log.w(TAG, "Sense alarma exacta, fem servir setWindow: ${e.message}")
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP, nextMinute, WINDOW_FALLBACK_MS, pendingIntent
            )
        }
    }

    /**
     * Cert si podem demanar alarmes exactes en aquest dispositiu (API 31+).
     * Amb USE_EXACT_ALARM declarat hauria de ser sempre cert, però certs
     * fabricants i ajustos d'estalvi el poden impedir.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        return alarmManager?.canScheduleExactAlarms() ?: false
    }

    /** Cancel·la el tick programat (quan es treuen tots els widgets). */
    fun cancelUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        alarmManager.cancel(tickPendingIntent(context))
    }

    /** Actualitza tots els widgets actius dels tres tipus. */
    fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        for (widgetClass in WIDGET_CLASSES) {
            updateAllOf(context, appWidgetManager, widgetClass)
        }
    }

    /** Cert si encara queda algun widget actiu de qualsevol tipus. */
    fun anyWidgetsRemain(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return WIDGET_CLASSES.any { widgetClass ->
            appWidgetManager.getAppWidgetIds(ComponentName(context, widgetClass)).isNotEmpty()
        }
    }

    private fun tickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CronosWidgetTickReceiver::class.java).apply {
            action = ACTION_TICK
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateAllOf(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetClass: Class<*>,
    ) {
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, widgetClass))
        if (appWidgetIds.isEmpty()) return
        // El sistema mateix instancia la classe, per tant sempre té
        // constructor sense arguments.
        val provider = widgetClass.getDeclaredConstructor().newInstance() as CronosBaseWidget
        for (appWidgetId in appWidgetIds) {
            try {
                provider.updateAppWidget(context, appWidgetManager, appWidgetId)
            } catch (e: Exception) {
                // Un widget que falla no ha de trencar la cadena de ticks:
                // el següent tick ja està programat pel receiver (això és
                // una doble xarxa de seguretat).
                Log.e(TAG, "Error actualitzant ${widgetClass.simpleName} #$appWidgetId", e)
            }
        }
    }
}
