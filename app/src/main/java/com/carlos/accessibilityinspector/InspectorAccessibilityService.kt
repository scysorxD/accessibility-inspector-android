@file:Suppress("DEPRECATION")

package com.carlos.accessibilityinspector

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InspectorAccessibilityService : AccessibilityService() {
    private lateinit var preferences: InspectorPreferences
    private lateinit var repository: LogRepository
    private val serializer = AccessibilityTreeSerializer()
    private val handler = Handler(Looper.getMainLooper())
    private val snapshotExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val debouncePolicy = BoundedDebouncePolicy(
        quietDelayMillis = SNAPSHOT_QUIET_DELAY_MS,
        maxDelayMillis = SNAPSHOT_MAX_DELAY_MS,
    )
    private var previousTreeHash: String? = null
    private var hashSessionId: Long? = null
    private var scheduledSessionId: Long? = null

    private val snapshotRunnable = Runnable {
        val expectedSessionId = scheduledSessionId
        scheduledSessionId = null
        debouncePolicy.onSnapshotTriggered()
        if (expectedSessionId != null) {
            snapshotExecutor.execute { captureSnapshot(expectedSessionId) }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferences = InspectorPreferences(applicationContext)
        repository = LogRepository.get(applicationContext)
        if (preferences.captureEnabled && repository.activeSessionId() == null) {
            preferences.captureEnabled = false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::preferences.isInitialized || !preferences.captureEnabled) return
        val sessionId = repository.activeSessionId() ?: return

        val eventPackage = event.packageName?.toString().orEmpty()
        if (
            preferences.captureMode == CaptureMode.ALL_APPS &&
            eventPackage.isNotBlank() &&
            eventPackage != packageName &&
            eventPackage != preferences.lastObservedPackage
        ) {
            preferences.lastObservedPackage = eventPackage
        }
        if (
            !CapturePolicy.accepts(
                preferences.captureMode,
                preferences.selectedPackage,
                eventPackage,
            )
        ) {
            return
        }

        repository.append(sessionId, AccessibilityUtils.formatEvent(event))
        if (event.eventType in SNAPSHOT_EVENT_TYPES) {
            scheduledSessionId = sessionId
            val delay = debouncePolicy.delayAfterEvent(SystemClock.uptimeMillis())
            handler.removeCallbacks(snapshotRunnable)
            handler.postDelayed(snapshotRunnable, delay)
        }
    }

    private fun captureSnapshot(expectedSessionId: Long) {
        if (!repository.isSessionActive(expectedSessionId)) return
        val root = rootInActiveWindow
        if (root == null) {
            repository.append(
                expectedSessionId,
                "ROOT_NULL timestamp=${LogRepository.timestamp()}",
            )
            return
        }

        try {
            val rootPackage = runCatching {
                root.packageName?.toString().orEmpty()
            }.getOrDefault("")
            if (
                !CapturePolicy.accepts(
                    preferences.captureMode,
                    preferences.selectedPackage,
                    rootPackage,
                )
            ) {
                return
            }
            if (expectedSessionId != hashSessionId) {
                previousTreeHash = null
                hashSessionId = expectedSessionId
            }
            val snapshot = serializer.serialize(root)
            if (snapshot.hash == previousTreeHash) {
                repository.append(
                    expectedSessionId,
                    "TREE_UNCHANGED timestamp=${LogRepository.timestamp()} hash=${snapshot.hash}",
                )
            } else {
                val accepted = repository.append(
                    expectedSessionId,
                    "\nTREE_SNAPSHOT timestamp=${LogRepository.timestamp()} hash=${snapshot.hash}\n" +
                        snapshot.fullText +
                        snapshot.interestingNodesText,
                )
                if (accepted) previousTreeHash = snapshot.hash
            }
        } catch (error: RuntimeException) {
            repository.append(
                expectedSessionId,
                    "TREE_SERIALIZATION_ERROR timestamp=${LogRepository.timestamp()} " +
                        "type=${error.javaClass.simpleName}",
            )
        } finally {
            if (Build.VERSION.SDK_INT < 33) runCatching { root.recycle() }
        }
    }

    override fun onInterrupt() {
        if (::repository.isInitialized) {
            repository.activeSessionId()?.let { sessionId ->
                repository.append(
                    sessionId,
                    "SERVICE_INTERRUPTED timestamp=${LogRepository.timestamp()}",
                )
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(snapshotRunnable)
        snapshotExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val SNAPSHOT_QUIET_DELAY_MS = 750L
        private const val SNAPSHOT_MAX_DELAY_MS = 3_000L
        private val SNAPSHOT_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
        )
    }
}
