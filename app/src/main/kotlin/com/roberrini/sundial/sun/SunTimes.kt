package com.roberrini.sundial.sun

/**
 * A span of the 24h dial, as fractions of the day (0f = midnight, 0.5f = noon).
 * [Between.end] may be smaller than [Between.start] when the span wraps midnight.
 */
sealed class DaySpan {
    data class Between(val start: Float, val end: Float) : DaySpan()
    data object All : DaySpan()
    data object None : DaySpan()
}

/**
 * Where the sun is above the horizon ([daylight]) and above civil twilight ([twilight],
 * -6° elevation) today. [twilight] always contains [daylight].
 */
data class SunTimes(val daylight: DaySpan, val twilight: DaySpan) {

    companion object {
        /** Used when no location is known: a 07:00–19:00 day with 40 minutes of twilight. */
        val DEFAULT = SunTimes(
            daylight = DaySpan.Between(7f / 24f, 19f / 24f),
            twilight = DaySpan.Between((7f * 60 - 40) / 1440f, (19f * 60 + 40) / 1440f),
        )
    }
}
