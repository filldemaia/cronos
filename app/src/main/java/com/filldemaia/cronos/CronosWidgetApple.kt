package com.filldemaia.cronos

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import com.filldemaia.cronos.ui.theme.paletteForHour
import java.util.Calendar

/**
 * Widget "estil Apple": una targeta arrodonida que canvia de color
 * segons el moment del dia, igual que la pantalla principal de l'app.
 */
class CronosWidgetApple : CronosBaseWidget() {

    override val layoutRes: Int = R.layout.widget_layout_apple
    override val rootViewId: Int = R.id.appleWidgetRoot

    override fun fillViews(context: Context, views: RemoteViews) {
        val timeInCatalan = CatalanTimeFormatter.getCurrentTimeInCatalan()
        val palette = paletteForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

        views.setTextViewText(R.id.appleWidgetTime, timeInCatalan)
        views.setTextColor(R.id.appleWidgetTime, Color.WHITE)
        views.setInt(R.id.appleWidgetRoot, "setBackgroundResource", palette.widgetBackgroundRes)
    }
}
