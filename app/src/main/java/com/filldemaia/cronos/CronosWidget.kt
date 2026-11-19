package com.filldemaia.cronos

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.widget.RemoteViews

/**
 * Widget petit (4x1, transparent): mostra l'hora catalana en text blanc
 * amb ombra, perquè es llegeixi sobre qualsevol fons de pantalla sense
 * tapar-lo amb una targeta.
 */
class CronosWidget : CronosBaseWidget() {

    override val layoutRes: Int = R.layout.widget_layout_time
    override val rootViewId: Int = R.id.smallWidgetRoot
    override val widgetType = WidgetType.TIME

    override fun fillViews(context: Context, views: RemoteViews, widthDp: Float, heightDp: Float) {
        val timeInCatalan = CatalanTimeFormatter.getCurrentTimeInCatalan()
        views.setTextViewText(R.id.smallWidgetTime, timeInCatalan)
        views.setTextColor(R.id.smallWidgetTime, Color.WHITE)
        views.setTextViewTextSize(
            R.id.smallWidgetTime, TypedValue.COMPLEX_UNIT_SP,
            WidgetTextFitter.time(context, timeInCatalan, SettingsRepository(context).widgetTextSize(widgetType),
                widthDp - 12f, heightDp - 12f)
        )
    }
}
