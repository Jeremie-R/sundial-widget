package com.roberrini.sundial.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.roberrini.sundial.R
import com.roberrini.sundial.render.Look
import com.roberrini.sundial.render.NightSun
import com.roberrini.sundial.render.Shape
import com.roberrini.sundial.render.SundialColors
import com.roberrini.sundial.render.SundialRenderer
import com.roberrini.sundial.render.SundialStyle
import com.roberrini.sundial.sun.SunTimesProvider
import com.roberrini.sundial.time.DayProgress
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The appearance section of the settings screen: a live preview plus pickers for look,
 * background shape, colours, theme, and the night details. Thumbnails are rendered by
 * [SundialRenderer] itself, so what's shown is what the widget will draw.
 */
class AppearanceUi(
    private val activity: AppCompatActivity,
    root: View,
    private val onChanged: () -> Unit,
) {
    private val context: Context get() = activity
    private val preview: ImageView = root.findViewById(R.id.appearance_preview)
    private val rowLook = Row(root.findViewById(R.id.row_look), R.string.row_style) { pickLook() }
    private val rowShape = Row(root.findViewById(R.id.row_shape), R.string.row_shape) { pickShape() }
    private val rowHue = Row(root.findViewById(R.id.row_hue), R.string.row_hue) { pickHue() }
    private val rowTheme = Row(root.findViewById(R.id.row_theme), R.string.row_theme) { pickTheme() }
    private val rowNight = Row(root.findViewById(R.id.row_night), R.string.row_night) { pickNightSun() }
    private val sparkles: MaterialSwitch = root.findViewById(R.id.sparkles_switch)

    init {
        sparkles.setOnCheckedChangeListener { _, checked ->
            if (checked != AppearanceSettings.style(context).sparkles) {
                AppearanceSettings.setSparkles(context, checked)
                changed()
            }
        }
    }

    fun refresh() {
        val style = AppearanceSettings.style(context)
        rowLook.value(context.getString(style.look.label))
        rowShape.value(context.getString(style.shape.label))
        rowHue.value(context.getString(AppearanceSettings.huePreset(context).label))
        rowTheme.value(context.getString(AppearanceSettings.themeMode(context).label))
        rowNight.value(context.getString(style.nightSun.label))
        sparkles.isChecked = style.sparkles
        preview.setImageBitmap(renderSample(dp(PREVIEW_DP), style, AppearanceSettings.colors(context)))
    }

    private fun changed() {
        refresh()
        onChanged()
    }

    // ---- pickers ----

    private fun pickLook() {
        val style = AppearanceSettings.style(context)
        val colors = AppearanceSettings.colors(context)
        val options = Look.entries.map { look ->
            Thumb(context.getString(look.label), renderSample(dp(THUMB_DP), style.copy(look = look), colors), look == style.look)
        }
        gridDialog(R.string.row_style, options) { AppearanceSettings.setLook(context, Look.entries[it]); changed() }
    }

    private fun pickShape() {
        val style = AppearanceSettings.style(context)
        val colors = AppearanceSettings.colors(context)
        val options = Shape.entries.map { shape ->
            Thumb(context.getString(shape.label), renderSample(dp(THUMB_DP), style.copy(shape = shape), colors), shape == style.shape)
        }
        gridDialog(R.string.row_shape, options) { AppearanceSettings.setShape(context, Shape.entries[it]); changed() }
    }

    private fun pickHue() {
        val current = AppearanceSettings.huePreset(context)
        val dark = AppearanceSettings.isDark(context)
        val presets = HuePreset.entries
        val adapter = object : BaseAdapter() {
            override fun getCount() = presets.size
            override fun getItem(position: Int) = presets[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_hue, parent, false)
                val preset = presets[position]
                val palette = preset.degrees?.let { SundialColors.fromHue(it, dark) } ?: SundialColors.fromSystem(context, dark)
                row.findViewById<View>(R.id.hue_swatch).background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(palette.primary)
                    setStroke(dp(2f), palette.primaryContainer)
                }
                row.findViewById<TextView>(R.id.hue_label).text = context.getString(preset.label)
                row.findViewById<TextView>(R.id.hue_label).setTypeface(null, if (preset == current) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                return row
            }
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.row_hue)
            .setAdapter(adapter) { _, which -> AppearanceSettings.setHuePreset(context, presets[which]); changed() }
            .show()
    }

    private fun pickTheme() = choiceDialog(
        R.string.row_theme, ThemeMode.entries.map { context.getString(it.label) },
        ThemeMode.entries.indexOf(AppearanceSettings.themeMode(context)),
    ) { AppearanceSettings.setThemeMode(context, ThemeMode.entries[it]); changed() }

    private fun pickNightSun() = choiceDialog(
        R.string.row_night, NightSun.entries.map { context.getString(it.label) },
        NightSun.entries.indexOf(AppearanceSettings.style(context).nightSun),
    ) { AppearanceSettings.setNightSun(context, NightSun.entries[it]); changed() }

    private fun choiceDialog(title: Int, labels: List<String>, checked: Int, onPick: (Int) -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which -> onPick(which); dialog.dismiss() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private class Thumb(val label: String, val bitmap: Bitmap, val selected: Boolean)

    private fun gridDialog(title: Int, options: List<Thumb>, onPick: (Int) -> Unit) {
        val grid = LayoutInflater.from(context).inflate(R.layout.dialog_grid, null) as GridView
        grid.adapter = object : BaseAdapter() {
            override fun getCount() = options.size
            override fun getItem(position: Int) = options[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val cell = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_thumb, parent, false)
                val option = options[position]
                cell.findViewById<ImageView>(R.id.thumb_image).setImageBitmap(option.bitmap)
                cell.findViewById<TextView>(R.id.thumb_label).text = option.label
                cell.findViewById<View>(R.id.thumb_frame).isSelected = option.selected
                return cell
            }
        }
        var dialog: AlertDialog? = null
        grid.setOnItemClickListener { _, _, position, _ -> onPick(position); dialog?.dismiss() }
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(grid)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ---- rendering ----

    /** The face as the widget would draw it right now, with the current time painted in. */
    private fun renderSample(px: Int, style: SundialStyle, colors: SundialColors): Bitmap {
        val now = LocalTime.now()
        val today = LocalDate.now()
        // Same 12/24-hour choice as the widget's TextClock.
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        val text = SundialRenderer.TextOverlay(
            time = now.format(DateTimeFormatter.ofPattern(pattern)),
            date = "${today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${today.dayOfMonth}",
        )
        return SundialRenderer.render(px, px, DayProgress.now(), SunTimesProvider.today(context), colors, style, text)
    }

    private fun dp(value: Float): Int = (value * context.resources.displayMetrics.density).roundToInt()

    private inner class Row(view: View, title: Int, onClick: () -> Unit) {
        private val valueView: TextView = view.findViewById(R.id.row_value)
        init {
            view.findViewById<TextView>(R.id.row_title).text = context.getString(title)
            view.setOnClickListener { onClick() }
        }
        fun value(text: String) { valueView.text = text }
    }

    private companion object {
        const val PREVIEW_DP = 176f
        const val THUMB_DP = 96f
    }
}
