package com.roberrini.sundial.moon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Expected values come from PyEphem, which implements a far more detailed model;
 * our rise/set agreed with it to within 15 seconds across these cases.
 */
class MoonCalculatorTest {

    private val paris = ZoneId.of("Europe/Paris")

    @Test
    fun parisAutumnEvening() {
        val moon = MoonCalculator.info(LocalDate.of(2026, 10, 21), paris, 48.86, 2.35)
        assertClock("16:54", moon.rise)
        assertClock("02:30", moon.set)
        assertEquals(0.756, moon.illumination, 0.005)
        assertFalse(moon.waning)
        assertEquals(MoonPhase.WAXING_GIBBOUS, moon.phaseName)
    }

    @Test
    fun parisSettingBeforeMidnight() {
        val moon = MoonCalculator.info(LocalDate.of(2026, 9, 16), paris, 48.86, 2.35)
        assertClock("13:53", moon.rise)
        assertClock("21:47", moon.set)
        assertEquals(0.275, moon.illumination, 0.005)
    }

    @Test
    fun southernHemisphere() {
        val moon = MoonCalculator.info(
            LocalDate.of(2026, 1, 15), ZoneId.of("Australia/Sydney"), -33.87, 151.21,
        )
        assertClock("02:21", moon.rise)
        assertClock("17:32", moon.set)
        assertEquals(0.132, moon.illumination, 0.005)
        assertTrue(moon.waning)
    }

    @Test
    fun nearTheEquatorWhereSetComesBeforeRise() {
        val moon = MoonCalculator.info(LocalDate.of(2026, 3, 5), ZoneId.of("Asia/Singapore"), 1.35, 103.82)
        assertClock("20:43", moon.rise)
        assertClock("08:20", moon.set)
        assertEquals(0.969, moon.illumination, 0.005)
    }

    @Test
    fun polarMoonThatNeverSets() {
        val moon = MoonCalculator.info(LocalDate.of(2026, 12, 21), ZoneId.of("Europe/Oslo"), 69.65, 18.96)
        assertNull(moon.rise)
        assertNull(moon.set)
        assertTrue(moon.alwaysUp)
        assertFalse(moon.alwaysDown)
    }

    @Test
    fun principalPhasesLandOnTheirNames() {
        // Instants of the October 2026 new, first quarter and full moons.
        assertPhase("2026-10-10T15:50:00Z", MoonPhase.NEW, lit = 0.0)
        assertPhase("2026-10-18T16:12:00Z", MoonPhase.FIRST_QUARTER, lit = 0.5)
        assertPhase("2026-10-26T04:11:00Z", MoonPhase.FULL, lit = 1.0)
        assertPhase("2026-10-03T13:24:00Z", MoonPhase.LAST_QUARTER, lit = 0.5)
    }

    @Test
    fun inBetweenPhasesAreNamedByQuadrant() {
        assertPhase("2026-10-14T12:00:00Z", MoonPhase.WAXING_CRESCENT, lit = 0.145)
        assertPhase("2026-10-21T12:00:00Z", MoonPhase.WAXING_GIBBOUS, lit = 0.763)
        assertPhase("2026-10-29T12:00:00Z", MoonPhase.WANING_GIBBOUS, lit = 0.851)
        assertPhase("2026-11-04T12:00:00Z", MoonPhase.WANING_CRESCENT, lit = 0.227)
    }

    @Test
    fun phaseNamesCoverTheWholeCycle() {
        // Every point of the cycle maps to a name, and the four principal ones do appear.
        val names = (0..999).map { MoonPhase.of(it / 1000.0) }
        assertEquals(MoonPhase.entries.toSet(), names.toSet())
        assertEquals(MoonPhase.NEW, MoonPhase.of(0.0))
        assertEquals(MoonPhase.NEW, MoonPhase.of(0.999))
        assertEquals(MoonPhase.FULL, MoonPhase.of(0.5))
    }

    private fun assertPhase(instant: String, expected: MoonPhase, lit: Double) {
        val illumination = MoonCalculator.illumination(Instant.parse(instant))
        assertEquals(instant, lit, illumination.fraction, 0.01)
        assertEquals(instant, expected, MoonPhase.of(illumination.phase))
    }

    /** Within two minutes of the expected wall-clock time. */
    private fun assertClock(expected: String, fraction: Float?) {
        requireNotNull(fraction) { "expected $expected, got no crossing" }
        val (h, m) = expected.split(":").map { it.toInt() }
        val actual = fraction * 1440
        assertEquals(
            "expected $expected, got %02d:%02d".format((actual / 60).toInt(), (actual % 60).toInt()),
            (h * 60 + m).toFloat(), actual, 2f,
        )
    }
}
