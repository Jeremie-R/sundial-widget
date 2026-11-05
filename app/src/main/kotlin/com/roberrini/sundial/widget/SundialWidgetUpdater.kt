package com.roberrini.sundial.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.graphics.drawable.Icon
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.os.BundleCompat
import com.roberrini.sundial.R
import com.roberrini.sundial.render.SundialColors
import com.roberrini.sundial.action.ActionSlot
import com.roberrini.sundial.action.LaunchAction
import com.roberrini.sundial.moon.MoonInfo
import com.roberrini.sundial.moon.MoonProvider
import com.roberrini.sundial.render.SundialRenderer
import com.roberrini.sundial.render.ClockFace
import com.roberrini.sundial.render.ClockTypeface
import com.roberrini.sundial.render.ClockWeight
import com.roberrini.sundial.render.SundialStyle
import com.roberrini.sundial.settings.AppearanceSettings
import com.roberrini.sundial.sun.SunTimes
import com.roberrini.sundial.sun.SunTimesProvider
import com.roberrini.sundial.time.DayProgress
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Turns "now" into RemoteViews for every widget instance: reads the sizes the launcher
 * reports for each instance, renders a bitmap per size and hands it to the ImageView in
 * the widget layout. Time and date are TextClock views in the layout, so they tick on
 * their own inside the launcher; the alarm only needs to move the sun.
 */
object SundialWidgetUpdater {

    /** Re-render every instance of the widget; safe to call from any receiver. */
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SundialWidgetProvider::class.java))
        update(context, manager, ids)
    }

    fun update(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val colors = AppearanceSettings.colors(context)
        val style = AppearanceSettings.style(context)
        val frame = Frame(
            progress = DayProgress.now(),
            sunTimes = SunTimesProvider.today(context),
            colors = colors,
            style = style,
            textStyle = SundialRenderer.textStyle(style.look, colors),
            tapIntent = tapPendingIntent(context),
            moon = if (style.face == ClockFace.MOON) MoonProvider.today(context) else null,
        )
        for (id in appWidgetIds) {
            manager.updateAppWidget(id, buildRemoteViews(context, manager, id, frame))
        }
        Log.d(TAG, "Rendered ${appWidgetIds.size} widget(s) at progress=${frame.progress} sun=${frame.sunTimes}")
    }

    /** Everything about "now" that is the same for every widget instance and size. */
    private class Frame(
        val progress: Float,
        val sunTimes: SunTimes,
        val colors: SundialColors,
        val style: SundialStyle,
        val textStyle: SundialRenderer.TextStyle,
        val tapIntent: PendingIntent?,
        val moon: MoonInfo?,
    )

    /** Activity to launch on tap, per the user's choice; null means the widget isn't clickable. */
    private fun tapPendingIntent(context: Context): PendingIntent? {
        val intent = LaunchAction.load(context, ActionSlot.WIDGET_TAP).toIntent(context) ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildRemoteViews(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        frame: Frame,
    ): RemoteViews {
        val options = manager.getAppWidgetOptions(appWidgetId)
        // Android 12+ launchers report every size the widget can be shown at
        // (e.g. portrait and landscape). Provide a tailored bitmap for each.
        val sizes = BundleCompat.getParcelableArrayList(
            options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java,
        ).orEmpty().take(MAX_SIZE_VARIANTS)

        if (sizes.isNotEmpty()) {
            return RemoteViews(sizes.associateWith { size -> viewsForSize(context, size, frame) })
        }

        // Fallback for launchers that only report min/max bounds.
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, DEFAULT_SIZE_DP)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, DEFAULT_SIZE_DP)
        return viewsForSize(context, SizeF(widthDp.toFloat(), heightDp.toFloat()), frame)
    }

    private fun viewsForSize(context: Context, size: SizeF, frame: Frame): RemoteViews {
        val density = context.resources.displayMetrics.density
        val style = frame.style
        // The bitmap covers the whole cell: the background shape is part of the drawing.
        val bitmap = SundialRenderer.render(
            dpToPx(size.width, density), dpToPx(size.height, density),
            frame.progress, frame.sunTimes, frame.colors, style, moon = frame.moon,
        )
        val dialDp = min(size.width, size.height)

        return RemoteViews(context.packageName, layoutFor(style.face)).apply {
            setImageViewBitmap(R.id.widget_dial, bitmap)
            when (style.face) {
                ClockFace.ANALOG -> applyAnalog(frame, dialDp, density)
                ClockFace.STACKED -> applyStacked(frame, dialDp)
                ClockFace.DIGITAL, ClockFace.MOON -> applyDigital(frame, dialDp, density)
            }
            if (frame.tapIntent != null) setOnClickPendingIntent(R.id.widget_root, frame.tapIntent)
        }
    }

    private fun layoutFor(face: ClockFace): Int = when (face) {
        ClockFace.STACKED -> R.layout.widget_sundial_stacked
        ClockFace.ANALOG -> R.layout.widget_sundial_analog
        ClockFace.DIGITAL, ClockFace.MOON -> R.layout.widget_sundial
    }

    private fun RemoteViews.applyAnalog(frame: Frame, dialDp: Float, density: Float) {
        val handPx = dpToPx(dialDp * ANALOG_SIZE, density)
        val icons = SundialRenderer.analogHands(handPx, frame.colors, frame.style)
        setIcon(R.id.widget_analog, "setDial", Icon.createWithBitmap(icons.dial))
        setIcon(R.id.widget_analog, "setHourHand", Icon.createWithBitmap(icons.hour))
        setIcon(R.id.widget_analog, "setMinuteHand", Icon.createWithBitmap(icons.minute))
    }

    private fun RemoteViews.applyDigital(frame: Frame, dialDp: Float, density: Float) {
        val isMoon = frame.style.face == ClockFace.MOON
        val fraction = if (isMoon) SundialRenderer.TIME_TEXT * 0.85f else SundialRenderer.TIME_TEXT
        showTime(R.id.widget_date, TIME_VIEWS, frame, dialDp, fraction)
        setViewVisibility(R.id.widget_date, if (isMoon) View.GONE else View.VISIBLE)
        // The moon disc takes the upper half, so the time is padded clear of it.
        val padTop = if (isMoon) ((dialDp * (SundialRenderer.MOON_CENTRE_Y + SundialRenderer.MOON_RADIUS)) * density).toInt() else 0
        setViewPadding(R.id.widget_text_block, 0, padTop, 0, 0)
    }

    private fun RemoteViews.applyStacked(frame: Frame, dialDp: Float) {
        val index = variantIndex(frame.style)
        for ((i, id) in HOUR_VIEWS.withIndex()) styleTime(id, i == index, frame, dialDp, SundialRenderer.STACKED_TEXT)
        for ((i, id) in MINUTE_VIEWS.withIndex()) styleTime(id, i == index, frame, dialDp, SundialRenderer.STACKED_TEXT)
        setViewVisibility(R.id.widget_date, View.VISIBLE)
        setTextColor(R.id.widget_date, frame.textStyle.date)
        setTextViewTextSize(
            R.id.widget_date, TypedValue.COMPLEX_UNIT_SP,
            (dialDp * SundialRenderer.DATE_TEXT).coerceAtLeast(MIN_DATE_SP),
        )
        setViewPadding(R.id.widget_text_block, 0, 0, 0, 0)
    }

    private fun RemoteViews.showTime(dateId: Int, views: List<Int>, frame: Frame, dialDp: Float, fraction: Float) {
        val index = variantIndex(frame.style)
        for ((i, id) in views.withIndex()) styleTime(id, i == index, frame, dialDp, fraction)
        setTextColor(dateId, frame.textStyle.date)
        setTextViewTextSize(
            dateId, TypedValue.COMPLEX_UNIT_SP,
            (dialDp * SundialRenderer.DATE_TEXT).coerceAtLeast(MIN_DATE_SP),
        )
    }

    /** Shows or hides one typeface variant, and styles it if it is the visible one. */
    private fun RemoteViews.styleTime(id: Int, visible: Boolean, frame: Frame, dialDp: Float, fraction: Float) {
        setViewVisibility(id, if (visible) View.VISIBLE else View.GONE)
        if (!visible) return
        setTextColor(id, frame.textStyle.time)
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, (dialDp * fraction).coerceAtLeast(MIN_TIME_SP))
        // Stretch and tracking are remotable, so these need no extra views.
        setFloat(id, "setTextScaleX", stretchFor(frame.style.typeface))
        setFloat(id, "setLetterSpacing", trackingFor(frame.style.typeface))
    }

    /**
     * Index into the per-typeface view lists, which run regular/bold for each family.
     * Condensed reuses the modern views and narrows them with setTextScaleX.
     */
    private fun variantIndex(style: SundialStyle): Int {
        val family = when (style.typeface) {
            // Wide reuses the modern views and stretches them with setTextScaleX, because a
            // bundled @font/ resource does not apply when the launcher inflates our RemoteViews.
            ClockTypeface.MODERN, ClockTypeface.CONDENSED, ClockTypeface.WIDE -> 0
            ClockTypeface.CLASSIC -> 1
        }
        return family * 2 + if (style.weight == ClockWeight.BOLD) 1 else 0
    }

    private fun stretchFor(typeface: ClockTypeface): Float = when (typeface) {
        ClockTypeface.CONDENSED -> 0.82f
        ClockTypeface.WIDE -> 1.18f
        else -> 1f
    }

    private fun trackingFor(typeface: ClockTypeface): Float = when (typeface) {
        ClockTypeface.WIDE -> -0.04f
        ClockTypeface.CONDENSED -> -0.01f
        else -> 0f
    }

    private fun dpToPx(dp: Float, density: Float): Int =
        (dp * density).roundToInt().coerceIn(MIN_BITMAP_PX, MAX_BITMAP_PX)

    private const val TAG = "SundialWidget"

    // Regular/bold pairs per family (modern, classic), in the order variantIndex() computes.
    private val TIME_VIEWS = listOf(
        R.id.time_modern_regular, R.id.time_modern_bold,
        R.id.time_classic_regular, R.id.time_classic_bold,
    )
    private val HOUR_VIEWS = listOf(
        R.id.hour_modern_regular, R.id.hour_modern_bold,
        R.id.hour_classic_regular, R.id.hour_classic_bold,
    )
    private val MINUTE_VIEWS = listOf(
        R.id.minute_modern_regular, R.id.minute_modern_bold,
        R.id.minute_classic_regular, R.id.minute_classic_bold,
    )

    /** The analog hands are rendered at this multiple of the dial, then scaled by the clock view. */
    private const val ANALOG_SIZE = 1.0f

    // Floors for the scaled text on tiny widgets.
    private const val MIN_TIME_SP = 14f
    private const val MIN_DATE_SP = 9f

    /** RemoteViews(Map) allows at most 16 variants. */
    private const val MAX_SIZE_VARIANTS = 16

    /** 2x2 cells on a typical launcher (70dp per cell minus margins). */
    private const val DEFAULT_SIZE_DP = 110

    private const val MIN_BITMAP_PX = 32

    /** Keeps the per-widget bitmap well under the Binder transaction budget. */
    private const val MAX_BITMAP_PX = 1024
}
