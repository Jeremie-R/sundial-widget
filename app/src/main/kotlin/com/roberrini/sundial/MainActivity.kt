package com.roberrini.sundial

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
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
import com.roberrini.sundial.location.Cities
import com.roberrini.sundial.location.City
import com.roberrini.sundial.location.LatLon
import com.roberrini.sundial.location.LocationSettings
import com.roberrini.sundial.location.LocationSource
import com.roberrini.sundial.settings.AppearanceUi
import com.roberrini.sundial.sun.DaySpan
import com.roberrini.sundial.sun.SunTimesProvider
import com.roberrini.sundial.widget.SundialWidgetProvider
import com.roberrini.sundial.widget.SundialWidgetUpdater
import com.roberrini.sundial.widget.TapAction
import java.util.Locale

/**
 * The app's only screen: where the sun times come from, and what tapping the widget
 * does. Serves both as the launcher entry point and as the widget's (re)configure
 * activity, in which case it must hand the widget id back with RESULT_OK.
 */
class MainActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedTap: TapAction = TapAction.OpenClock
    private lateinit var appearance: AppearanceUi

    private lateinit var locDevice: RadioButton
    private lateinit var locDeviceLabel: TextView
    private lateinit var locCity: RadioButton
    private lateinit var locCityLabel: TextView
    private lateinit var sunSummary: TextView

    private lateinit var clockOption: RadioButton
    private lateinit var appOption: RadioButton
    private lateinit var noneOption: RadioButton
    private lateinit var clockLabel: TextView
    private lateinit var appLabel: TextView

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
        setContentView(R.layout.activity_main)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        // Configure flow: nothing changes unless the user taps Done.
        if (isConfigureFlow) setResult(RESULT_CANCELED)

        locDevice = findViewById(R.id.loc_device)
        locDeviceLabel = findViewById(R.id.loc_device_label)
        locCity = findViewById(R.id.loc_city)
        locCityLabel = findViewById(R.id.loc_city_label)
        sunSummary = findViewById(R.id.sun_summary)
        clockOption = findViewById(R.id.tap_clock)
        appOption = findViewById(R.id.tap_app)
        noneOption = findViewById(R.id.tap_none)
        clockLabel = findViewById(R.id.tap_clock_label)
        appLabel = findViewById(R.id.tap_app_label)

        appearance = AppearanceUi(this, findViewById(android.R.id.content)) { SundialWidgetUpdater.updateAll(this) }

        locDevice.setOnClickListener { useDeviceLocation() }
        locCity.setOnClickListener { pickCity() }

        clockLabel.text = clockAppLabel() ?: getString(R.string.no_clock_app)
        selectedTap = TapAction.load(this)
        showTapSelection()
        clockOption.setOnClickListener { selectTap(TapAction.OpenClock) }
        noneOption.setOnClickListener { selectTap(TapAction.None) }
        appOption.setOnClickListener { pickApp() }

        findViewById<Button>(R.id.done).setOnClickListener { save() }
        findViewById<Button>(R.id.add_widget).apply {
            visibility = if (isConfigureFlow) View.GONE else View.VISIBLE
            setOnClickListener { requestPinWidget() }
        }

        refreshLocationUi()
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
            else -> (times.daylight as DaySpan.Between).let { getString(R.string.sun_summary, clock(it.start), clock(it.end)) }
        }
        // The preview draws today's sun path, so it follows the location too.
        appearance.refresh()
    }

    private fun clock(fraction: Float): String {
        val minutes = (fraction * 1440).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)
    }

    // ---- Tap action ----

    private fun selectTap(action: TapAction) {
        selectedTap = action
        showTapSelection()
    }

    private fun showTapSelection() {
        clockOption.isChecked = selectedTap is TapAction.OpenClock
        appOption.isChecked = selectedTap is TapAction.OpenApp
        noneOption.isChecked = selectedTap is TapAction.None
        appLabel.text = (selectedTap as? TapAction.OpenApp)
            ?.let { labelOf(it.component) }
            ?: getString(R.string.no_app_chosen)
    }

    private fun clockAppLabel(): String? {
        val info = Intent(AlarmClock.ACTION_SHOW_ALARMS).resolveActivityInfo(packageManager, 0) ?: return null
        return info.loadLabel(packageManager).toString()
    }

    private fun labelOf(component: ComponentName): String? = runCatching {
        packageManager.getActivityInfo(component, 0).loadLabel(packageManager).toString()
    }.getOrNull()

    private fun pickApp() {
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
            .setAdapter(adapter) { _, which -> selectTap(TapAction.OpenApp(apps[which].component)) }
            // Dismissed without choosing: keep whatever was selected before.
            .setOnCancelListener { showTapSelection() }
            .show()
    }

    // ---- Done / pin ----

    private fun save() {
        TapAction.save(this, selectedTap)
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
