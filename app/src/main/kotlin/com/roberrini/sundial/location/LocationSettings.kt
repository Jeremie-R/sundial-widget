package com.roberrini.sundial.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit

data class LatLon(val lat: Double, val lon: Double)

/** Where the app should take its coordinates from. */
enum class LocationSource { DEVICE, CITY }

/**
 * Persists the location choice and the last device fix. The widget runs in the
 * background, where Android hands out no location without the "all the time"
 * permission, so the fix is refreshed whenever the settings screen is open and
 * reused from here in between.
 */
object LocationSettings {

    private const val PREFS = "sundial"
    private const val KEY_SOURCE = "location_source"
    private const val KEY_CITY = "location_city"
    private const val KEY_LAT = "device_lat"
    private const val KEY_LON = "device_lon"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Null until the user has made (or been asked for) a choice. */
    fun source(context: Context): LocationSource? =
        prefs(context).getString(KEY_SOURCE, null)?.let { runCatching { LocationSource.valueOf(it) }.getOrNull() }

    fun setSource(context: Context, source: LocationSource) =
        prefs(context).edit { putString(KEY_SOURCE, source.name) }

    fun city(context: Context): City? = Cities.byId(prefs(context).getString(KEY_CITY, null))

    fun setCity(context: Context, city: City) = prefs(context).edit {
        putString(KEY_CITY, city.id)
        putString(KEY_SOURCE, LocationSource.CITY.name)
    }

    fun storedDeviceLocation(context: Context): LatLon? {
        val p = prefs(context)
        if (!p.contains(KEY_LAT)) return null
        return LatLon(
            Double.fromBits(p.getLong(KEY_LAT, 0)),
            Double.fromBits(p.getLong(KEY_LON, 0)),
        )
    }

    fun storeDeviceLocation(context: Context, location: LatLon) = prefs(context).edit {
        putLong(KEY_LAT, location.lat.toBits())
        putLong(KEY_LON, location.lon.toBits())
    }

    /**
     * Best available coordinates for the current source, or null. For [LocationSource.DEVICE]
     * a fresh last-known fix is preferred (it's free and may succeed while the app is
     * visible), then the stored one.
     */
    fun resolve(context: Context): LatLon? = when (source(context)) {
        LocationSource.CITY -> city(context)?.let { LatLon(it.lat, it.lon) }
        LocationSource.DEVICE -> lastKnownDeviceLocation(context)?.also { storeDeviceLocation(context, it) }
            ?: storedDeviceLocation(context)
        null -> null
    }

    /** The provider to ask for a coarse fix on this device, if any. */
    fun coarseProvider(context: Context): String? {
        val manager = context.getSystemService(LocationManager::class.java)
        val providers = manager.allProviders
        return listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull { it in providers }
    }

    private fun lastKnownDeviceLocation(context: Context): LatLon? {
        if (!hasPermission(context)) return null
        val provider = coarseProvider(context) ?: return null
        val manager = context.getSystemService(LocationManager::class.java)
        return try {
            manager.getLastKnownLocation(provider)?.let { LatLon(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
