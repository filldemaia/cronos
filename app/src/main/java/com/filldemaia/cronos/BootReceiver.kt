package com.filldemaia.cronos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Reprograma i refresca els widgets després d'un reinici del dispositiu
 * (les alarmes no sobreviuen al reinici).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver: ${intent.action}")
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        if (WidgetUpdateScheduler.anyWidgetsRemain(context)) {
            Log.d(TAG, "Dispositiu reiniciat - reprogramant widgets")
            WidgetUpdateScheduler.scheduleNextUpdate(context)
            WidgetUpdateScheduler.updateAll(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
