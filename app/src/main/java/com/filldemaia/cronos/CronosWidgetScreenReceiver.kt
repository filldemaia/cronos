package com.filldemaia.cronos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Refresca els widgets quan l'usuari es disposa a mirar-los: en encendre
 * la pantalla o desbloquejar el dispositiu, i amb cada tick de rellotge
 * del sistema mentre el dispositiu és despert.
 *
 * L'alarma per minut de [WidgetUpdateScheduler] és la garantia principal,
 * però molts fabricants la difereixen o la bloquegen quan la pantalla és
 * apagada. Aquests broadcasts de sistema sempre arriben en encendre la
 * pantalla, de manera que el widget ja mostra l'hora correcta l'instant
 * en què el mires: mai en veus el segon "salt". El tick de sistema
 * (TIME_TICK), que el sistema emet un cop per minut mentre la pantalla
 * està encesa, acompanya l'alarma i fa que l'hora es repinti en viu.
 */
class CronosWidgetScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_TIME_TICK -> Unit
            else -> return
        }
        if (!WidgetUpdateScheduler.anyWidgetsRemain(context)) return
        Log.d(TAG, "Pantalla encesa/tick de sistema (${intent.action}) - refrescant widgets")
        WidgetUpdateScheduler.scheduleNextUpdate(context)
        WidgetUpdateScheduler.updateAll(context)
    }

    companion object {
        private const val TAG = "CronosWidgetScreenReceiver"
    }
}