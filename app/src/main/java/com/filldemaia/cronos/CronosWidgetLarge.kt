package com.filldemaia.cronos

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews

/**
 * Widget gran vertical (4x2, transparent): mostra la data i l'hora
 * tradicional en text blanc amb ombra, sense fons ni hora digital.
 */
class CronosWidgetLarge : CronosBaseWidget() {

    override val layoutRes: Int = R.layout.widget_layout_large
    override val rootViewId: Int = R.id.largeWidgetRoot

    override fun fillViews(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.largeWidgetDate, CatalanDateFormatter.today())
        views.setTextViewText(R.id.largeWidgetCatalanTime, CatalanTimeFormatter.getCurrentTimeInCatalan())
        views.setTextColor(R.id.largeWidgetDate, Color.WHITE)
        views.setTextColor(R.id.largeWidgetCatalanTime, Color.WHITE)
    }
}
