package com.carlos.accessibilityinspector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureRuntimePolicyTest {
    @Test
    fun debounceUsesQuietDelayWhenEventsStop() {
        val policy = BoundedDebouncePolicy(quietDelayMillis = 750, maxDelayMillis = 3_000)

        assertEquals(750, policy.delayAfterEvent(nowMillis = 1_000))
        assertEquals(750, policy.delayAfterEvent(nowMillis = 1_500))
    }

    @Test
    fun debounceCapsContinuousEventsAtThreeSecondsFromBurstStart() {
        val policy = BoundedDebouncePolicy(quietDelayMillis = 750, maxDelayMillis = 3_000)

        assertEquals(750, policy.delayAfterEvent(nowMillis = 0))
        assertEquals(750, policy.delayAfterEvent(nowMillis = 700))
        assertEquals(750, policy.delayAfterEvent(nowMillis = 1_400))
        assertEquals(200, policy.delayAfterEvent(nowMillis = 2_800))
    }

    @Test
    fun debounceStartsNewBurstAfterSnapshotTriggers() {
        val policy = BoundedDebouncePolicy(quietDelayMillis = 750, maxDelayMillis = 3_000)
        policy.delayAfterEvent(nowMillis = 0)

        policy.onSnapshotTriggered()

        assertEquals(750, policy.delayAfterEvent(nowMillis = 3_001))
    }

    @Test
    fun sessionGateRejectsStoppedAndSupersededSessionIds() {
        val gate = SessionGate()
        val first = gate.begin()
        assertTrue(gate.accepts(first))

        assertEquals(first, gate.deactivate())
        assertFalse(gate.accepts(first))

        val second = gate.begin()
        assertFalse(gate.accepts(first))
        assertTrue(gate.accepts(second))
    }

    @Test
    fun sizeGateWritesOneMarkerAndAlwaysAllowsFooter() {
        val gate = LogSizeGate(maxBytes = 10)

        assertEquals(LogWriteDecision(writeEntry = true), gate.normal(currentBytes = 0, entryBytes = 9))
        assertEquals(
            LogWriteDecision(writeEntry = false, writeLimitMarker = true),
            gate.normal(currentBytes = 9, entryBytes = 2),
        )
        assertEquals(
            LogWriteDecision(writeEntry = false, writeLimitMarker = false),
            gate.normal(currentBytes = 9, entryBytes = 1),
        )
        assertTrue(gate.footer().writeEntry)
    }

    @Test
    fun sizeGateMarksExactLimitAfterWritingEntry() {
        val gate = LogSizeGate(maxBytes = 10)

        assertEquals(
            LogWriteDecision(writeEntry = true, writeLimitMarker = true),
            gate.normal(currentBytes = 4, entryBytes = 6),
        )
    }
}
