package com.roberrini.sundial.action

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.roberrini.sundial.R

/** What a widget tap or a landing-page button does. */
sealed class LaunchAction {

    /** The clock app's alarms screen. */
    data object ShowAlarms : LaunchAction()

    /** The clock app's timers screen. */
    data object ShowTimers : LaunchAction()

    /** The launcher activity of an app the user picked. */
    data class OpenApp(val component: ComponentName) : LaunchAction()

    data object None : LaunchAction()

    /** Resolved, launchable intent, or null when nothing should happen (or nothing handles it). */
    fun toIntent(context: Context): Intent? {
        val intent = when (this) {
            ShowAlarms -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            ShowTimers -> Intent(AlarmClock.ACTION_SHOW_TIMERS)
            is OpenApp -> Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component)
            None -> return null
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    /** Short label for a button; the app's own name when it opens an app. */
    fun label(context: Context): String = when (this) {
        ShowAlarms -> context.getString(R.string.action_alarms)
        ShowTimers -> context.getString(R.string.action_timers)
        None -> context.getString(R.string.action_none)
        is OpenApp -> runCatching {
            context.packageManager.getActivityInfo(component, 0).loadLabel(context.packageManager).toString()
        }.getOrNull() ?: context.getString(R.string.action_app)
    }

    companion object {
        private const val PREFS = "sundial"
        private const val KIND_ALARMS = "alarms"
        private const val KIND_TIMERS = "timers"
        private const val KIND_APP = "app"
        private const val KIND_NONE = "none"

        /** Written by versions before the landing page, when the only action was the clock. */
        private const val KIND_LEGACY_CLOCK = "clock"

        fun load(context: Context, slot: ActionSlot): LaunchAction {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return when (prefs.getString(slot.key, null)) {
                KIND_ALARMS, KIND_LEGACY_CLOCK -> ShowAlarms
                KIND_TIMERS -> ShowTimers
                KIND_NONE -> None
                KIND_APP -> prefs.getString(slot.componentKey, null)
                    ?.let(ComponentName::unflattenFromString)
                    ?.let(::OpenApp)
                    ?: slot.default
                else -> slot.default
            }
        }

        fun save(context: Context, slot: ActionSlot, action: LaunchAction) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                when (action) {
                    ShowAlarms -> putString(slot.key, KIND_ALARMS).remove(slot.componentKey)
                    ShowTimers -> putString(slot.key, KIND_TIMERS).remove(slot.componentKey)
                    None -> putString(slot.key, KIND_NONE).remove(slot.componentKey)
                    is OpenApp -> putString(slot.key, KIND_APP)
                        .putString(slot.componentKey, action.component.flattenToString())
                }
            }
        }
    }
}

/** The three places the user can assign an action to. */
enum class ActionSlot(
    val key: String,
    @param:StringRes val title: Int,
    val default: LaunchAction,
) {
    WIDGET_TAP("tap_action", R.string.slot_widget_tap, LaunchAction.ShowAlarms),
    BUTTON_PRIMARY("button_primary", R.string.slot_button_primary, LaunchAction.ShowAlarms),
    BUTTON_SECONDARY("button_secondary", R.string.slot_button_secondary, LaunchAction.ShowTimers);

    val componentKey: String get() = "${key}_component"
}
