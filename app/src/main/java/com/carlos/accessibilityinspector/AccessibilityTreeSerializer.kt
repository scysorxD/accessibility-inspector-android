@file:Suppress("DEPRECATION")

package com.carlos.accessibilityinspector

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

data class TreeSnapshot(
    val fullText: String,
    val interestingNodesText: String,
    val hash: String,
)

class AccessibilityTreeSerializer(
    private val maxDepth: Int = 50,
) {
    fun serialize(root: AccessibilityNodeInfo): TreeSnapshot {
        val full = StringBuilder("--- ACCESSIBILITY TREE ---\n")
        val interesting = StringBuilder("--- INTERESTING NODES ---\n")
        traverse(root, depth = 0, path = "0", full = full, interesting = interesting)
        full.append("--- END ACCESSIBILITY TREE ---\n")
        interesting.append("--- END INTERESTING NODES ---\n")
        val fullText = full.toString()
        return TreeSnapshot(
            fullText = fullText,
            interestingNodesText = interesting.toString(),
            hash = LogFormatting.sha256(fullText),
        )
    }

    private fun traverse(
        node: AccessibilityNodeInfo,
        depth: Int,
        path: String,
        full: StringBuilder,
        interesting: StringBuilder,
    ) {
        if (depth > maxDepth) {
            full.append("  ".repeat(maxDepth + 1))
                .append("[$path] DEPTH_LIMIT_REACHED maxDepth=$maxDepth\n")
            return
        }

        val snapshot = readNode(node, path) ?: run {
            full.append("  ".repeat(depth)).append("[$path] STALE_NODE\n")
            return
        }
        appendFullNode(full, snapshot, depth)
        if (LogFormatting.isInteresting(snapshot.interest)) {
            appendInterestingNode(interesting, snapshot)
        }

        if (depth == maxDepth) {
            if (snapshot.childCount > 0) {
                full.append("  ".repeat(depth + 1))
                    .append("[$path/…] DEPTH_LIMIT_REACHED maxDepth=$maxDepth\n")
            }
            return
        }

        for (index in 0 until snapshot.childCount) {
            val child = safely { node.getChild(index) } ?: continue
            try {
                traverse(child, depth + 1, "$path/$index", full, interesting)
            } finally {
                if (Build.VERSION.SDK_INT < 33) safely { child.recycle() }
            }
        }
    }

    private fun readNode(node: AccessibilityNodeInfo, path: String): NodeSnapshot? = try {
        val password = node.isPassword
        val boundsInScreen = Rect().also(node::getBoundsInScreen)
        val boundsInParent = Rect().also(node::getBoundsInParent)
        val actions = node.actionList.orEmpty().map { it.id }.distinct()
        val text = LogFormatting.safeText(password, node.text)
        val hint = LogFormatting.safeText(password, node.hintText)
        val description = LogFormatting.safeText(password, node.contentDescription)
        val state = if (Build.VERSION.SDK_INT >= 30) {
            LogFormatting.safeText(password, node.stateDescription)
        } else {
            null
        }
        val error = LogFormatting.safeText(password, node.error)

        NodeSnapshot(
            path = path,
            packageName = node.packageName?.toString(),
            className = node.className?.toString(),
            viewId = node.viewIdResourceName,
            text = text,
            hint = hint,
            contentDescription = description,
            stateDescription = state,
            paneTitle = if (Build.VERSION.SDK_INT >= 28) {
                LogFormatting.safeText(password, node.paneTitle)
            } else {
                null
            },
            tooltipText = if (Build.VERSION.SDK_INT >= 28) {
                LogFormatting.safeText(password, node.tooltipText)
            } else {
                null
            },
            errorText = error,
            boundsInScreen = boundsInScreen.toShortString(),
            boundsInParent = boundsInParent.toShortString(),
            clickable = node.isClickable,
            longClickable = node.isLongClickable,
            focusable = node.isFocusable,
            focused = node.isFocused,
            accessibilityFocused = node.isAccessibilityFocused,
            enabled = node.isEnabled,
            selected = node.isSelected,
            checkable = node.isCheckable,
            checked = node.isChecked,
            editable = node.isEditable,
            scrollable = node.isScrollable,
            password = password,
            visibleToUser = node.isVisibleToUser,
            importantForAccessibility = node.isImportantForAccessibility,
            dismissable = node.isDismissable,
            heading = Build.VERSION.SDK_INT >= 28 && node.isHeading,
            screenReaderFocusable = Build.VERSION.SDK_INT >= 28 && node.isScreenReaderFocusable,
            textSelectable = Build.VERSION.SDK_INT >= 33 && node.isTextSelectable,
            actions = actions,
            childCount = node.childCount,
        )
    } catch (_: RuntimeException) {
        null
    }

    private fun appendFullNode(output: StringBuilder, node: NodeSnapshot, depth: Int) {
        val indent = "  ".repeat(depth)
        output.append(indent).append('[').append(node.path).append("]\n")
        with(node) {
            output.line(indent, "package", packageName)
            output.line(indent, "class", className)
            output.line(indent, "viewId", viewId)
            output.line(indent, "text", text)
            output.line(indent, "hintText", hint)
            output.line(indent, "contentDescription", contentDescription)
            output.line(indent, "stateDescription", stateDescription)
            output.line(indent, "paneTitle", paneTitle)
            output.line(indent, "tooltipText", tooltipText)
            output.line(indent, "errorText", errorText)
            output.append(indent).append("  boundsInScreen=").append(boundsInScreen).append('\n')
            output.append(indent).append("  boundsInParent=").append(boundsInParent).append('\n')
            output.append(indent).append("  clickable=").append(clickable)
                .append(" longClickable=").append(longClickable)
                .append(" focusable=").append(focusable)
                .append(" focused=").append(focused)
                .append(" accessibilityFocused=").append(accessibilityFocused).append('\n')
            output.append(indent).append("  enabled=").append(enabled)
                .append(" selected=").append(selected)
                .append(" checkable=").append(checkable)
                .append(" checked=").append(checked)
                .append(" editable=").append(editable)
                .append(" scrollable=").append(scrollable).append('\n')
            output.append(indent).append("  password=").append(password)
                .append(" visibleToUser=").append(visibleToUser)
                .append(" importantForAccessibility=").append(importantForAccessibility)
                .append(" dismissable=").append(dismissable).append('\n')
            output.append(indent).append("  heading=").append(heading)
                .append(" screenReaderFocusable=").append(screenReaderFocusable)
                .append(" textSelectable=").append(textSelectable).append('\n')
            output.append(indent).append("  actions=")
                .append(actions.joinToString(prefix = "[", postfix = "]", transform = LogFormatting::actionName))
                .append(" childCount=").append(childCount).append("\n\n")
        }
    }

    private fun appendInterestingNode(output: StringBuilder, node: NodeSnapshot) {
        output.append("path=[").append(node.path).append("]\n")
        output.append("class=").append(LogFormatting.quote(node.className)).append('\n')
        output.append("viewId=").append(LogFormatting.quote(node.viewId)).append('\n')
        output.append("text=").append(LogFormatting.quote(node.text)).append('\n')
        output.append("hint=").append(LogFormatting.quote(node.hint)).append('\n')
        output.append("contentDescription=")
            .append(LogFormatting.quote(node.contentDescription)).append('\n')
        output.append("clickable=").append(node.clickable)
            .append(" editable=").append(node.editable)
            .append(" scrollable=").append(node.scrollable).append('\n')
        output.append("bounds=").append(node.boundsInScreen).append('\n')
        output.append("actions=")
            .append(node.actions.joinToString(prefix = "[", postfix = "]", transform = LogFormatting::actionName))
            .append("\n\n")
    }

    private fun StringBuilder.line(indent: String, name: String, value: String?) {
        append(indent).append("  ").append(name).append('=')
            .append(LogFormatting.quote(value)).append('\n')
    }

    private inline fun <T> safely(block: () -> T): T? = try {
        block()
    } catch (_: RuntimeException) {
        null
    }

    private data class NodeSnapshot(
        val path: String,
        val packageName: String?,
        val className: String?,
        val viewId: String?,
        val text: String?,
        val hint: String?,
        val contentDescription: String?,
        val stateDescription: String?,
        val paneTitle: String?,
        val tooltipText: String?,
        val errorText: String?,
        val boundsInScreen: String,
        val boundsInParent: String,
        val clickable: Boolean,
        val longClickable: Boolean,
        val focusable: Boolean,
        val focused: Boolean,
        val accessibilityFocused: Boolean,
        val enabled: Boolean,
        val selected: Boolean,
        val checkable: Boolean,
        val checked: Boolean,
        val editable: Boolean,
        val scrollable: Boolean,
        val password: Boolean,
        val visibleToUser: Boolean,
        val importantForAccessibility: Boolean,
        val dismissable: Boolean,
        val heading: Boolean,
        val screenReaderFocusable: Boolean,
        val textSelectable: Boolean,
        val actions: List<Int>,
        val childCount: Int,
    ) {
        val interest = NodeInterest(
            clickable = clickable,
            editable = editable,
            scrollable = scrollable,
            text = text,
            hint = hint,
            contentDescription = contentDescription,
            viewId = viewId,
            actionIds = actions.toSet(),
        )
    }
}
