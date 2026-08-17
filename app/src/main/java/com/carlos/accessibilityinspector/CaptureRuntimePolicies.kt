package com.carlos.accessibilityinspector

class BoundedDebouncePolicy(
    private val quietDelayMillis: Long,
    private val maxDelayMillis: Long,
) {
    private var burstStartedAtMillis: Long? = null

    init {
        require(quietDelayMillis > 0)
        require(maxDelayMillis >= quietDelayMillis)
    }

    fun delayAfterEvent(nowMillis: Long): Long {
        val burstStart = burstStartedAtMillis ?: nowMillis.also {
            burstStartedAtMillis = it
        }
        val quietDeadline = nowMillis + quietDelayMillis
        val maximumDeadline = burstStart + maxDelayMillis
        return (minOf(quietDeadline, maximumDeadline) - nowMillis).coerceAtLeast(0)
    }

    fun onSnapshotTriggered() {
        burstStartedAtMillis = null
    }
}

class SessionGate {
    private var nextId = 1L
    private var activeId: Long? = null

    @Synchronized
    fun begin(): Long = nextId++.also { activeId = it }

    @Synchronized
    fun current(): Long? = activeId

    @Synchronized
    fun accepts(expectedId: Long): Boolean = activeId == expectedId

    @Synchronized
    fun deactivate(): Long? = activeId.also { activeId = null }
}

data class LogWriteDecision(
    val writeEntry: Boolean,
    val writeLimitMarker: Boolean = false,
)

class LogSizeGate(
    private val maxBytes: Long,
) {
    private var limitMarkerWritten = false

    init {
        require(maxBytes > 0)
    }

    fun normal(currentBytes: Long, entryBytes: Long): LogWriteDecision {
        if (limitMarkerWritten) return LogWriteDecision(writeEntry = false)
        val resultingBytes = currentBytes + entryBytes
        return when {
            resultingBytes < maxBytes -> LogWriteDecision(writeEntry = true)
            resultingBytes == maxBytes -> {
                limitMarkerWritten = true
                LogWriteDecision(writeEntry = true, writeLimitMarker = true)
            }
            else -> {
                limitMarkerWritten = true
                LogWriteDecision(writeEntry = false, writeLimitMarker = true)
            }
        }
    }

    fun footer(): LogWriteDecision = LogWriteDecision(writeEntry = true)
}
