package com.roberrini.sundial.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle

class SundialWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        SundialWidgetUpdater.update(context, appWidgetManager, appWidgetIds)
        // Idempotent: also recovers the alarm if it was ever dropped (e.g. after a force-stop).
        SundialUpdateScheduler.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        // The user resized the widget: re-render bitmaps for the new sizes.
        SundialWidgetUpdater.update(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        SundialUpdateScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        SundialUpdateScheduler.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TICK) {
            onTick(context)
            return
        }
        super.onReceive(context, intent)
    }

    private fun onTick(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SundialWidgetProvider::class.java))
        if (ids.isEmpty()) {
            // Nothing left to draw (e.g. onDisabled was missed): stop ticking.
            SundialUpdateScheduler.cancel(context)
            return
        }
        SundialWidgetUpdater.update(context, manager, ids)
    }

    companion object {
        /** Sent by [SundialUpdateScheduler]'s alarm to move the sun. */
        const val ACTION_TICK = "com.roberrini.sundial.action.TICK"
    }
}
