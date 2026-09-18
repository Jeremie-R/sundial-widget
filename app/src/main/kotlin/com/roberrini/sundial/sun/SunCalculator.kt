package com.roberrini.sundial.sun

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * NOAA solar position algorithm (the one behind NOAA's sunrise/sunset calculator).
 * Accurate to about a minute for the next few decades, no network, no dependency.
 */
object SunCalculator {

    /** Zenith angle for sunrise/sunset: 90° plus refraction and the solar disc's radius. */
    private const val ZENITH_SUNRISE = 90.833

    /** Twilights: sun centre 6°, 12° and 18° below the horizon. */
    private const val ZENITH_CIVIL = 96.0
    private const val ZENITH_NAUTICAL = 102.0
    private const val ZENITH_ASTRONOMICAL = 108.0

    fun compute(latitude: Double, longitude: Double, date: LocalDate, zone: ZoneId): SunTimes {
        val offsetMinutes = zone.rules.getOffset(date.atStartOfDay(zone).toInstant()).totalSeconds / 60
        return SunTimes(
            daylight = span(latitude, longitude, date, ZENITH_SUNRISE, offsetMinutes),
            civil = span(latitude, longitude, date, ZENITH_CIVIL, offsetMinutes),
            nautical = span(latitude, longitude, date, ZENITH_NAUTICAL, offsetMinutes),
            astronomical = span(latitude, longitude, date, ZENITH_ASTRONOMICAL, offsetMinutes),
        )
    }

    private fun span(lat: Double, lon: Double, date: LocalDate, zenith: Double, offsetMinutes: Int): DaySpan {
        val jc = (julianDay(date) + 0.5 - 2451545.0) / 36525.0
        val meanLong = (280.46646 + jc * (36000.76983 + jc * 0.0003032)).mod(360.0)
        val meanAnom = 357.52911 + jc * (35999.05029 - 0.0001537 * jc)
        val eccent = 0.016708634 - jc * (0.000042037 + 0.0000001267 * jc)
        val eqOfCentre = sin(rad(meanAnom)) * (1.914602 - jc * (0.004817 + 0.000014 * jc)) +
            sin(rad(2 * meanAnom)) * (0.019993 - 0.000101 * jc) +
            sin(rad(3 * meanAnom)) * 0.000289
        val trueLong = meanLong + eqOfCentre
        val appLong = trueLong - 0.00569 - 0.00478 * sin(rad(125.04 - 1934.136 * jc))
        val meanObliq = 23.0 + (26.0 + (21.448 - jc * (46.815 + jc * (0.00059 - jc * 0.001813))) / 60.0) / 60.0
        val obliq = meanObliq + 0.00256 * cos(rad(125.04 - 1934.136 * jc))
        val declination = deg(asin(sin(rad(obliq)) * sin(rad(appLong))))
        val y = tan(rad(obliq / 2)).let { it * it }
        val eqOfTime = 4 * deg(
            y * sin(2 * rad(meanLong)) -
                2 * eccent * sin(rad(meanAnom)) +
                4 * eccent * y * sin(rad(meanAnom)) * cos(2 * rad(meanLong)) -
                0.5 * y * y * sin(4 * rad(meanLong)) -
                1.25 * eccent * eccent * sin(2 * rad(meanAnom)),
        )
        val cosHourAngle = cos(rad(zenith)) / (cos(rad(lat)) * cos(rad(declination))) -
            tan(rad(lat)) * tan(rad(declination))
        if (cosHourAngle > 1) return DaySpan.None   // sun never gets this high today
        if (cosHourAngle < -1) return DaySpan.All   // sun never gets this low today

        val hourAngle = deg(acos(cosHourAngle))
        val solarNoonUtc = 720.0 - 4 * lon - eqOfTime // minutes
        val rise = solarNoonUtc - 4 * hourAngle + offsetMinutes
        val set = solarNoonUtc + 4 * hourAngle + offsetMinutes
        return DaySpan.Between(fraction(rise), fraction(set))
    }

    /** Julian day number at 0h UTC. */
    private fun julianDay(date: LocalDate): Double {
        var y = date.year
        var m = date.monthValue
        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        return (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + date.dayOfMonth + b - 1524.5
    }

    private fun fraction(localMinutes: Double): Float = (localMinutes.mod(1440.0) / 1440.0).toFloat()
    private fun rad(deg: Double) = Math.toRadians(deg)
    private fun deg(rad: Double) = Math.toDegrees(rad)
}
