package com.roberrini.sundial.render

import androidx.annotation.StringRes
import com.roberrini.sundial.R

/** What sits inside the dial. */
enum class ClockFace(@param:StringRes val label: Int) {
    /** HH:MM on one line, with the date beneath. */
    DIGITAL(R.string.face_digital),

    /** The hour over the minutes, with the date beneath. */
    STACKED(R.string.face_stacked),

    /** The hour and minutes as two heavy rows that overlap and stagger into each other. */
    OVERLAP(R.string.face_overlap),

    /** Hands, ticked by the launcher itself. */
    ANALOG(R.string.face_analog),

    /** Tonight's moon above the time. */
    MOON(R.string.face_moon);

    /** Whether this face shows digits, and so takes the typeface options. */
    val isDigital: Boolean get() = this != ANALOG
}

/**
 * Type family for the digits. A RemoteViews client can't set a typeface at runtime
 * ([android.widget.TextView.setTypeface] isn't remotable), so each family is a
 * pre-placed view in the widget layout that the updater shows and the rest it hides.
 */
enum class ClockTypeface(@param:StringRes val label: Int) {
    MODERN(R.string.typeface_modern),
    CLASSIC(R.string.typeface_classic),
    WIDE(R.string.typeface_wide),
    CONDENSED(R.string.typeface_condensed),
}

/** How heavy the digits are. Two steps, chosen per family rather than a free slider. */
enum class ClockWeight(@param:StringRes val label: Int) {
    REGULAR(R.string.weight_regular),
    BOLD(R.string.weight_bold),
}

/** Stroke of the analog hands. */
enum class HandWeight(@param:StringRes val label: Int, val fraction: Float) {
    FINE(R.string.hands_fine, 0.016f),
    REGULAR(R.string.hands_regular, 0.026f),
    BOLD(R.string.hands_bold, 0.040f),
}

/** How much detail the moon disc carries. */
enum class MoonStyle(@param:StringRes val label: Int) {
    /** A clean two-tone disc. */
    SIMPLE(R.string.moon_simple),

    /** The same disc with maria and craters shaded in. */
    DETAILED(R.string.moon_detailed),
}
