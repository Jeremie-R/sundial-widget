package com.roberrini.sundial.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Alarms don't survive a reboot, and a clock/timezone change should move the sun
 * immediately rather than at the next tick. Only does work if a widget is placed.
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                if (!hasWidgets(context)) return
                SundialWidgetUpdater.updateAll(context)
                SundialUpdateScheduler.schedule(context)
            }
        }
    }

    private fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, SundialWidgetProvider::class.java)
        return manager.getAppWidgetIds(provider).isNotEmpty()
    }
}
