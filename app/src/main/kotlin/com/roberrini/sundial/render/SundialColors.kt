package com.roberrini.sundial.render

import android.content.Context
import android.content.res.Resources
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.google.android.material.color.utilities.Hct

/**
 * Material colour roles the looks draw with. Plain ints so the renderer has no dependency
 * on a Context and can be exercised from tests with any palette.
 */
data class SundialColors(
    @param:ColorInt val surface: Int,
    @param:ColorInt val surfaceContainer: Int,
    @param:ColorInt val onSurface: Int,
    @param:ColorInt val onSurfaceVariant: Int,
    @param:ColorInt val outline: Int,
    @param:ColorInt val outlineVariant: Int,
    @param:ColorInt val primary: Int,
    @param:ColorInt val primaryContainer: Int,
    @param:ColorInt val secondaryContainer: Int,
    @param:ColorInt val onSecondaryContainer: Int,
    @param:ColorInt val tertiary: Int,
    /** Pure ink and ground for the high-contrast look. */
    @param:ColorInt val ink: Int,
    @param:ColorInt val ground: Int,
    @param:ColorInt val catchlight: Int,
) {
    companion object {

        /**
         * The device's Material You palette (Android 12+): `system_accent1/2/3_*` and
         * `system_neutral1/2_*` are the same tonal palettes Compose's
         * `dynamicLightColorScheme()` is built from. Falls back to a teal [fromHue] palette
         * if they can't be resolved.
         */
        fun fromSystem(context: Context, dark: Boolean): SundialColors = try {
            fun c(@ColorRes id: Int) = context.getColor(id)
            if (dark) SundialColors(
                surface = c(android.R.color.system_neutral1_900),
                surfaceContainer = c(android.R.color.system_neutral1_800),
                onSurface = c(android.R.color.system_neutral1_100),
                onSurfaceVariant = c(android.R.color.system_neutral2_200),
                outline = c(android.R.color.system_neutral2_400),
                outlineVariant = c(android.R.color.system_neutral2_700),
                primary = c(android.R.color.system_accent1_200),
                primaryContainer = c(android.R.color.system_accent1_700),
                secondaryContainer = c(android.R.color.system_accent2_700),
                onSecondaryContainer = c(android.R.color.system_accent2_100),
                tertiary = c(android.R.color.system_accent3_200),
                ink = c(android.R.color.system_neutral1_0),
                ground = c(android.R.color.system_neutral1_1000),
                catchlight = CATCHLIGHT,
            ) else SundialColors(
                surface = c(android.R.color.system_neutral1_10),
                surfaceContainer = c(android.R.color.system_neutral1_50),
                onSurface = c(android.R.color.system_neutral1_900),
                onSurfaceVariant = c(android.R.color.system_neutral2_700),
                outline = c(android.R.color.system_neutral2_500),
                outlineVariant = c(android.R.color.system_neutral2_200),
                primary = c(android.R.color.system_accent1_600),
                primaryContainer = c(android.R.color.system_accent1_100),
                secondaryContainer = c(android.R.color.system_accent2_100),
                onSecondaryContainer = c(android.R.color.system_accent2_900),
                tertiary = c(android.R.color.system_accent3_600),
                ink = c(android.R.color.system_neutral1_1000),
                ground = c(android.R.color.system_neutral1_0),
                catchlight = CATCHLIGHT,
            )
        } catch (e: Resources.NotFoundException) {
            fromHue(DEFAULT_HUE, dark)
        }

        /**
         * A static palette built the way Material You builds one from a wallpaper: HCT tonal
         * palettes with the "tonal spot" chromas (primary 36, secondary 16, tertiary 24 at
         * hue + 60, neutrals 6 / 8).
         */
        fun fromHue(hue: Double, dark: Boolean): SundialColors {
            fun tone(h: Double, chroma: Double, t: Double): Int = Hct.from(h, chroma, t).toInt()
            val p = { t: Double -> tone(hue, 36.0, t) }
            val s = { t: Double -> tone(hue, 16.0, t) }
            val tt = { t: Double -> tone((hue + 60.0) % 360.0, 24.0, t) }
            val n = { t: Double -> tone(hue, 6.0, t) }
            val nv = { t: Double -> tone(hue, 8.0, t) }
            return if (dark) SundialColors(
                surface = n(6.0), surfaceContainer = n(12.0),
                onSurface = n(90.0), onSurfaceVariant = nv(80.0),
                outline = nv(60.0), outlineVariant = nv(30.0),
                primary = p(80.0), primaryContainer = p(30.0),
                secondaryContainer = s(30.0), onSecondaryContainer = s(90.0),
                tertiary = tt(80.0),
                ink = n(98.0), ground = n(2.0), catchlight = CATCHLIGHT,
            ) else SundialColors(
                surface = n(98.0), surfaceContainer = n(94.0),
                onSurface = n(10.0), onSurfaceVariant = nv(30.0),
                outline = nv(50.0), outlineVariant = nv(80.0),
                primary = p(40.0), primaryContainer = p(90.0),
                secondaryContainer = s(90.0), onSecondaryContainer = s(10.0),
                tertiary = tt(40.0),
                ink = n(4.0), ground = n(100.0), catchlight = CATCHLIGHT,
            )
        }

        const val DEFAULT_HUE = 195.0
        private const val CATCHLIGHT = 0xE6FFFFFF.toInt()
    }
}
