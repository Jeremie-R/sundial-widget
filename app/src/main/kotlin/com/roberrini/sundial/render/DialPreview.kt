package com.roberrini.sundial.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.format.DateFormat
import com.roberrini.sundial.moon.MoonProvider
import com.roberrini.sundial.settings.AppearanceSettings
import com.roberrini.sundial.sun.SunTimesProvider
import com.roberrini.sundial.time.DayProgress
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The dial as the widget would draw it right now, with the time and date painted in.
 * Shared by the landing page's hero and the settings previews and thumbnails.
 */
object DialPreview {

    fun render(
        context: Context,
        sizePx: Int,
        style: SundialStyle = AppearanceSettings.style(context),
        colors: SundialColors = AppearanceSettings.colors(context),
    ): Bitmap = SundialRenderer.render(
        width = sizePx,
        height = sizePx,
        progress = DayProgress.now(),
        sunTimes = SunTimesProvider.today(context),
        colors = colors,
        style = style,
        text = overlay(context, style),
        moon = MoonProvider.today(context),
    )

    /** The font the widget's layout would use, so a preview shows the real thing. */
    private fun typefaceFor(context: Context, style: SundialStyle): Typeface? {
        val bold = style.weight == ClockWeight.BOLD
        return when (style.typeface) {
            ClockTypeface.CLASSIC -> Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            ClockTypeface.MODERN, ClockTypeface.CONDENSED, ClockTypeface.WIDE ->
                Typeface.create(if (bold) "sans-serif-black" else "sans-serif", Typeface.NORMAL)
        }
    }

    private fun overlay(context: Context, style: SundialStyle): SundialRenderer.TextOverlay {
        // Matches the widget's TextClocks, which follow the device's 12/24-hour setting.
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        val today = LocalDate.now()
        val now = LocalTime.now()
        return SundialRenderer.TextOverlay(
            time = now.format(DateTimeFormatter.ofPattern(pattern)),
            date = "${today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${today.dayOfMonth}",
            hour = now.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH" else "hh")),
            minute = now.format(DateTimeFormatter.ofPattern("mm")),
            typeface = typefaceFor(context, style),
        )
    }
}
