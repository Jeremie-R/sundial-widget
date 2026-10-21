package com.roberrini.sundial

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.roberrini.sundial.action.ActionSlot
import com.roberrini.sundial.action.LaunchAction
import com.roberrini.sundial.location.Cities
import com.roberrini.sundial.location.City
import com.roberrini.sundial.location.LatLon
import com.roberrini.sundial.location.LocationSettings
import com.roberrini.sundial.location.LocationSource
import com.roberrini.sundial.settings.AppearanceUi
import com.roberrini.sundial.sun.DaySpan
import com.roberrini.sundial.sun.SunTimesProvider
import com.roberrini.sundial.util.DayTimeFormat
import com.roberrini.sundial.widget.SundialWidgetProvider
import com.roberrini.sundial.widget.SundialWidgetUpdater
import java.util.Locale

/**
 * Appearance, where the sun times come from, and what each button and the widget tap
 * opens. Also serves as the widget's (re)configure activity, in which case it must hand
 * the widget id back with RESULT_OK.
 */
class SettingsActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var appearance: AppearanceUi

    private lateinit var locDevice: RadioButton
    private lateinit var locDeviceLabel: TextView
    private lateinit var locCity: RadioButton
    private lateinit var locCityLabel: TextView
    private lateinit var sunSummary: TextView

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                LocationSettings.setSource(this, LocationSource.DEVICE)
                fetchDeviceLocation()
            } else {
                // Refused: fall back to a manual city, and offer the picker straight away.
                LocationSettings.setSource(this, LocationSource.CITY)
                if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    // Android won't show the prompt again: explain, and lead to the picker from there.
                    explainPermanentDenial()
                } else if (LocationSettings.city(this) == null) {
                    pickCity()
                }
            }
            refreshLocationUi()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        // Configure flow: nothing is handed back unless the user taps Done.
        if (isConfigureFlow) setResult(RESULT_CANCELED)

        locDevice = findViewById(R.id.loc_device)
        locDeviceLabel = findViewById(R.id.loc_device_label)
        locCity = findViewById(R.id.loc_city)
        locCityLabel = findViewById(R.id.loc_city_label)
        sunSummary = findViewById(R.id.sun_summary)

        appearance = AppearanceUi(this, findViewById(android.R.id.content)) {
            SundialWidgetUpdater.updateAll(this)
        }

        locDevice.setOnClickListener { useDeviceLocation() }
        locCity.setOnClickListener { pickCity() }

        for (slot in ActionSlot.entries) {
            findViewById<View>(rowIdFor(slot)).setOnClickListener { pickAction(slot) }
        }

        findViewById<Button>(R.id.done).setOnClickListener { save() }
        findViewById<Button>(R.id.add_widget).apply {
            visibility = if (isConfigureFlow) View.GONE else View.VISIBLE
            setOnClickListener { requestPinWidget() }
        }

        refreshLocationUi()
        refreshActions()
        // First run: ask for the permission right away; the fallback is the city picker.
        if (savedInstanceState == null && LocationSettings.source(this) == null) useDeviceLocation()
    }

    override fun onResume() {
        super.onResume()
        // The widget can't get a fix in the background, so refresh it whenever we're visible.
        if (LocationSettings.source(this) == LocationSource.DEVICE && LocationSettings.hasPermission(this)) {
            fetchDeviceLocation()
        }
    }

    private val isConfigureFlow: Boolean
        get() = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID

    // ---- Location ----

    private fun useDeviceLocation() {
        if (LocationSettings.hasPermission(this)) {
            LocationSettings.setSource(this, LocationSource.DEVICE)
            fetchDeviceLocation()
            refreshLocationUi()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun fetchDeviceLocation() {
        if (!LocationSettings.hasPermission(this)) return
        val provider = LocationSettings.coarseProvider(this) ?: return
        val manager = getSystemService(LocationManager::class.java)
        try {
            manager.getLastKnownLocation(provider)?.let { onDeviceLocation(LatLon(it.latitude, it.longitude)) }
            manager.getCurrentLocation(provider, null, mainExecutor) { location ->
                if (location != null && !isDestroyed) onDeviceLocation(LatLon(location.latitude, location.longitude))
            }
        } catch (e: SecurityException) {
            refreshLocationUi()
        }
    }

    private fun onDeviceLocation(location: LatLon) {
        LocationSettings.storeDeviceLocation(this, location)
        refreshLocationUi()
        SundialWidgetUpdater.updateAll(this)
    }

    private fun explainPermanentDenial() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.perm_denied_title)
            .setMessage(R.string.perm_denied_body)
            .setNegativeButton(R.string.perm_denied_use_city) { _, _ ->
                if (LocationSettings.city(this) == null) pickCity()
            }
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
                )
            }
            .show()
    }

    private fun pickCity() {
        val view = layoutInflater.inflate(R.layout.dialog_city_picker, null)
        val search = view.findViewById<EditText>(R.id.city_search)
        val list = view.findViewById<ListView>(R.id.city_list)
        val shown = ArrayList(Cities.all)
        val adapter = object : ArrayAdapter<City>(this, R.layout.item_city, R.id.city_name, shown) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).also { row ->
                    val city = getItem(position) ?: return row
                    row.findViewById<TextView>(R.id.city_name).text = city.name
                    row.findViewById<TextView>(R.id.city_country).text = city.country
                }
        }
        list.adapter = adapter
        search.doAfterTextChanged { text ->
            val query = text?.toString()?.trim().orEmpty().lowercase(Locale.ROOT)
            shown.clear()
            shown.addAll(Cities.all.filter { query.isEmpty() || it.label.lowercase(Locale.ROOT).contains(query) })
            adapter.notifyDataSetChanged()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_city)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setOnCancelListener { refreshLocationUi() }
            .show()
        list.setOnItemClickListener { _, _, position, _ ->
            val city = adapter.getItem(position) ?: return@setOnItemClickListener
            LocationSettings.setCity(this, city)
            refreshLocationUi()
            SundialWidgetUpdater.updateAll(this)
            dialog.dismiss()
        }
    }

    private fun refreshLocationUi() {
        val source = LocationSettings.source(this)
        locDevice.isChecked = source == LocationSource.DEVICE
        locCity.isChecked = source == LocationSource.CITY

        locDeviceLabel.text = when {
            !LocationSettings.hasPermission(this) -> getString(R.string.loc_device_denied)
            source != LocationSource.DEVICE -> getString(R.string.loc_device_available)
            else -> LocationSettings.storedDeviceLocation(this)
                ?.let { getString(R.string.loc_near, Cities.nearest(it.lat, it.lon).name) }
                ?: getString(R.string.loc_device_waiting)
        }
        locCityLabel.text = LocationSettings.city(this)?.label ?: getString(R.string.loc_city_none)

        val located = LocationSettings.resolve(this) != null
        val times = SunTimesProvider.today(this)
        sunSummary.text = when {
            !located -> getString(R.string.sun_summary_default)
            times.daylight is DaySpan.All -> getString(R.string.sun_all_day)
            times.daylight is DaySpan.None -> getString(R.string.sun_none)
            else -> (times.daylight as DaySpan.Between).let {
                getString(R.string.sun_summary, DayTimeFormat.of(this, it.start), DayTimeFormat.of(this, it.end))
            }
        }
        // The preview draws today's sun path, so it follows the location too.
        appearance.refresh()
    }

    // ---- Buttons and widget tap ----

    private fun rowIdFor(slot: ActionSlot): Int = when (slot) {
        ActionSlot.WIDGET_TAP -> R.id.row_widget_tap
        ActionSlot.BUTTON_PRIMARY -> R.id.row_button_primary
        ActionSlot.BUTTON_SECONDARY -> R.id.row_button_secondary
    }

    private fun refreshActions() {
        for (slot in ActionSlot.entries) {
            val row = findViewById<View>(rowIdFor(slot))
            row.findViewById<TextView>(R.id.row_title).setText(slot.title)
            row.findViewById<TextView>(R.id.row_value).text = LaunchAction.load(this, slot).label(this)
        }
    }

    private fun pickAction(slot: ActionSlot) {
        val current = LaunchAction.load(this, slot)
        val choices = listOf(LaunchAction.ShowAlarms, LaunchAction.ShowTimers, null, LaunchAction.None)
        val labels = choices.map { choice ->
            when (choice) {
                // The "another app" row doubles as the current app's name once one is chosen.
                null -> (current as? LaunchAction.OpenApp)?.label(this) ?: getString(R.string.action_choose_app)
                else -> choice.label(this)
            }
        }
        val checked = when (current) {
            is LaunchAction.OpenApp -> 2
            else -> choices.indexOf(current)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(slot.title)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
                dialog.dismiss()
                val choice = choices[which]
                if (choice == null) pickApp(slot) else applyAction(slot, choice)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyAction(slot: ActionSlot, action: LaunchAction) {
        LaunchAction.save(this, slot, action)
        refreshActions()
        if (slot == ActionSlot.WIDGET_TAP) SundialWidgetUpdater.updateAll(this)
    }

    private fun pickApp(slot: ActionSlot) {
        val pm = packageManager
        @Suppress("DEPRECATION")
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0,
        )
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppEntry(
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm),
                    component = ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                )
            }
            .sortedBy { it.label.lowercase(Locale.ROOT) }

        val adapter = object : ArrayAdapter<AppEntry>(this, R.layout.item_app, R.id.app_label, apps) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).also { row ->
                    row.findViewById<ImageView>(R.id.app_icon).setImageDrawable(getItem(position)?.icon)
                }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_app)
            .setAdapter(adapter) { _, which -> applyAction(slot, LaunchAction.OpenApp(apps[which].component)) }
            .show()
    }

    // ---- Done / pin ----

    private fun save() {
        SundialWidgetUpdater.updateAll(this)
        if (isConfigureFlow) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        }
        finish()
    }

    private fun requestPinWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, SundialWidgetProvider::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
        }
    }

    private class AppEntry(val label: String, val icon: Drawable, val component: ComponentName) {
        override fun toString() = label
    }
}
