package com.roberrini.sundial

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.roberrini.sundial.action.ActionSlot
import com.roberrini.sundial.action.LaunchAction
import com.roberrini.sundial.location.Cities
import com.roberrini.sundial.location.LocationSettings
import com.roberrini.sundial.location.LocationSource
import com.roberrini.sundial.moon.MoonInfo
import com.roberrini.sundial.moon.MoonProvider
import com.roberrini.sundial.render.DialPreview
import com.roberrini.sundial.render.MoonRenderer
import com.roberrini.sundial.sun.DaySpan
import com.roberrini.sundial.sun.SunTimes
import com.roberrini.sundial.sun.SunTimesProvider
import com.roberrini.sundial.util.DayTimeFormat
import kotlin.math.roundToInt

/**
 * The app's home screen: the dial as the widget draws it now, today's sun and moon for
 * the configured location, and the buttons that leave for the clock app or settings.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var dial: ImageView
    private lateinit var location: TextView
    private lateinit var moonIcon: ImageView
    private lateinit var moonPhase: TextView
    private lateinit var moonIllumination: TextView
    private lateinit var primaryButton: MaterialButton
    private lateinit var secondaryButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dial = findViewById(R.id.dial)
        location = findViewById(R.id.location)
        moonIcon = findViewById(R.id.moon_icon)
        moonPhase = findViewById(R.id.moon_phase)
        moonIllumination = findViewById(R.id.moon_illumination)
        primaryButton = findViewById(R.id.button_primary)
        secondaryButton = findViewById(R.id.button_secondary)

        findViewById<Button>(R.id.button_settings).setOnClickListener { openSettings() }
        // The sun and moon need a location, and that's set in settings.
        location.setOnClickListener { openSettings() }

        // First run: there is no location yet, so start where the permission is asked for.
        if (savedInstanceState == null && LocationSettings.source(this) == null) openSettings()
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        // Everything here is "now": re-read it each time the screen comes forward.
        showDial()
        showLocation()
        showSun(SunTimesProvider.today(this))
        showMoon(MoonProvider.today(this))
        showButtons()
    }

    private fun showDial() {
        val size = resources.getDimensionPixelSize(R.dimen.landing_dial_size)
        dial.setImageBitmap(DialPreview.render(this, size))
    }

    private fun showLocation() {
        val settings = LocationSettings.source(this)
        location.text = when {
            settings == LocationSource.CITY -> LocationSettings.city(this)?.label
            settings == LocationSource.DEVICE -> LocationSettings.storedDeviceLocation(this)
                ?.let { getString(R.string.loc_near, Cities.nearest(it.lat, it.lon).name) }
            else -> null
        } ?: getString(R.string.landing_no_location)
    }

    private fun showSun(sun: SunTimes) {
        val daylight = sun.daylight
        infoRow(R.id.row_sunrise, R.string.info_sunrise, single(daylight, start = true))
        infoRow(R.id.row_sunset, R.string.info_sunset, single(daylight, start = false))
        infoRow(R.id.row_daylight, R.string.info_daylight, dayLength(daylight))
        infoRow(R.id.row_civil, R.string.info_civil, range(sun.civil))
        infoRow(R.id.row_nautical, R.string.info_nautical, range(sun.nautical))
        infoRow(R.id.row_astronomical, R.string.info_astronomical, range(sun.astronomical))
    }

    private fun showMoon(moon: MoonInfo?) {
        val hasMoon = moon != null
        findViewById<View>(R.id.moon_section).visibility = if (hasMoon) View.VISIBLE else View.GONE
        if (moon == null) return

        val size = resources.getDimensionPixelSize(R.dimen.moon_icon_size)
        moonIcon.setImageBitmap(
            MoonRenderer.render(
                sizePx = size,
                illumination = moon.illumination,
                waning = moon.waning,
                lit = MaterialColors.getColor(moonIcon, androidx.appcompat.R.attr.colorPrimary),
                dark = MaterialColors.getColor(moonIcon, com.google.android.material.R.attr.colorSurfaceVariant),
            ),
        )
        moonPhase.setText(moon.phaseName.label)
        moonIllumination.text = getString(R.string.moon_lit, (moon.illumination * 100).roundToInt())
        infoRow(
            R.id.row_moonrise, R.string.info_moonrise,
            when {
                moon.alwaysUp -> getString(R.string.moon_always_up)
                moon.alwaysDown -> getString(R.string.moon_always_down)
                else -> DayTimeFormat.of(this, moon.rise)
            },
        )
        infoRow(
            R.id.row_moonset, R.string.info_moonset,
            if (moon.alwaysUp || moon.alwaysDown) "" else DayTimeFormat.of(this, moon.set),
        )
    }

    private fun showButtons() {
        bind(primaryButton, ActionSlot.BUTTON_PRIMARY)
        bind(secondaryButton, ActionSlot.BUTTON_SECONDARY)
    }

    private fun bind(button: MaterialButton, slot: ActionSlot) {
        val action = LaunchAction.load(this, slot)
        val intent = action.toIntent(this)
        // A button with nothing to open would be a dead end, so it isn't shown at all.
        button.visibility = if (intent == null) View.GONE else View.VISIBLE
        button.text = action.label(this)
        button.setOnClickListener {
            runCatching { startActivity(intent) }
                .onFailure { Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun infoRow(rowId: Int, labelId: Int, value: String) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.info_label).setText(labelId)
        row.findViewById<TextView>(R.id.info_value).text = value
    }

    private fun single(span: DaySpan, start: Boolean): String = when (span) {
        DaySpan.All -> getString(R.string.sun_all_day_short)
        DaySpan.None -> getString(R.string.sun_none_short)
        is DaySpan.Between -> DayTimeFormat.of(this, if (start) span.start else span.end)
    }

    private fun range(span: DaySpan): String = when (span) {
        DaySpan.All -> getString(R.string.sun_all_day_short)
        DaySpan.None -> getString(R.string.sun_none_short)
        is DaySpan.Between -> getString(
            R.string.info_range, DayTimeFormat.of(this, span.start), DayTimeFormat.of(this, span.end),
        )
    }

    private fun dayLength(span: DaySpan): String {
        val fraction = when (span) {
            DaySpan.All -> 1f
            DaySpan.None -> 0f
            is DaySpan.Between -> (span.end - span.start + 1f) % 1f
        }
        val minutes = (fraction * 1440).roundToInt()
        return getString(R.string.info_duration, minutes / 60, minutes % 60)
    }
}
