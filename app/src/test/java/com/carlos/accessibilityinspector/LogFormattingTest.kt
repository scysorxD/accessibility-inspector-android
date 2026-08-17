package com.carlos.accessibilityinspector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogFormattingTest {
    @Test
    fun passwordFieldsAlwaysRedactText() {
        assertEquals(
            "<REDACTED_PASSWORD_FIELD>",
            LogFormatting.safeText(true, "secret"),
        )
        assertEquals(
            "<REDACTED_PASSWORD_FIELD>",
            LogFormatting.safeText(true, null),
        )
    }

    @Test
    fun readableTextEscapesControlCharactersAndQuotes() {
        assertEquals("\"line\\n\\\"quoted\\\"\\tend\"", LogFormatting.quote("line\n\"quoted\"\tend"))
        assertEquals("null", LogFormatting.quote(null))
    }

    @Test
    fun sha256IsDeterministicAndLowercase() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            LogFormatting.sha256("abc"),
        )
    }

    @Test
    fun interestingNodeCriteriaCoverSemanticAndActionableContent() {
        assertFalse(LogFormatting.isInteresting(NodeInterest()))
        assertTrue(LogFormatting.isInteresting(NodeInterest(clickable = true)))
        assertTrue(LogFormatting.isInteresting(NodeInterest(text = "Destination")))
        assertTrue(LogFormatting.isInteresting(NodeInterest(actionIds = setOf(2_097_152))))
    }

    @Test
    fun knownActionsHaveReadableNames() {
        assertEquals("ACTION_CLICK(16)", LogFormatting.actionName(16))
        assertEquals("ACTION_SET_TEXT(2097152)", LogFormatting.actionName(2_097_152))
        assertEquals("ACTION_UNKNOWN(123456)", LogFormatting.actionName(123_456))
    }
}
