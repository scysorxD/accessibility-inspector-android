package com.carlos.accessibilityinspector

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

object AccessibilityUtils {
    fun isInspectorServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val expected = ComponentName(context, InspectorAccessibilityService::class.java)
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val service = info.resolveInfo?.serviceInfo ?: return@any false
                ComponentName(service.packageName, service.name) == expected
            }
    }

    fun formatEvent(event: AccessibilityEvent): String {
        val password = event.isPassword
        val eventText = if (password) {
            LogFormatting.REDACTED
        } else {
            event.text.joinToString(
                prefix = "[",
                postfix = "]",
                transform = LogFormatting::quote,
            )
        }
        val description = LogFormatting.safeText(password, event.contentDescription)
        return """
            |
            |============================================================
            |EVENT
            |timestamp: ${LogRepository.timestamp()}
            |eventType: ${AccessibilityEvent.eventTypeToString(event.eventType)}
            |packageName: ${event.packageName}
            |className: ${event.className}
            |windowId: ${event.windowId}
            |contentChangeTypes: ${event.contentChangeTypes}
            |text: $eventText
            |contentDescription: ${LogFormatting.quote(description)}
            |password: $password
            |============================================================
        """.trimMargin()
    }
}
