package com.roberrini.sundial.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
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
import com.roberrini.sundial.render.SundialRenderer
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
    )

    /** Activity to launch on tap, per the user's [TapAction]; null means the widget isn't clickable. */
    private fun tapPendingIntent(context: Context): PendingIntent? {
        val intent = TapAction.load(context).toIntent(context) ?: return null
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
        // The bitmap covers the whole cell: the background shape is part of the drawing.
        val bitmap = SundialRenderer.render(
            dpToPx(size.width, density), dpToPx(size.height, density),
            frame.progress, frame.sunTimes, frame.colors, frame.style,
        )

        // The text sits inside the dial, so it scales with the dial rather than staying a fixed sp.
        val dialDp = min(size.width, size.height)
        val timeSp = (dialDp * SundialRenderer.TIME_TEXT).coerceAtLeast(MIN_TIME_SP)
        val dateSp = (dialDp * SundialRenderer.DATE_TEXT).coerceAtLeast(MIN_DATE_SP)
        val text = frame.textStyle
        val timeId = if (text.heavy) R.id.widget_time else R.id.widget_time_medium
        val hiddenTimeId = if (text.heavy) R.id.widget_time_medium else R.id.widget_time
        return RemoteViews(context.packageName, R.layout.widget_sundial).apply {
            setImageViewBitmap(R.id.widget_dial, bitmap)
            setViewVisibility(timeId, View.VISIBLE)
            setViewVisibility(hiddenTimeId, View.GONE)
            setTextColor(timeId, text.time)
            setTextColor(R.id.widget_date, text.date)
            setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, timeSp)
            setTextViewTextSize(R.id.widget_date, TypedValue.COMPLEX_UNIT_SP, dateSp)
            if (frame.tapIntent != null) setOnClickPendingIntent(R.id.widget_root, frame.tapIntent)
        }
    }

    private fun dpToPx(dp: Float, density: Float): Int =
        (dp * density).roundToInt().coerceIn(MIN_BITMAP_PX, MAX_BITMAP_PX)

    private const val TAG = "SundialWidget"

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
