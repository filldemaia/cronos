package com.filldemaia.cronos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews

/**
 * Base comuna dels tres widgets de Cronos: gestió del cicle de vida,
 * refresc en tocar el widget i actualització d'una instància concreta.
 * Les subclasses només defineixen el layout, la vista arrel i com es
 * omplen els textos.
 */
abstract class CronosBaseWidget : AppWidgetProvider() {

    /** Layout del widget. */
    protected abstract val layoutRes: Int

    /** Vista arrel, on penja el toc per refrescar el widget. */
    protected abstract val rootViewId: Int

    /** Omple els textos del widget amb l'hora/data actuals. */
    protected abstract fun fillViews(context: Context, views: RemoteViews)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate cridat per ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Si encara queden widgets d'altres tipus, l'actualització ha de
        // continuar: només es cancel·la quan no en queda cap.
        if (WidgetUpdateScheduler.anyWidgetsRemain(context)) {
            WidgetUpdateScheduler.scheduleNextUpdate(context)
        } else {
            WidgetUpdateScheduler.cancelUpdates(context)
        }
    }

    /** Actualitza una instància concreta del widget. */
    internal fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, layoutRes)
        fillViews(context, views)

        // Tocant el widget es refresca immediatament, per si l'alarma
        // s'ha endarrerit (bateria / estalvi).
        views.setOnClickPendingIntent(rootViewId, refreshPendingIntent(context, appWidgetId))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun refreshPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val refreshIntent = Intent(context, this.javaClass).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val TAG: String get() = javaClass.simpleName
}
