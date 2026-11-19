package com.filldemaia.cronos

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.widget.RemoteViews

/**
 * Widget gran vertical (4x2, transparent): mostra la data i l'hora
 * tradicional en text blanc amb ombra, sense fons ni hora digital.
 */
class CronosWidgetLarge : CronosBaseWidget() {

    override val layoutRes: Int = R.layout.widget_layout_date_time
    override val rootViewId: Int = R.id.largeWidgetRoot
    override val widgetType = WidgetType.DATE_AND_TIME

    override fun fillViews(context: Context, views: RemoteViews, widthDp: Float, heightDp: Float) {
        val date = CatalanDateFormatter.today()
        val time = CatalanTimeFormatter.getCurrentTimeInCatalan()
        val (dateSize, timeSize) = WidgetTextFitter.dateAndTime(
            context, date, time, SettingsRepository(context).widgetTextSize(widgetType),
            widthDp - 20f, heightDp - 20f
        )
        views.setTextViewText(R.id.largeWidgetDate, date)
        views.setTextViewText(R.id.largeWidgetCatalanTime, time)
        views.setTextColor(R.id.largeWidgetDate, Color.WHITE)
        views.setTextColor(R.id.largeWidgetCatalanTime, Color.WHITE)
        views.setTextViewTextSize(R.id.largeWidgetDate, TypedValue.COMPLEX_UNIT_SP, dateSize)
        views.setTextViewTextSize(R.id.largeWidgetCatalanTime, TypedValue.COMPLEX_UNIT_SP, timeSize)
    }
}
