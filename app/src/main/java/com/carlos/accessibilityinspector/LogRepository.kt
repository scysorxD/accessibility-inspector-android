package com.carlos.accessibilityinspector

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object SessionRetention {
    private val validName =
        Regex("""accessibility_inspector_\d{4}-\d{2}-\d{2}_\d{6}(?:_\d+)?\.txt""")

    fun filesToDelete(names: List<String>, limit: Int): List<String> {
        val valid = names.filter(validName::matches).sorted()
        if (limit <= 0) return emptyList()
        return valid.take((valid.size - limit).coerceAtLeast(0))
    }

    fun latest(names: List<String>): String? =
        names.filter(validName::matches).maxOrNull()
}

data class SessionToken(
    val id: Long,
    val file: File,
)

class LogRepository private constructor(
    private val context: Context,
    private val preferences: InspectorPreferences,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val logsDirectory = File(context.filesDir, LOGS_DIRECTORY)
    private val sessionLock = Any()
    private val sessionGate = SessionGate()
    private var activeSession: ActiveSession? = null

    fun startSession(mode: CaptureMode, selectedPackage: String): SessionToken =
        synchronized(sessionLock) {
            check(activeSession == null) { "A capture session is already active" }
            check(logsDirectory.isDirectory || logsDirectory.mkdirs()) {
                "Could not create the private log directory"
            }

            val file = uniqueSessionFile()
            check(file.createNewFile()) { "Could not create the session log file" }
            val id = sessionGate.begin()
            val session = ActiveSession(
                id = id,
                file = file,
                sizeGate = LogSizeGate(MAX_LOG_BYTES),
            )
            try {
                val header = normalized(sessionHeader(mode, selectedPackage))
                executor.submit {
                    file.appendText(header, Charsets.UTF_8)
                    session.bytesWritten = header.toByteArray(Charsets.UTF_8).size.toLong()
                }.get()
            } catch (error: Exception) {
                sessionGate.deactivate()
                file.delete()
                throw error
            }

            activeSession = session
            preferences.latestLogPath = file.absolutePath
            trimHistory()
            SessionToken(id, file)
        }

    fun activeSessionId(): Long? = synchronized(sessionLock) { activeSession?.id }

    fun isSessionActive(expectedSessionId: Long): Boolean =
        synchronized(sessionLock) {
            activeSession?.id == expectedSessionId && sessionGate.accepts(expectedSessionId)
        }

    fun append(text: String): Boolean {
        val sessionId = activeSessionId() ?: return false
        return append(sessionId, text)
    }

    fun append(expectedSessionId: Long, text: String): Boolean =
        synchronized(sessionLock) {
            val session = activeSession
            if (
                session == null ||
                session.id != expectedSessionId ||
                !sessionGate.accepts(expectedSessionId)
            ) {
                return@synchronized false
            }
            val entry = normalized(text)
            executor.submit { writeNormal(session, entry) }
            true
        }

    fun stopSession(): Boolean {
        val completion = synchronized(sessionLock) {
            val session = activeSession ?: return false
            activeSession = null
            check(sessionGate.deactivate() == session.id)
            val footer = normalized(
                "\n============================================================\n" +
                    "SESSION ENDED\n" +
                    "Session stopped: ${timestamp()}\n" +
                    "============================================================\n",
            )
            executor.submit { writeFooter(session, footer) }
        }
        completion.get()
        return true
    }

    fun clearLogs() {
        synchronized(sessionLock) {
            activeSession = null
            sessionGate.deactivate()
            executor.submit {}.get()
            logsDirectory.listFiles()
                .orEmpty()
                .filter(::isInspectorLog)
                .forEach(File::delete)
            preferences.latestLogPath = ""
        }
    }

    fun latestLog(): File? {
        val persisted = preferences.latestLogPath.takeIf(String::isNotBlank)?.let(::File)
        if (persisted?.isFile == true && isInspectorLog(persisted)) return persisted
        val latestName = SessionRetention.latest(logsDirectory.list().orEmpty().toList()) ?: return null
        return File(logsDirectory, latestName).takeIf(File::isFile)
    }

    fun currentSize(): Long = latestLog()?.length() ?: 0L

    fun shutdown() {
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun writeNormal(session: ActiveSession, entry: String) {
        val entryBytes = entry.toByteArray(Charsets.UTF_8)
        val decision = session.sizeGate.normal(session.bytesWritten, entryBytes.size.toLong())
        if (decision.writeEntry && runCatching {
                session.file.appendBytes(entryBytes)
            }.isSuccess
        ) {
            session.bytesWritten += entryBytes.size
        }
        if (decision.writeLimitMarker) {
            val markerBytes = LOG_SIZE_LIMIT_MARKER.toByteArray(Charsets.UTF_8)
            if (runCatching { session.file.appendBytes(markerBytes) }.isSuccess) {
                session.bytesWritten += markerBytes.size
            }
        }
    }

    private fun writeFooter(session: ActiveSession, footer: String) {
        check(session.sizeGate.footer().writeEntry)
        val bytes = footer.toByteArray(Charsets.UTF_8)
        if (runCatching { session.file.appendBytes(bytes) }.isSuccess) {
            session.bytesWritten += bytes.size
        }
    }

    private fun uniqueSessionFile(): File {
        val base = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").format(OffsetDateTime.now())
        var candidate = File(logsDirectory, "accessibility_inspector_$base.txt")
        var suffix = 1
        while (candidate.exists()) {
            candidate = File(logsDirectory, "accessibility_inspector_${base}_$suffix.txt")
            suffix++
        }
        return candidate
    }

    private fun trimHistory() {
        SessionRetention.filesToDelete(
            logsDirectory.list().orEmpty().toList(),
            limit = MAX_SESSIONS,
        ).forEach { File(logsDirectory, it).delete() }
    }

    private fun isInspectorLog(file: File): Boolean =
        file.parentFile == logsDirectory &&
            file.extension == "txt" &&
            file.name.startsWith("accessibility_inspector_")

    private fun sessionHeader(mode: CaptureMode, selectedPackage: String): String {
        val metrics = context.resources.displayMetrics
        val packageFilter = when (mode) {
            CaptureMode.ALL_APPS -> "All apps"
            CaptureMode.CABIFY_ONLY -> "Cabify only: ${selectedPackage.ifBlank { "<not configured>" }}"
        }
        return """
            |CABIFY ACCESSIBILITY INSPECTOR
            |============================================================
            |Session started: ${timestamp()}
            |App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
            |Android version: ${Build.VERSION.RELEASE}
            |API level: ${Build.VERSION.SDK_INT}
            |Device manufacturer: ${Build.MANUFACTURER}
            |Device model: ${Build.MODEL}
            |Screen resolution: ${metrics.widthPixels}x${metrics.heightPixels}
            |Display density: ${metrics.densityDpi} dpi
            |Selected package filter: $packageFilter
            |Privacy: local app-private log; shared only by explicit user action
            |============================================================
            |
        """.trimMargin()
    }

    private fun normalized(text: String): String = if (text.endsWith('\n')) text else "$text\n"

    private data class ActiveSession(
        val id: Long,
        val file: File,
        val sizeGate: LogSizeGate,
        var bytesWritten: Long = 0,
    )

    companion object {
        private const val LOGS_DIRECTORY = "accessibility_logs"
        private const val MAX_SESSIONS = 10
        private const val MAX_LOG_BYTES = 25L * 1024L * 1024L
        private const val LOG_SIZE_LIMIT_MARKER = "LOG_SIZE_LIMIT_REACHED\n"
        private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: LogRepository? = null

        fun get(context: Context): LogRepository =
            instance ?: synchronized(this) {
                instance ?: LogRepository(
                    context.applicationContext,
                    InspectorPreferences(context.applicationContext),
                ).also { instance = it }
            }

        fun timestamp(): String = timestampFormatter.format(OffsetDateTime.now())
    }
}
