package com.example.cronos

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.example.cronos.ui.theme.paletteForHour
import java.util.Calendar

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
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdateScheduler.cancelUpdates(context)
    }

    companion object {
        private const val TAG = "CronosWidgetApple"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_apple)

            val timeInCatalan = CatalanTimeFormatter.getCurrentTimeInCatalan()
            val palette = paletteForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

            views.setTextViewText(R.id.appleWidgetTime, timeInCatalan)
            views.setTextColor(R.id.appleWidgetTime, palette.widgetTextColor)
            views.setInt(R.id.appleWidgetRoot, "setBackgroundResource", palette.widgetBackgroundRes)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}