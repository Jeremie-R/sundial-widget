package com.roberrini.sundial.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class DayProgressTest {

    @Test
    fun midnightIsZero() {
        assertEquals(0f, DayProgress.of(LocalTime.MIDNIGHT), 0f)
    }

    @Test
    fun noonIsHalf() {
        assertEquals(0.5f, DayProgress.of(LocalTime.NOON), 0f)
    }

    @Test
    fun sunriseAndSunsetLandOnQuarters() {
        assertEquals(0.25f, DayProgress.of(LocalTime.of(6, 0)), 0f)
        assertEquals(0.75f, DayProgress.of(LocalTime.of(18, 0)), 0f)
    }

    @Test
    fun lastSecondOfDayApproachesOne() {
        val progress = DayProgress.of(LocalTime.of(23, 59, 59))
        assertEquals(1f, progress, 1f / 86_400f)
    }
}
