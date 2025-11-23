package com.example.horacat

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
            
            // Obtenir tots els widgets actius
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, HoracatWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            
            if (appWidgetIds.isNotEmpty()) {
                Log.d(TAG, "Trobats ${appWidgetIds.size} widgets actius")
                
                // Enviar intent per actualitzar els widgets
                val updateIntent = Intent(context, HoracatWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(updateIntent)
            } else {
                Log.d(TAG, "No hi ha widgets actius")
            }
        }
    }
    
    companion object {
        private const val TAG = "BootReceiver"
    }
}
