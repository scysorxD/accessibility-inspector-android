# Cabify Accessibility Inspector — Technical Specification for Cursor

## 1. Objective

Build a **small native Android diagnostic application** whose only purpose is to determine whether the Cabify Android application exposes enough useful information through Android Accessibility APIs to later build a robust Cabify automation agent.

This application is **NOT the Cabify automation app yet**.

For this first proof of concept, the app must:

1. Install on a physical Android phone.
2. Register an Android `AccessibilityService`.
3. Allow the user to enable that accessibility service from Android Settings.
4. Observe the currently active application/window.
5. When Cabify is opened and navigated manually, inspect the accessibility tree.
6. Log useful information from every accessibility node.
7. Save the diagnostic information into a human-readable text file.
8. Provide a normal Android **Share** button so the log can be shared through Gmail, Outlook, WhatsApp, Google Drive, etc.
9. Make it easy to clear the log and run a fresh test.
10. Do **not** perform clicks, gestures, text entry, screenshots, OCR, AI, HTTP requests, or Cabify automation in this version.

The result of this experiment will determine whether a future automation application can use accessibility nodes as its primary control mechanism, with computer vision/AI only as a fallback.

---

# 2. Development approach

Use a simple native Android application.

Preferred stack:

- Kotlin
- Android Studio compatible Gradle project
- Jetpack Compose for the small UI
- AndroidX
- Minimum SDK: choose a sensible modern minimum, preferably API 26+ unless there is a concrete reason otherwise
- Target/compile SDK: current stable SDK installed by the development environment
- No external backend
- No Firebase
- No analytics
- No cloud services
- No database needed
- No unnecessary third-party dependencies

Keep the project extremely small and understandable.

Suggested application name:

`Accessibility Inspector`

Suggested package name:

`com.carlos.accessibilityinspector`

The package name is not critical.

---

# 3. Important scope restriction

This version is strictly an **inspector/logger**.

DO NOT implement:

- `performAction(ACTION_CLICK)`
- gesture injection
- coordinate clicking
- automatic Cabify navigation
- automatic typing
- screenshots
- screen recording
- OCR
- computer vision
- LLM calls
- OpenAI calls
- AWS calls
- HTTP APIs
- WebSockets
- MQTT
- polling
- MacroDroid integration
- background remote control

We first need evidence of what Cabify exposes.

---

# 4. Accessibility service

Implement an Android `AccessibilityService`.

The service must be declared correctly in `AndroidManifest.xml` and configured using an XML accessibility-service configuration resource.

The configuration must allow retrieval of window content.

Required concept:

```xml
android:canRetrieveWindowContent="true"
```

Listen to useful accessibility events including at least:

- `TYPE_WINDOW_STATE_CHANGED`
- `TYPE_WINDOW_CONTENT_CHANGED`
- `TYPE_VIEW_CLICKED`
- `TYPE_VIEW_FOCUSED`
- `TYPE_VIEW_TEXT_CHANGED`
- `TYPE_VIEW_SCROLLED`
- `TYPE_WINDOWS_CHANGED` where supported/useful

Do not blindly dump the entire tree hundreds of times per second.

Implement sensible throttling/debouncing.

A suggested behavior:

- Always record the event itself.
- Dump the accessibility tree after important window/content events.
- Debounce repeated content-change tree dumps by roughly 500–1000 ms.
- Avoid writing exact duplicate tree snapshots consecutively.

The goal is useful diagnostic output, not maximum event volume.

---

# 5. Retrieve the active accessibility tree

On relevant events, retrieve:

```kotlin
rootInActiveWindow
```

Recursively traverse the resulting `AccessibilityNodeInfo` tree.

The traversal must be safe:

- handle `null`
- avoid crashes on stale nodes
- cap recursion depth at a safe value, e.g. 50
- catch expected platform exceptions when appropriate
- never block the accessibility callback for a long period
- move file-writing work off the main/accessibility callback thread if necessary

---

# 6. Information to log for each accessibility event

For every important accessibility event, log a header similar to:

```text
============================================================
EVENT
timestamp: 2026-08-16T14:52:31.123-03:00
eventType: TYPE_WINDOW_CONTENT_CHANGED
packageName: com.example
className: android.widget.FrameLayout
windowId: 42
contentChangeTypes: ...
text: [...]
contentDescription: ...
============================================================
```

Use an ISO-like local timestamp with milliseconds.

The exact format may differ, but it must be easy for a human to read.

---

# 7. Information to log for every accessibility node

For each node in the tree, log as much of the following information as Android exposes:

- depth
- child index / path within tree
- packageName
- className
- viewIdResourceName
- text
- hintText
- contentDescription
- stateDescription
- paneTitle where available
- tooltipText where available
- error text where available
- bounds in screen
- bounds in parent/window if useful
- clickable
- longClickable
- focusable
- focused
- accessibilityFocused
- enabled
- selected
- checkable
- checked
- editable
- scrollable
- password
- visibleToUser
- importantForAccessibility where available
- dismissable where available
- heading where available
- screenReaderFocusable where available
- text selectable where available
- actions supported by the node

For actions, output human-readable action names where possible, for example:

```text
actions=[ACTION_CLICK, ACTION_FOCUS, ACTION_SET_TEXT]
```

Also log the numeric action IDs if useful.

Example desired node representation:

```text
[0/2/1]
class=android.widget.EditText
package=com.cabify...
viewId=com.cabify...:id/search_destination
text=""
hintText="¿A dónde vas?"
contentDescription=null
bounds=[42,310][1038,430]
clickable=true
focusable=true
editable=true
enabled=true
visibleToUser=true
actions=[ACTION_CLICK, ACTION_FOCUS, ACTION_SET_TEXT]
```

A one-node-per-block or one-node-per-line format is acceptable.

Prefer readability over minimizing file size.

---

# 8. Tree structure must be preserved

Do not produce only a flat list.

The log must make parent/child hierarchy obvious.

Either use indentation:

```text
ROOT ...
  CHILD 0 ...
    CHILD 0 ...
    CHILD 1 ...
  CHILD 1 ...
```

or paths:

```text
[0]
[0/0]
[0/0/0]
[0/0/1]
[0/1]
```

Ideally use both indentation and path.

This will be important later when deciding whether the clickable parent of a text node can be reliably located.

---

# 9. Package/application filtering

Do **not** hardcode a presumed Cabify package name as the only allowed package.

The exact package should be discovered from actual accessibility events.

The UI should provide:

### Capture mode

- `All apps`
- `Cabify only`

For the first implementation, `Cabify only` can work using one of these mechanisms:

Preferred:

1. Observe packages while capture mode is `All apps`.
2. Show the most recently observed package name in the UI.
3. Allow the user to enter/select the Cabify package name.
4. Save that package name locally.
5. When `Cabify only` is enabled, ignore events belonging to other packages except Android system UI if needed for context.

Alternative acceptable behavior:

- Filter package names containing `cabify`, but still show the actual discovered package name.

Do not assume `com.cabify.rider` without verifying it on the device.

---

# 10. Capture controls

The main UI should contain approximately:

## Accessibility Inspector

Accessibility service:

`Enabled` / `Disabled`

Button:

`Open Accessibility Settings`

Capture:

`Stopped` / `Recording`

Buttons:

- `Start capture`
- `Stop capture`
- `Clear log`
- `Share log`

Optional but useful:

- `Preview log`
- `Copy last snapshot`
- `Show current log size`

Do not overdesign the UI.

A single screen is enough.

---

# 11. Start/Stop capture semantics

The accessibility service itself may remain enabled in Android Settings.

However, the application should have its own internal capture switch.

When `Start capture` is pressed:

- start accepting/writing events
- add a session header
- record start time
- record device/app diagnostic information

When `Stop capture` is pressed:

- stop writing new Cabify accessibility information
- add a session footer
- flush/close files as needed

This is important because the service can remain enabled while the user is not testing.

---

# 12. Session header

At the beginning of each capture, write something similar to:

```text
CABIFY ACCESSIBILITY INSPECTOR
Session started: ...
App version: ...
Android version: ...
API level: ...
Device manufacturer: ...
Device model: ...
Screen resolution: ...
Display density: ...
Selected package filter: ...
```

Do not collect hardware identifiers such as:

- IMEI
- Android advertising ID
- serial number
- phone number
- account information

They are irrelevant.

---

# 13. Logging behavior

Store logs in the application's private/app-specific files directory.

Suggested filename:

```text
accessibility_inspector_2026-08-16_145200.txt
```

Prefer one log per capture session.

If simpler, a single active log is acceptable, but one file per session is better.

Keep a small history, for example the latest 10 sessions.

Avoid infinite storage growth.

If more than 10 session logs exist, delete the oldest.

---

# 14. Share functionality

Implement a normal Android share action using `FileProvider`.

Do not expose `file://` paths.

Use a content URI with temporary read permission.

The `Share log` button should launch the standard Android Sharesheet.

Use MIME type:

```text
text/plain
```

Suggested share title:

```text
Share accessibility log
```

Suggested subject if supported:

```text
Cabify Accessibility Inspector Log
```

The log must be shareable to email applications so I can send it to myself and inspect it from a PC.

The app should never require broad storage permission merely to share its own log.

---

# 15. Clear log

`Clear log` should require a small confirmation dialog:

```text
Delete the current diagnostic log?
Cancel | Delete
```

It should delete only this app's diagnostic files.

---

# 16. Privacy/safety

This diagnostic app may encounter text displayed by other apps.

Therefore:

- Do not upload anything automatically.
- Do not send logs over the network.
- Do not include INTERNET permission unless something unexpectedly requires it. It should not be necessary.
- Store logs locally.
- The user explicitly chooses when to share a log.
- Clearly indicate when capture is active.
- Prefer filtering to Cabify once its package has been identified.

If accessibility events contain a password field (`isPassword == true`):

- DO NOT log its text.
- Replace its value with:

```text
<REDACTED_PASSWORD_FIELD>
```

Also redact obviously sensitive editable password content.

Do not add complicated generic PII filtering at this stage because the purpose is to inspect Cabify's labels and values, but password fields must always be redacted.

---

# 17. Duplicate suppression

Cabify may generate many nearly identical accessibility events.

Implement lightweight duplicate suppression.

For example:

- Generate a stable textual representation/hash of the tree snapshot.
- If the immediately previous snapshot hash is identical, do not write the entire tree again.
- Still allow a short event entry such as:

```text
TREE_UNCHANGED hash=...
```

This keeps the file readable.

Do not overengineer this.

---

# 18. Tree dump trigger strategy

Suggested algorithm:

```text
onAccessibilityEvent(event):

    if capture is not enabled:
        return

    log event metadata

    if package filter does not match:
        return

    if event type indicates relevant screen/window/content change:
        schedule debounced tree snapshot

debouncedSnapshot():

    root = rootInActiveWindow

    if root == null:
        log "ROOT_NULL"
        return

    tree = serializeTree(root)

    hash = sha256(tree)

    if hash != previousTreeHash:
        append tree
        previousTreeHash = hash
    else:
        append TREE_UNCHANGED
```

The exact implementation can differ.

---

# 19. Useful diagnostic feature: node summary

In addition to the full tree, produce a compact section containing only nodes that are likely to be actionable.

A node is "interesting" if one or more is true:

- `clickable == true`
- `editable == true`
- `scrollable == true`
- node has non-empty text
- node has non-empty hint text
- node has non-empty content description
- node has a non-empty view ID
- node exposes ACTION_CLICK
- node exposes ACTION_SET_TEXT

Example:

```text
--- INTERESTING NODES ---

path=[0/1/3]
text="Introducir ruta"
class=android.widget.TextView
viewId=...
clickable=true
bounds=[...]

path=[0/1/4]
hint="¿A dónde vas?"
editable=true
actions=[ACTION_SET_TEXT]
bounds=[...]

--- END INTERESTING NODES ---
```

This section will make analysis much faster.

Still keep the full tree.

---

# 20. Optional UI preview

If straightforward, add a log preview text area showing:

- last event
- active package
- last 10–30 interesting nodes

Do not render the entire multi-megabyte log in Compose.

The preview is optional.

---

# 21. Permission/setup UX

The app cannot silently grant itself Accessibility permission.

When the service is disabled:

- show `Accessibility service disabled`
- provide `Open Accessibility Settings`

The button should navigate the user to the relevant Android Accessibility Settings screen.

When returning to the app, detect whether the service has become enabled and update the UI.

Do not falsely show the service as enabled based only on a SharedPreference.

Read actual Android accessibility-service state.

---

# 22. Accessibility service description

The service description shown by Android should be clear, for example:

```text
Accessibility Inspector reads the active screen's accessibility hierarchy
and stores it locally for diagnostic testing. It does not send data over
the network or perform automatic actions.
```

---

# 23. Project structure

Keep the architecture simple.

A structure similar to the following is sufficient:

```text
app/
  src/main/
    java/com/carlos/accessibilityinspector/
      MainActivity.kt
      InspectorAccessibilityService.kt
      AccessibilityTreeSerializer.kt
      LogRepository.kt
      AccessibilityUtils.kt
      ShareUtils.kt
    res/
      xml/
        accessibility_service_config.xml
        file_paths.xml
      values/
        strings.xml
      AndroidManifest.xml
```

Names may change if there is a good reason.

Do not introduce Clean Architecture, repositories/use cases/domain layers, dependency injection frameworks, or multiple modules for this tiny diagnostic POC.

A small `LogRepository` helper is fine.

---

# 24. AccessibilityTreeSerializer responsibilities

Create a dedicated serializer/helper so traversal logic does not become tangled with the service.

Suggested API:

```kotlin
class AccessibilityTreeSerializer {
    fun serialize(root: AccessibilityNodeInfo): TreeSnapshot
}
```

Possible data result:

```kotlin
data class TreeSnapshot(
    val fullText: String,
    val interestingNodesText: String,
    val hash: String
)
```

Or equivalent.

The serializer should:

- recursively traverse children
- create hierarchy/path information
- safely read node properties
- produce readable action names
- redact password fields
- avoid crashes if a node disappears during traversal

---

# 25. What counts as a successful experiment

This POC is successful if, while manually navigating Cabify, the resulting log lets us answer questions such as:

1. What is Cabify's package name on this phone?
2. Does the destination search field appear in the accessibility tree?
3. Does it expose:
   - text?
   - hint text?
   - contentDescription?
   - viewIdResourceName?
4. Is the search field marked editable?
5. Does it expose `ACTION_SET_TEXT`?
6. Does the visible destination result appear as a node?
7. Is that node clickable, or does its parent expose `ACTION_CLICK`?
8. Does the estimated price appear as text?
9. Does the final confirmation button appear as text/contentDescription?
10. Does the driver-searching state appear as text?
11. Do driver details appear after assignment?
12. Are vehicle, plate/license plate, driver name, and ETA exposed?
13. Does Cabify mostly expose useful semantic nodes, or is the UI effectively opaque to accessibility?

That is the entire purpose of version 1.

---

# 26. Important test screens we want to inspect later

The app must produce enough diagnostics for these Cabify screens:

### A. Cabify home screen

We want to identify:

- route/search entry point
- any popup from a previous journey
- close/X controls
- destination field

### B. Destination search

We want:

- destination input field
- placeholder/hint
- suggestions/results
- selected destination

### C. Price/vehicle category screen

We want:

- ride category
- price
- ETA if shown
- continue/confirm button

### D. Final confirmation screen

We want:

- final request/confirm button
- price
- origin
- destination

### E. Searching for driver

We want:

- "searching driver" text/state
- cancel controls
- progress/state labels

### F. Driver assigned

We want:

- driver name
- vehicle model
- vehicle color if available
- license plate
- ETA
- pickup information
- cancellation state if shown

Do not automate these screens yet.

Just make sure their accessibility structures can be captured.

---

# 27. README

Create a concise `README.md` in the project explaining:

## What the app does

It inspects accessibility metadata exposed by Android apps for local diagnostic testing.

## What it does NOT do

It does not automate Cabify, click buttons, take screenshots, or upload data.

## How to use it

1. Install APK.
2. Open app.
3. Open Accessibility Settings.
4. Enable Accessibility Inspector.
5. Return to app.
6. Press Start Capture.
7. Open Cabify.
8. Navigate manually through the desired screens.
9. Return to Accessibility Inspector.
10. Press Stop Capture.
11. Press Share Log.
12. Send the `.txt` file to yourself.

## Privacy

Logs remain local unless explicitly shared by the user.

---

# 28. Build quality requirements

Before considering the task complete:

- project compiles
- app launches
- accessibility settings button works
- accessibility service can be enabled
- app correctly detects enabled/disabled state
- Start/Stop capture works
- log file is created
- accessibility events are written
- tree snapshot is written
- duplicate suppression does not break logging
- Clear Log works
- Share Log launches Android Sharesheet
- shared text file can be opened by another app
- no INTERNET permission exists unless clearly justified
- no crashes when switching rapidly between apps
- no crashes when `rootInActiveWindow == null`
- no password text is written to log

---

# 29. Manual test checklist for the developer

After implementation, perform at least this test:

1. Install app on an emulator or physical Android device.
2. Enable its accessibility service.
3. Start capture.
4. Open Android Settings.
5. Navigate through two Settings screens.
6. Stop capture.
7. Verify the log contains:
   - Settings package
   - multiple events
   - accessibility tree
   - text labels
   - bounds
   - clickable flags
   - actions
8. Share the log through the Android Sharesheet.
9. Open/share the file successfully.
10. Confirm no network permission was added.

Cabify-specific validation will be done manually on my physical phone after Cursor completes the project.

---

# 30. Deliverables

Cursor should implement the complete Android project, not merely provide snippets.

Deliver:

- buildable project
- all Kotlin code
- Manifest changes
- accessibility XML configuration
- FileProvider configuration
- simple Compose UI
- logging
- package filtering
- full tree serialization
- interesting-node summary
- session files
- share functionality
- README

Do not stop after writing a plan.

Implement it.

---

# 31. Development workflow

Before coding:

1. Inspect the existing repository if one already exists.
2. If the repository is empty, create the Android project structure.
3. Briefly state the implementation approach.
4. Implement incrementally.
5. Build the project.
6. Fix all compile errors.
7. Run relevant tests/static checks where practical.
8. Verify the final APK/build succeeds.
9. Summarize the files created/modified and any physical-device-only validation that remains.

Do not claim that Cabify exposes useful accessibility nodes until we have tested the resulting APK on the actual phone.

---

# 32. Future architecture context — DO NOT implement now

This inspector is the first experiment toward a later accessibility automation agent.

If Cabify exposes sufficiently useful nodes, a future application may use a state machine similar to:

```text
IDLE
  ↓
OPEN_CABIFY
  ↓
DISMISS_OPTIONAL_POPUP
  ↓
OPEN_DESTINATION_SEARCH
  ↓
ENTER_DESTINATION
  ↓
SELECT_DESTINATION
  ↓
READ_PRICE
  ↓
WAIT_FOR_USER_CONFIRMATION
  ↓
CONFIRM_RIDE
  ↓
WAIT_FOR_DRIVER
  ↓
READ_DRIVER_DETAILS
  ↓
COMPLETE
```

The preferred future locator order would likely be:

1. accessibility `viewIdResourceName`
2. accessibility text/contentDescription/hint
3. semantic node properties/actions
4. relationship to parent/child nodes
5. bounds/relative layout
6. computer vision or AI as fallback
7. fixed coordinates only as a last-resort fallback

But again:

**DO NOT implement this automation in the inspector POC.**

First collect evidence.

---

# 33. Final instruction to Cursor

Build the smallest reliable diagnostic tool that will let us answer:

> "Can a custom Android AccessibilityService semantically understand enough of Cabify's UI to later control Cabify without depending primarily on hard-coded screen coordinates?"

Optimize for:

- observability
- readable logs
- reliability
- small codebase
- easy APK installation
- easy sharing of logs

Do not optimize for production architecture yet.
