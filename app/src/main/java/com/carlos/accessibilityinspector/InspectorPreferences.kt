package com.carlos.accessibilityinspector

import android.content.Context
import androidx.core.content.edit

class InspectorPreferences(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var captureEnabled: Boolean
        get() = preferences.getBoolean(KEY_CAPTURE_ENABLED, false)
        set(value) = preferences.edit { putBoolean(KEY_CAPTURE_ENABLED, value) }

    var captureMode: CaptureMode
        get() = runCatching {
            CaptureMode.valueOf(
                preferences.getString(KEY_CAPTURE_MODE, CaptureMode.ALL_APPS.name)
                    ?: CaptureMode.ALL_APPS.name,
            )
        }.getOrDefault(CaptureMode.ALL_APPS)
        set(value) = preferences.edit { putString(KEY_CAPTURE_MODE, value.name) }

    var selectedPackage: String
        get() = preferences.getString(KEY_SELECTED_PACKAGE, "").orEmpty()
        set(value) = preferences.edit { putString(KEY_SELECTED_PACKAGE, value.trim()) }

    var lastObservedPackage: String
        get() = preferences.getString(KEY_LAST_OBSERVED_PACKAGE, "").orEmpty()
        set(value) = preferences.edit { putString(KEY_LAST_OBSERVED_PACKAGE, value.trim()) }

    var latestLogPath: String
        get() = preferences.getString(KEY_LATEST_LOG_PATH, "").orEmpty()
        set(value) = preferences.edit { putString(KEY_LATEST_LOG_PATH, value) }

    companion object {
        private const val PREFERENCES_NAME = "inspector_preferences"
        private const val KEY_CAPTURE_ENABLED = "capture_enabled"
        private const val KEY_CAPTURE_MODE = "capture_mode"
        private const val KEY_SELECTED_PACKAGE = "selected_package"
        private const val KEY_LAST_OBSERVED_PACKAGE = "last_observed_package"
        private const val KEY_LATEST_LOG_PATH = "latest_log_path"
    }
}
