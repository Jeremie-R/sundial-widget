package com.roberrini.sundial.time

import java.time.LocalTime
import java.time.ZoneId

/** Maps wall-clock time to the 0f..1f `progress` consumed by the renderer. */
object DayProgress {

    private const val SECONDS_PER_DAY = 24 * 60 * 60

    /** Fraction of the 24h day elapsed at [time]: 00:00 -> 0f, 12:00 -> 0.5f, 23:59:59 -> ~1f. */
    fun of(time: LocalTime): Float = time.toSecondOfDay() / SECONDS_PER_DAY.toFloat()

    fun now(zone: ZoneId = ZoneId.systemDefault()): Float = of(LocalTime.now(zone))
}
