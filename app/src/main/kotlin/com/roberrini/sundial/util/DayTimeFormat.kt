package com.roberrini.sundial.util

import android.content.Context
import android.text.format.DateFormat
import java.util.Calendar

/** Formats a fraction of the day (0f = midnight) as a clock time in the device's format. */
object DayTimeFormat {

    private const val EM_DASH = "—"

    fun of(context: Context, fraction: Float?): String {
        if (fraction == null) return EM_DASH
        val minutes = (fraction * 1440).toInt().coerceIn(0, 1439)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
        }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }
}
