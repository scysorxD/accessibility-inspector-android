# Accessibility Inspector Design

## Decision

Build one native Android application module with Kotlin, Jetpack Compose, and an
`AccessibilityService`. Use Android Gradle Plugin 9.3, Gradle 9.5, compile/target
SDK 37, and minimum SDK 26. The app has no network permission, backend, database,
automation, screenshots, OCR, or third-party runtime libraries.

Two alternatives were considered:

1. A single activity/service implementation. This minimizes files but tangles UI,
   accessibility traversal, persistence, and sharing, making failures harder to
   isolate.
2. A small set of focused helpers around one activity and one service. This keeps
   the POC small while making serialization and logging independently testable.
3. A layered architecture with dependency injection and use cases. This adds
   complexity without improving this diagnostic experiment.

Option 2 is selected.

## Components

- `MainActivity` renders one Compose screen, reports the actual enabled state of
  the service, opens Android Accessibility Settings, controls capture, selects
  the package filter, and clears or shares logs.
- `InspectorAccessibilityService` receives only the configured event types,
  updates the observed package, records matching event metadata, and schedules
  debounced snapshots. It never performs node actions or gestures.
- `AccessibilityTreeSerializer` safely traverses at most 50 levels, preserves
  indentation and node paths, redacts password text, includes readable action
  names, creates the interesting-node section, and hashes the stable snapshot.
- `LogRepository` owns session files, serializes all writes on one background
  executor, maintains the current/latest log, and retains at most ten sessions.
- `InspectorPreferences` stores capture state, capture mode, selected Cabify
  package, last observed package, and latest log path in app-private preferences.
- `AccessibilityUtils` formats events and checks the real service state.
- `ShareUtils` exposes only app-owned logs through `FileProvider`.

## Data flow

Starting capture creates a timestamped app-private text file, writes device and
filter diagnostics, then enables event acceptance. The service copies event
metadata into immutable text and queues file writes. Relevant window/content
events schedule one snapshot after 750 ms; repeated requests replace the pending
snapshot. The serializer reads `rootInActiveWindow`, creates full and interesting
representations, and compares its SHA-256 hash with the previous snapshot.
Unchanged trees produce a short `TREE_UNCHANGED` entry.

Stopping capture first disables event acceptance and then queues the session
footer. Sharing resolves the current or newest session and launches the Android
Sharesheet with a temporary read grant. Clearing removes only inspector `.txt`
files after explicit confirmation.

## Reliability and privacy

All node access is defensive and catches stale-node runtime failures. Null roots
are logged without crashing. File writes run off the accessibility callback
thread. Password node text, hint, content description, state description, and
error text are replaced with `<REDACTED_PASSWORD_FIELD>`. No hardware identifiers
or network capability are collected.

## Testing

Local unit tests cover capture filtering, password redaction, event/tree
formatting helpers, stable hashing, and session retention behavior where it can
be isolated from Android framework objects. Android lint and a debug APK build
validate resources, manifest declarations, Compose compilation, and
`FileProvider` configuration. Enabling the service, inspecting real trees, and
opening the Sharesheet require an emulator or physical device and remain a
documented manual checklist.

## Self-review

The design has no placeholders, preserves the strict inspector-only scope, maps
all required deliverables to focused components, and makes explicit that
Cabify's accessibility quality cannot be determined until device testing.
