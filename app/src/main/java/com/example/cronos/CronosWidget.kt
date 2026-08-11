package com.example.cronos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews

/**
 * Widget petit (2x2, transparent): mostra l'hora catalana en text blanc
 * amb ombra, perquè es llegeixi sobre qualsevol fons de pantalla sense
 * tapar-lo amb una targeta.
 */
class CronosWidget : AppWidgetProvider() {

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

    companion object {
        private const val TAG = "CronosWidget"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            val timeInCatalan = CatalanTimeFormatter.getCurrentTimeInCatalan()

            views.setTextViewText(R.id.smallWidgetTime, timeInCatalan)
            views.setTextColor(R.id.smallWidgetTime, Color.WHITE)

            // Tocant el widget es refresca immediatament, per si l'alarma
            // s'ha endarrerit (bateria / estalvi).
            val refreshIntent = Intent(context, CronosWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.smallWidgetRoot, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}