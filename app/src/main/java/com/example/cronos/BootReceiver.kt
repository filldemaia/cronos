package com.example.cronos

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver per reiniciar el widget després d'un reinici del dispositiu
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver: ${intent.action}")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d(TAG, "Dispositiu reiniciat - reprogramant widgets")

            val appWidgetManager = AppWidgetManager.getInstance(context)

            // Reprogramar els tres tipus de widget: petit, estil Apple i gran
            reprogramarWidget(context, appWidgetManager, CronosWidget::class.java)
            reprogramarWidget(context, appWidgetManager, CronosWidgetApple::class.java)
            reprogramarWidget(context, appWidgetManager, CronosWidgetLarge::class.java)
        }
    }

    private fun reprogramarWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetClass: Class<*>
    ) {
        val component = ComponentName(context, widgetClass)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(component)

        if (appWidgetIds.isNotEmpty()) {
            Log.d(TAG, "Trobats ${appWidgetIds.size} widgets actius de ${widgetClass.simpleName}")
            val updateIntent = Intent(context, widgetClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(updateIntent)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
