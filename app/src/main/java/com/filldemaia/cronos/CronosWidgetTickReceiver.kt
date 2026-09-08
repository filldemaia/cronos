package com.filldemaia.cronos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Rep el tick per minut del [WidgetUpdateScheduler] i actualitza tots
 * els widgets actius.
 *
 * Ordre crític: **primer** es programa el següent tick i **després** es
 * refresquen els widgets. Així, encara que l'actualització falli o el
 * sistema pari el procés a mig camí, la cadena d'alarmes per minut no
 * es trenca i el widget no queda endarrerit.
 */
class CronosWidgetTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // El tick per minut, i també canvis d'hora/zona horària del sistema:
        // en tots els casos cal refrescar els widgets i reprogramar.
        when (intent.action) {
            WidgetUpdateScheduler.ACTION_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }
        Log.d(TAG, "Tick (${intent.action}): actualitzant widgets")
        WidgetUpdateScheduler.scheduleNextUpdate(context)
        WidgetUpdateScheduler.updateAll(context)
    }

    companion object {
        private const val TAG = "CronosWidgetTickReceiver"
    }
}
