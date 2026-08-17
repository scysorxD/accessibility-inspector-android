package com.carlos.accessibilityinspector

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRetentionTest {
    @Test
    fun oldestInspectorSessionsBeyondLimitAreSelectedForDeletion() {
        val files = (1..12).map { "accessibility_inspector_2026-08-${it.toString().padStart(2, '0')}_120000.txt" }

        assertEquals(files.take(2), SessionRetention.filesToDelete(files, limit = 10))
    }

    @Test
    fun unrelatedAndMalformedFilesAreIgnoredWhenSelectingOldValidSession() {
        val files = listOf(
            "notes.txt",
            "accessibility_inspector_bad.txt",
            "accessibility_inspector_2026-08-15_120000.txt",
            "accessibility_inspector_2026-08-16_120000.txt",
            "accessibility_inspector_2026-08-17_120000.txt",
        )

        assertEquals(
            listOf("accessibility_inspector_2026-08-15_120000.txt"),
            SessionRetention.filesToDelete(files, limit = 2),
        )
    }

    @Test
    fun newestSessionUsesSortableFilenameOrder() {
        assertEquals(
            "accessibility_inspector_2026-08-17_120001.txt",
            SessionRetention.latest(
                listOf(
                    "accessibility_inspector_2026-08-17_115959.txt",
                    "accessibility_inspector_2026-08-17_120001.txt",
                    "other.txt",
                ),
            ),
        )
    }

    @Test
    fun collisionSuffixStillCountsAsInspectorSession() {
        val collision = "accessibility_inspector_2026-08-17_120001_1.txt"

        assertEquals(collision, SessionRetention.latest(listOf(collision)))
    }
}
