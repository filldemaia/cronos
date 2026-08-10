package com.example.cronos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Rep el tick per minut del [WidgetUpdateScheduler], actualitza tots
 * els widgets actius i en programa el següent tick.
 */
class CronosWidgetTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetUpdateScheduler.ACTION_TICK) return
        Log.d(TAG, "Tick: actualitzant widgets")
        WidgetUpdateScheduler.updateAll(context)
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }

    companion object {
        private const val TAG = "CronosWidgetTickReceiver"
    }
}