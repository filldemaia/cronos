package com.filldemaia.cronos

import android.content.Context
import android.view.View
import android.widget.RemoteViews

/**
 * Senyera completa amb un vel radial fosc al centre per llegir l'hora.
 * El nom de classe es conserva per no trencar els widgets ja afegits.
 */
class CronosWidgetApple : CronosBaseWidget() {

    override val layoutRes: Int = R.layout.widget_layout_senyera
    override val rootViewId: Int = R.id.appleWidgetRoot
    override val widgetType = WidgetType.SENYERA

    override fun fillViews(context: Context, views: RemoteViews, widthDp: Float, heightDp: Float) {
        val timeInCatalan = CatalanTimeFormatter.getCurrentTimeInCatalan()
        views.setViewVisibility(R.id.senyeraPreviewTime, View.GONE)
        views.setImageViewBitmap(
            R.id.appleWidgetTime,
            WidgetTextBitmap.render(
                context, timeInCatalan, SettingsRepository(context).widgetTextSize(widgetType),
                widthDp - 32f, heightDp - 32f
            )
        )
        views.setContentDescription(R.id.appleWidgetTime, timeInCatalan)
    }
}
