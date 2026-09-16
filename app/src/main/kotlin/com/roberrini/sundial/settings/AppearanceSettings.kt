package com.roberrini.sundial.settings

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.roberrini.sundial.R
import com.roberrini.sundial.render.Look
import com.roberrini.sundial.render.NightSun
import com.roberrini.sundial.render.Shape
import com.roberrini.sundial.render.SundialColors
import com.roberrini.sundial.render.SundialStyle

/** Where the widget's colours come from: the device's own Material You palette, or a fixed hue. */
enum class HuePreset(@param:StringRes val label: Int, val degrees: Double?) {
    DEVICE(R.string.hue_device, null),
    TEAL(R.string.hue_teal, 195.0),
    SKY(R.string.hue_sky, 240.0),
    LAVENDER(R.string.hue_lavender, 300.0),
    ROSE(R.string.hue_rose, 350.0),
    PEACH(R.string.hue_peach, 45.0),
    LIME(R.string.hue_lime, 125.0),
}

enum class ThemeMode(@param:StringRes val label: Int) {
    AUTO(R.string.theme_auto),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

/** Persisted appearance choices, and the palette/style they resolve to. */
object AppearanceSettings {

    private const val PREFS = "sundial"
    private const val KEY_LOOK = "look"
    private const val KEY_SHAPE = "shape"
    private const val KEY_NIGHT_SUN = "night_sun"
    private const val KEY_SPARKLES = "sparkles"
    private const val KEY_HUE = "hue"
    private const val KEY_THEME = "theme"

    fun style(context: Context): SundialStyle {
        val p = prefs(context)
        val defaults = SundialStyle()
        return SundialStyle(
            look = p.enum(KEY_LOOK, defaults.look),
            shape = p.enum(KEY_SHAPE, defaults.shape),
            nightSun = p.enum(KEY_NIGHT_SUN, defaults.nightSun),
            sparkles = p.getBoolean(KEY_SPARKLES, defaults.sparkles),
        )
    }

    fun setLook(context: Context, look: Look) = prefs(context).edit { putString(KEY_LOOK, look.name) }
    fun setShape(context: Context, shape: Shape) = prefs(context).edit { putString(KEY_SHAPE, shape.name) }
    fun setNightSun(context: Context, nightSun: NightSun) = prefs(context).edit { putString(KEY_NIGHT_SUN, nightSun.name) }
    fun setSparkles(context: Context, on: Boolean) = prefs(context).edit { putBoolean(KEY_SPARKLES, on) }

    fun huePreset(context: Context): HuePreset = prefs(context).enum(KEY_HUE, HuePreset.DEVICE)
    fun setHuePreset(context: Context, preset: HuePreset) = prefs(context).edit { putString(KEY_HUE, preset.name) }

    fun themeMode(context: Context): ThemeMode = prefs(context).enum(KEY_THEME, ThemeMode.AUTO)
    fun setThemeMode(context: Context, mode: ThemeMode) = prefs(context).edit { putString(KEY_THEME, mode.name) }

    fun isDark(context: Context): Boolean = when (themeMode(context)) {
        ThemeMode.AUTO -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun colors(context: Context, dark: Boolean = isDark(context)): SundialColors {
        val hue = huePreset(context).degrees
        return if (hue == null) SundialColors.fromSystem(context, dark) else SundialColors.fromHue(hue, dark)
    }

    private inline fun <reified E : Enum<E>> android.content.SharedPreferences.enum(key: String, default: E): E =
        getString(key, null)?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
