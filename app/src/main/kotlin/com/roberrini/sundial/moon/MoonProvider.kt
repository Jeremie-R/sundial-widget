package com.roberrini.sundial.moon

import android.content.Context
import com.roberrini.sundial.location.LocationSettings
import java.time.LocalDate
import java.time.ZoneId

/** Today's moon for the configured location, or null when there is no location yet. */
object MoonProvider {

    fun today(context: Context, zone: ZoneId = ZoneId.systemDefault()): MoonInfo? {
        val location = LocationSettings.resolve(context) ?: return null
        return MoonCalculator.info(LocalDate.now(zone), zone, location.lat, location.lon)
    }
}
