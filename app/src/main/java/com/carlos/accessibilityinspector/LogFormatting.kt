package com.carlos.accessibilityinspector

import java.security.MessageDigest

data class NodeInterest(
    val clickable: Boolean = false,
    val editable: Boolean = false,
    val scrollable: Boolean = false,
    val text: String? = null,
    val hint: String? = null,
    val contentDescription: String? = null,
    val viewId: String? = null,
    val actionIds: Set<Int> = emptySet(),
)

object LogFormatting {
    const val REDACTED = "<REDACTED_PASSWORD_FIELD>"

    private val actionNames = mapOf(
        1 to "ACTION_FOCUS",
        2 to "ACTION_CLEAR_FOCUS",
        4 to "ACTION_SELECT",
        8 to "ACTION_CLEAR_SELECTION",
        16 to "ACTION_CLICK",
        32 to "ACTION_LONG_CLICK",
        64 to "ACTION_ACCESSIBILITY_FOCUS",
        128 to "ACTION_CLEAR_ACCESSIBILITY_FOCUS",
        256 to "ACTION_NEXT_AT_MOVEMENT_GRANULARITY",
        512 to "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY",
        1_024 to "ACTION_NEXT_HTML_ELEMENT",
        2_048 to "ACTION_PREVIOUS_HTML_ELEMENT",
        4_096 to "ACTION_SCROLL_FORWARD",
        8_192 to "ACTION_SCROLL_BACKWARD",
        16_384 to "ACTION_COPY",
        32_768 to "ACTION_PASTE",
        65_536 to "ACTION_CUT",
        131_072 to "ACTION_SET_SELECTION",
        262_144 to "ACTION_EXPAND",
        524_288 to "ACTION_COLLAPSE",
        1_048_576 to "ACTION_DISMISS",
        2_097_152 to "ACTION_SET_TEXT",
        16_777_216 to "ACTION_SHOW_ON_SCREEN",
        33_554_432 to "ACTION_SCROLL_TO_POSITION",
        67_108_864 to "ACTION_SCROLL_UP",
        134_217_728 to "ACTION_SCROLL_LEFT",
        268_435_456 to "ACTION_SCROLL_DOWN",
        536_870_912 to "ACTION_SCROLL_RIGHT",
        1_073_741_824 to "ACTION_CONTEXT_CLICK",
        16908342 to "ACTION_SET_PROGRESS",
        16908344 to "ACTION_MOVE_WINDOW",
        16908354 to "ACTION_PAGE_UP",
        16908355 to "ACTION_PAGE_DOWN",
        16908356 to "ACTION_PAGE_LEFT",
        16908357 to "ACTION_PAGE_RIGHT",
        16908372 to "ACTION_PRESS_AND_HOLD",
        16908373 to "ACTION_IME_ENTER",
        16908376 to "ACTION_DRAG_START",
        16908377 to "ACTION_DRAG_DROP",
        16908378 to "ACTION_DRAG_CANCEL",
        16908382 to "ACTION_SCROLL_IN_DIRECTION",
    )

    fun safeText(isPassword: Boolean, value: CharSequence?): String? =
        if (isPassword) REDACTED else value?.toString()

    fun quote(value: CharSequence?): String {
        if (value == null) return "null"
        val escaped = buildString {
            value.forEach { character ->
                append(
                    when (character) {
                        '\\' -> "\\\\"
                        '"' -> "\\\""
                        '\n' -> "\\n"
                        '\r' -> "\\r"
                        '\t' -> "\\t"
                        else -> character
                    },
                )
            }
        }
        return "\"$escaped\""
    }

    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun isInteresting(node: NodeInterest): Boolean =
        node.clickable ||
            node.editable ||
            node.scrollable ||
            !node.text.isNullOrBlank() ||
            !node.hint.isNullOrBlank() ||
            !node.contentDescription.isNullOrBlank() ||
            !node.viewId.isNullOrBlank() ||
            16 in node.actionIds ||
            2_097_152 in node.actionIds

    fun actionName(id: Int): String = "${actionNames[id] ?: "ACTION_UNKNOWN"}($id)"
}
