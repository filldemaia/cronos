package com.example.cronos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Widget gran vertical (2x4, transparent): mostra la data i l'hora
 * tradicional en text blanc amb ombra, sense fons ni hora digital.
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
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        if (WidgetUpdateScheduler.anyWidgetsRemain(context)) {
            WidgetUpdateScheduler.scheduleNextUpdate(context)
        } else {
            WidgetUpdateScheduler.cancelUpdates(context)
        }
    }

    companion object {
        private const val TAG = "CronosWidgetLarge"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_large)

            val timeInCatalan = CatalanTimeFormatter.getCurrentTimeInCatalan()
            val now = Date()
            val dateText = SimpleDateFormat("EEEE, d MMMM", Locale("ca", "ES")).format(now)

            views.setTextViewText(R.id.largeWidgetDate, dateText)
            views.setTextViewText(R.id.largeWidgetCatalanTime, timeInCatalan)
            views.setTextColor(R.id.largeWidgetDate, Color.WHITE)
            views.setTextColor(R.id.largeWidgetCatalanTime, Color.WHITE)

            val refreshIntent = Intent(context, CronosWidgetLarge::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.largeWidgetRoot, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}