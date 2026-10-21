package com.roberrini.sundial.moon

import androidx.annotation.StringRes
import com.roberrini.sundial.R

/** The eight phase names, as the app labels them. */
enum class MoonPhase(@param:StringRes val label: Int) {
    NEW(R.string.moon_new),
    WAXING_CRESCENT(R.string.moon_waxing_crescent),
    FIRST_QUARTER(R.string.moon_first_quarter),
    WAXING_GIBBOUS(R.string.moon_waxing_gibbous),
    FULL(R.string.moon_full),
    WANING_GIBBOUS(R.string.moon_waning_gibbous),
    LAST_QUARTER(R.string.moon_last_quarter),
    WANING_CRESCENT(R.string.moon_waning_crescent);

    companion object {
        /** Principal phases get a window of about 14 hours each side; the rest are in between. */
        private const val WINDOW = 0.02

        fun of(phase: Double): MoonPhase {
            val p = ((phase % 1.0) + 1.0) % 1.0
            return when {
                p < WINDOW || p > 1 - WINDOW -> NEW
                p < 0.25 - WINDOW -> WAXING_CRESCENT
                p < 0.25 + WINDOW -> FIRST_QUARTER
                p < 0.5 - WINDOW -> WAXING_GIBBOUS
                p < 0.5 + WINDOW -> FULL
                p < 0.75 - WINDOW -> WANING_GIBBOUS
                p < 0.75 + WINDOW -> LAST_QUARTER
                else -> WANING_CRESCENT
            }
        }
    }
}

/**
 * The moon on one day at one place. [rise] and [set] are fractions of the day
 * (0f = midnight, 0.5f = noon) and are null when the moon doesn't cross the horizon
 * that day — then one of [alwaysUp] / [alwaysDown] is true.
 */
data class MoonInfo(
    val rise: Float?,
    val set: Float?,
    val alwaysUp: Boolean,
    val alwaysDown: Boolean,
    /** Lit fraction of the disc, 0..1. */
    val illumination: Double,
    /** 0 = new, 0.25 = first quarter, 0.5 = full, 0.75 = last quarter. */
    val phase: Double,
) {
    val phaseName: MoonPhase get() = MoonPhase.of(phase)

    /** After full, the moon is lit on the other limb; the renderer mirrors the disc. */
    val waning: Boolean get() = phase > 0.5
}
