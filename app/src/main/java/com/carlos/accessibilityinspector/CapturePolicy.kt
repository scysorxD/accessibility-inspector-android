package com.carlos.accessibilityinspector

enum class CaptureMode {
    ALL_APPS,
    CABIFY_ONLY,
}

object CapturePolicy {
    fun accepts(
        mode: CaptureMode,
        selectedPackage: String,
        eventPackage: String,
    ): Boolean {
        val packageName = eventPackage.trim()
        if (packageName.isEmpty()) return false
        return when (mode) {
            CaptureMode.ALL_APPS -> true
            CaptureMode.CABIFY_ONLY ->
                selectedPackage.trim().takeIf(String::isNotEmpty) == packageName
        }
    }
}
