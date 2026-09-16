package com.roberrini.sundial.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.core.content.edit

/** What tapping the widget does. Stored as a single app-wide preference. */
sealed class TapAction {

    /** Opens whatever app handles [AlarmClock.ACTION_SHOW_ALARMS] (the system clock app). */
    data object OpenClock : TapAction()

    /** Opens the launcher activity of a user-chosen app. */
    data class OpenApp(val component: ComponentName) : TapAction()

    data object None : TapAction()

    /** Resolved, launchable intent for this action, or null if nothing should happen. */
    fun toIntent(context: Context): Intent? {
        val intent = when (this) {
            OpenClock -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            is OpenApp -> Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component)
            None -> return null
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    companion object {
        private const val PREFS = "sundial"
        private const val KEY_KIND = "tap_action"
        private const val KEY_COMPONENT = "tap_action_component"
        private const val KIND_CLOCK = "clock"
        private const val KIND_APP = "app"
        private const val KIND_NONE = "none"

        fun load(context: Context): TapAction {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return when (prefs.getString(KEY_KIND, KIND_CLOCK)) {
                KIND_NONE -> None
                KIND_APP -> prefs.getString(KEY_COMPONENT, null)
                    ?.let(ComponentName::unflattenFromString)
                    ?.let(::OpenApp)
                    ?: OpenClock
                else -> OpenClock
            }
        }

        fun save(context: Context, action: TapAction) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                when (action) {
                    OpenClock -> putString(KEY_KIND, KIND_CLOCK).remove(KEY_COMPONENT)
                    is OpenApp -> putString(KEY_KIND, KIND_APP)
                        .putString(KEY_COMPONENT, action.component.flattenToString())
                    None -> putString(KEY_KIND, KIND_NONE).remove(KEY_COMPONENT)
                }
            }
        }
    }
}
