package com.roberrini.sundial.sun

/**
 * A span of the 24h dial, as fractions of the day (0f = midnight, 0.5f = noon).
 * [Between.end] may be smaller than [Between.start] when the span wraps midnight.
 */
sealed class DaySpan {
    data class Between(val start: Float, val end: Float) : DaySpan()
    data object All : DaySpan()
    data object None : DaySpan()

    fun contains(t: Float): Boolean = when (this) {
        All -> true
        None -> false
        is Between -> if (start <= end) t in start..end else t >= start || t <= end
    }
}

/**
 * Today's sun, as nested spans of the dial: [daylight] is the sun above the horizon
 * (sunrise to sunset), and the three twilights are the sun within 6°, 12° and 18° below
 * it. Each span contains the previous one; [astronomical] is the full extent of the
 * twilight bar, and what's outside it is night.
 */
data class SunTimes(
    val daylight: DaySpan,
    val civil: DaySpan,
    val nautical: DaySpan,
    val astronomical: DaySpan,
) {
    companion object {
        /** Used when no location is known: a 07:00–19:00 day with mid-latitude equinox twilights. */
        val DEFAULT = SunTimes(
            daylight = span(7 * 60, 19 * 60),
            civil = span(7 * 60 - 30, 19 * 60 + 30),
            nautical = span(7 * 60 - 65, 19 * 60 + 65),
            astronomical = span(7 * 60 - 100, 19 * 60 + 100),
        )

        private fun span(startMinutes: Int, endMinutes: Int) =
            DaySpan.Between(startMinutes / 1440f, endMinutes / 1440f)
    }
}
