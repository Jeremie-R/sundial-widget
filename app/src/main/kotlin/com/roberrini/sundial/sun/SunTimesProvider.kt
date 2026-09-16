package com.roberrini.sundial.sun

import android.content.Context
import com.roberrini.sundial.location.LocationSettings
import java.time.LocalDate
import java.time.ZoneId

/** Today's sun times for the configured location, or [SunTimes.DEFAULT] when there is none. */
object SunTimesProvider {

    fun today(context: Context, zone: ZoneId = ZoneId.systemDefault()): SunTimes {
        val location = LocationSettings.resolve(context) ?: return SunTimes.DEFAULT
        return SunCalculator.compute(location.lat, location.lon, LocalDate.now(zone), zone)
    }
}
