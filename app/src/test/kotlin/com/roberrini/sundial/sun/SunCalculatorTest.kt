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
        val twilight = times.twilight as DaySpan.Between
        assertClock("05:04", twilight.start)
        assertClock("22:40", twilight.end)
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
        // Civil twilight still happens around midday in the polar night.
        val twilight = winter.twilight as DaySpan.Between
        assertClock("09:31", twilight.start)
        assertClock("13:53", twilight.end)
    }

    @Test
    fun twilightContainsDaylight() {
        val times = SunCalculator.compute(48.86, 2.35, LocalDate.of(2026, 9, 16), paris)
        val day = times.daylight as DaySpan.Between
        val twilight = times.twilight as DaySpan.Between
        assertTrue(twilight.start < day.start)
        assertTrue(twilight.end > day.end)
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
