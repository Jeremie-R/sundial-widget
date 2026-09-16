package com.roberrini.sundial.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Schedules the periodic re-render that moves the sun. Uses an inexact, non-wakeup
 * `RTC` repeating alarm: it never wakes the device on its own and is delivered the next
 * time the device is awake, which is the least battery-invasive option that still keeps
 * the dot moving whenever the screen is on. No exact-alarm permission is needed.
 *
 * The digital time is a TextClock and updates every minute regardless of this alarm.
 */
object SundialUpdateScheduler {

    /**
     * How often the sun position is re-rendered. The only knob to tighten for finer
     * motion (AlarmManager enforces a floor of 1 minute for repeating alarms).
     */
    val UPDATE_INTERVAL_MS: Long = TimeUnit.MINUTES.toMillis(15)

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // Align the first tick to the next interval boundary (e.g. :00, :15, :30, :45).
        val now = System.currentTimeMillis()
        val firstTick = (now / UPDATE_INTERVAL_MS + 1) * UPDATE_INTERVAL_MS
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            firstTick,
            UPDATE_INTERVAL_MS,
            tickPendingIntent(context),
        )
        Log.d(TAG, "Scheduled ticks every ${UPDATE_INTERVAL_MS / 1000}s, first at ${Date(firstTick)}")
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(tickPendingIntent(context))
    }

    private fun tickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SundialWidgetProvider::class.java)
            .setAction(SundialWidgetProvider.ACTION_TICK)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val TAG = "SundialWidget"
    private const val REQUEST_CODE = 0
}
