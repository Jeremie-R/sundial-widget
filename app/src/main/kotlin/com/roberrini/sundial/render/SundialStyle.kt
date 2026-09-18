package com.roberrini.sundial.render

import androidx.annotation.StringRes
import com.roberrini.sundial.R

/** The five treatments from the design lab. */
enum class Look(@param:StringRes val label: Int) {
    PILLOW(R.string.look_pillow),
    BOLD(R.string.look_bold),
    LINE(R.string.look_line),
    HIGH_CONTRAST(R.string.look_high_contrast),
    BEADS(R.string.look_beads),
}

/** Background silhouette behind the dial. */
enum class Shape(@param:StringRes val label: Int) {
    NONE(R.string.shape_none),
    CIRCLE(R.string.shape_circle),
    SQUIRCLE(R.string.shape_squircle),
    COOKIE_12(R.string.shape_cookie12),
    COOKIE_9(R.string.shape_cookie9),
    COOKIE_7(R.string.shape_cookie7),
    COOKIE_4(R.string.shape_cookie4),
    CLOVER_8(R.string.shape_clover8),
    SUNNY(R.string.shape_sunny),
    BURST(R.string.shape_burst),
}

/** How the sun is drawn while it is below the twilight line. */
enum class NightSun(@param:StringRes val label: Int) {
    OUTLINE(R.string.night_outline),
    MOON(R.string.night_moon),
    HIDDEN(R.string.night_hidden),
}

/** How the thin bar from the daylight track out to astronomical dawn/dusk is drawn. */
enum class TwilightBar(@param:StringRes val label: Int) {
    /** One uniform line all the way. */
    PLAIN(R.string.twilight_plain),
    /** Three bands (civil, nautical, astronomical) fading outward. */
    FADING(R.string.twilight_fading),
}

/** Everything about the dial's appearance that isn't a colour. */
data class SundialStyle(
    val look: Look = Look.PILLOW,
    val shape: Shape = Shape.COOKIE_12,
    val twilightBar: TwilightBar = TwilightBar.PLAIN,
    val nightSun: NightSun = NightSun.OUTLINE,
    val sparkles: Boolean = false,
)
