package com.roberrini.sundial.sun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SunCalculatorTest {

    private val paris = ZoneId.of("Europe/Paris")

    @Test
    fun parisMidsummer() {
        val times = SunCalculator.compute(48.86, 2.35, LocalDate.of(2026, 6, 21), paris)
        val day = times.daylight as DaySpan.Between
        assertClock("05:47", day.start)
        assertClock("21:57", day.end)
        val civil = times.civil as DaySpan.Between
        assertClock("05:04", civil.start)
        assertClock("22:40", civil.end)
        val nautical = times.nautical as DaySpan.Between
        assertClock("04:03", nautical.start)
        assertClock("23:41", nautical.end)
        // The sun never gets 18° below the horizon: no astronomical night, so the bar is a full ring.
        assertEquals(DaySpan.All, times.astronomical)
    }

    @Test
    fun parisAutumnTwilights() {
        val times = SunCalculator.compute(48.86, 2.35, LocalDate.of(2026, 9, 16), paris)
        assertClock("07:28", (times.daylight as DaySpan.Between).start)
        assertClock("06:57", (times.civil as DaySpan.Between).start)
        assertClock("06:19", (times.nautical as DaySpan.Between).start)
        assertClock("05:39", (times.astronomical as DaySpan.Between).start)
        assertClock("21:50", (times.astronomical as DaySpan.Between).end)
    }

    @Test
    fun timesMoveFromOneDayToTheNext() {
        // The widget recomputes for LocalDate.now() at every tick; consecutive days must differ.
        val today = SunCalculator.compute(48.86, 2.35, LocalDate.of(2026, 9, 16), paris).daylight as DaySpan.Between
        val tomorrow = SunCalculator.compute(48.86, 2.35, LocalDate.of(2026, 9, 17), paris).daylight as DaySpan.Between
        assertClock("07:30", tomorrow.start)
        assertClock("19:59", tomorrow.end)
        val driftMinutes = ((today.end - today.start) - (tomorrow.end - tomorrow.start)) * 1440
        assertTrue("September days should shorten by a few minutes, got $driftMinutes", driftMinutes in 2f..5f)
    }

    @Test
    fun parisMidwinter() {
        val day = SunCalculator.compute(48.86, 2.35, LocalDate.of(2026, 12, 21), paris).daylight as DaySpan.Between
        assertClock("08:41", day.start)
        assertClock("16:55", day.end)
    }

    @Test
    fun southernHemisphereSummer() {
        val sydney = ZoneId.of("Australia/Sydney")
        val day = SunCalculator.compute(-33.87, 151.21, LocalDate.of(2026, 1, 15), sydney).daylight as DaySpan.Between
        assertClock("06:00", day.start)
        assertClock("20:08", day.end)
    }

    @Test
    fun polarDayAndNight() {
        val tromso = ZoneId.of("Europe/Oslo")
        assertEquals(DaySpan.All, SunCalculator.compute(69.65, 18.96, LocalDate.of(2026, 6, 21), tromso).daylight)
        val winter = SunCalculator.compute(69.65, 18.96, LocalDate.of(2026, 12, 21), tromso)
        assertEquals(DaySpan.None, winter.daylight)
        // Twilight still happens around midday in the polar night.
        val civil = winter.civil as DaySpan.Between
        assertClock("09:31", civil.start)
        assertClock("13:53", civil.end)
        val astronomical = winter.astronomical as DaySpan.Between
        assertClock("06:28", astronomical.start)
        assertClock("16:56", astronomical.end)
    }

    @Test
    fun twilightsNestOutward() {
        val times = SunCalculator.compute(48.86, 2.35, LocalDate.of(2026, 9, 16), paris)
        val spans = listOf(times.daylight, times.civil, times.nautical, times.astronomical).map { it as DaySpan.Between }
        spans.zipWithNext { inner, outer ->
            assertTrue(outer.start < inner.start)
            assertTrue(outer.end > inner.end)
        }
    }

    /** Within two minutes of the expected wall-clock time. */
    private fun assertClock(expected: String, fraction: Float) {
        val (h, m) = expected.split(":").map { it.toInt() }
        val expectedMinutes = h * 60 + m
        val actualMinutes = fraction * 1440
        assertEquals("expected $expected, got ${fmt(actualMinutes)}", expectedMinutes.toFloat(), actualMinutes, 2f)
    }

    private fun fmt(minutes: Float) = "%02d:%02d".format(minutes.toInt() / 60, minutes.toInt() % 60)
}
