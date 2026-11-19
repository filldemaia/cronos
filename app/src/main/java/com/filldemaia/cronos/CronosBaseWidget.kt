package com.filldemaia.cronos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    protected abstract val widgetType: WidgetType

    /** Omple els textos del widget amb l'hora/data actuals. */
    protected abstract fun fillViews(context: Context, views: RemoteViews, widthDp: Float, heightDp: Float)

    /** Same RemoteViews for the launcher and the in-app preview. */
    internal fun createViews(context: Context, widthDp: Float, heightDp: Float): RemoteViews =
        RemoteViews(context.packageName, layoutRes).also {
            fillViews(context, it, widthDp, heightDp)
        }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

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
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        fun size(key: String, fallback: Float) = options.getInt(key).takeIf { it > 0 }?.toFloat() ?: fallback
        val portrait = createViews(
            context,
            size(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widgetType.defaultWidthDp),
            size(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetType.defaultHeightDp)
        )
        val landscape = createViews(
            context,
            size(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widgetType.defaultWidthDp),
            size(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetType.defaultHeightDp)
        )
        // Tocant el widget es refresca immediatament, per si l'alarma
        // s'ha endarrerit (bateria / estalvi).
        // Configure each child BEFORE combining: a sized/oriented RemoteViews
        // rejects addAction(), including setOnClickPendingIntent().
        val refresh = refreshPendingIntent(context, appWidgetId)
        portrait.setOnClickPendingIntent(rootViewId, refresh)
        landscape.setOnClickPendingIntent(rootViewId, refresh)
        val views = RemoteViews(landscape, portrait)

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
