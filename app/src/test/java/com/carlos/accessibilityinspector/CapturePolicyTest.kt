package com.carlos.accessibilityinspector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturePolicyTest {
    @Test
    fun allAppsAcceptsAnyNonBlankPackage() {
        assertTrue(CapturePolicy.accepts(CaptureMode.ALL_APPS, "", "com.example"))
    }

    @Test
    fun cabifyOnlyAcceptsOnlySelectedPackage() {
        assertTrue(
            CapturePolicy.accepts(
                CaptureMode.CABIFY_ONLY,
                "com.example.cabify",
                "com.example.cabify",
            ),
        )
        assertFalse(
            CapturePolicy.accepts(
                CaptureMode.CABIFY_ONLY,
                "com.example.cabify",
                "com.android.settings",
            ),
        )
    }

    @Test
    fun blankSelectedPackageRejectsCabifyOnlySafely() {
        assertFalse(CapturePolicy.accepts(CaptureMode.CABIFY_ONLY, " ", "com.example"))
    }

    @Test
    fun blankEventPackageIsRejected() {
        assertFalse(CapturePolicy.accepts(CaptureMode.ALL_APPS, "", " "))
    }
}
