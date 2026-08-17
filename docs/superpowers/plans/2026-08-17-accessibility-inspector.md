# Accessibility Inspector Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an installable Android diagnostic app that records readable, safe accessibility event and tree logs and shares them explicitly.

**Architecture:** One Android application module contains a Compose activity and an `AccessibilityService`, supported by small helpers for preferences, serialization, app-private session files, service-state checks, and `FileProvider` sharing. Accessibility callbacks only copy/serialize platform data; all disk writes are serialized on a background executor.

**Tech Stack:** Kotlin 2.3.21, Android Gradle Plugin 9.3.0, Gradle 9.5.0, Jetpack Compose BOM 2026.06.01, AndroidX, JUnit 4.

## Global Constraints

- Minimum SDK 26; compile and target SDK 37.
- No `INTERNET`, broad storage, screenshot, OCR, gesture, click, text-entry, HTTP, analytics, cloud, or automation functionality.
- Store only app-private `.txt` logs and expose them through `FileProvider`.
- Redact every textual field on password nodes as `<REDACTED_PASSWORD_FIELD>`.
- Limit tree traversal to depth 50, debounce snapshots by 750 ms, suppress consecutive duplicate trees, and retain ten sessions.

---

### Task 1: Android project and testable capture policy

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/test/java/com/carlos/accessibilityinspector/CapturePolicyTest.kt`
- Create: `app/src/main/java/com/carlos/accessibilityinspector/CapturePolicy.kt`

**Interfaces:**
- Produces: `enum class CaptureMode`; `CapturePolicy.accepts(mode, selectedPackage, eventPackage): Boolean`.

- [ ] Write tests proving all-app mode accepts any package, Cabify-only accepts only the selected package, and blank package configuration rejects safely.
- [ ] Run `./gradlew testDebugUnitTest` and confirm compilation fails because `CapturePolicy` does not exist.
- [ ] Add the minimal enum and pure filtering implementation.
- [ ] Run the focused tests and confirm they pass.
- [ ] Add the single-module Android/Compose build configuration and Gradle wrapper.

### Task 2: Safe formatting and tree snapshot model

**Files:**
- Create: `app/src/test/java/com/carlos/accessibilityinspector/LogFormattingTest.kt`
- Create: `app/src/main/java/com/carlos/accessibilityinspector/LogFormatting.kt`
- Create: `app/src/main/java/com/carlos/accessibilityinspector/AccessibilityTreeSerializer.kt`

**Interfaces:**
- Produces: `TreeSnapshot(fullText, interestingNodesText, hash)`; password redaction, text quoting, SHA-256, and action-name helpers.

- [ ] Write tests for password redaction, escaped readable text, deterministic SHA-256, interesting-node criteria, and known action names.
- [ ] Run the focused test and confirm the missing formatting API fails.
- [ ] Implement pure formatting helpers.
- [ ] Run tests and confirm green.
- [ ] Implement defensive `AccessibilityNodeInfo` traversal with paths, indentation, all specified properties, 50-level cap, readable actions, interesting-node output, and stable hash using the tested helpers.

### Task 3: Preferences and session log repository

**Files:**
- Create: `app/src/main/java/com/carlos/accessibilityinspector/InspectorPreferences.kt`
- Create: `app/src/main/java/com/carlos/accessibilityinspector/LogRepository.kt`

**Interfaces:**
- Produces: persisted capture/filter/package state and `startSession`, `append`, `stopSession`, `clearLogs`, `latestLog`, `currentSize`, and `shutdown`.

- [ ] Add tests for pure filename ordering/retention selection.
- [ ] Run tests and verify failure before implementation.
- [ ] Implement app-private timestamped sessions, diagnostic headers/footers, one-thread queued writes, latest path persistence, and deletion beyond ten files.
- [ ] Run unit tests and confirm green.

### Task 4: Accessibility service and Android declarations

**Files:**
- Create: `app/src/main/java/com/carlos/accessibilityinspector/InspectorAccessibilityService.kt`
- Create: `app/src/main/java/com/carlos/accessibilityinspector/AccessibilityUtils.kt`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Create: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: capture policy, preferences, serializer, and repository.
- Produces: configured event listener, actual enabled-state lookup, formatted event entries, and debounced tree dumps.

- [ ] Declare the service with `BIND_ACCESSIBILITY_SERVICE`, exported false, content retrieval enabled, and all required event types.
- [ ] Format immutable event metadata with ISO local timestamps.
- [ ] Persist recently observed package names in all-app mode.
- [ ] Filter events, queue event writes, and debounce relevant snapshots by 750 ms.
- [ ] Log `ROOT_NULL`, full/interesting trees, and `TREE_UNCHANGED` without node actions.
- [ ] Run unit tests and Android lint.

### Task 5: Compose controls, clear confirmation, and secure sharing

**Files:**
- Create: `app/src/main/java/com/carlos/accessibilityinspector/MainActivity.kt`
- Create: `app/src/main/java/com/carlos/accessibilityinspector/ShareUtils.kt`
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: preferences/repository and actual service state.
- Produces: one-screen UI and `ACTION_SEND` Sharesheet intent.

- [ ] Implement service status and Accessibility Settings navigation.
- [ ] Implement recording indicator, start/stop, all-app/Cabify-only selection, editable discovered package, last observed package, and current log size.
- [ ] Add clear confirmation that removes only inspector files.
- [ ] Add `FileProvider` and `text/plain` sharing with temporary read permission.
- [ ] Refresh actual enabled state on activity resume.
- [ ] Build the debug APK and fix all compile/resource errors.

### Task 6: Operator documentation and final verification

**Files:**
- Modify: `README.md`
- Create: `.gitignore`

**Interfaces:**
- Produces: reproducible Android Studio, command-line build, installation, permission, capture, sharing, and manual-validation instructions.

- [ ] Document prerequisites and exact Android Studio clicks from opening the folder through SDK sync and device selection.
- [ ] Document `gradlew.bat assembleDebug`, `adb install -r`, APK location, and explain that npm/npx are not used.
- [ ] Document phone steps for enabling the service, discovering Cabify's package, recording, stopping, clearing, and sharing.
- [ ] Document privacy/scope and a physical-device checklist without claiming Cabify results.
- [ ] Run `gradlew.bat testDebugUnitTest lintDebug assembleDebug`.
- [ ] Confirm the APK exists and inspect merged permissions to ensure `INTERNET` is absent.
- [ ] Compare implementation against every specification deliverable and report device-only validation honestly.

## Self-review

Every mandatory specification area maps to a task. Public helper names and data
types are consistent across tasks. The plan contains no deferred implementation
placeholders and retains the inspector-only restriction throughout.
