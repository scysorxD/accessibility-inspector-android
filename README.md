# Accessibility Inspector for Android

**Languages / Idiomas:** [English](#english) · [Español](#español)

---

## English

Native Android diagnostic app that inspects the accessibility metadata exposed
by **any foreground Android app** and saves human-readable event/tree logs you
can share explicitly.

### Not Cabify-only

This tool is **generic**. It works with Settings, launchers, banking apps,
ride-hailing apps, or any other package that produces accessibility events.

**Cabify is only one use case** that motivated the proof of concept: discovering
whether Cabify’s UI exposes enough semantic nodes for a future automation
experiment. Nothing in the capture engine is hard-coded to Cabify’s package
name. You discover the real package on device and optionally filter to it.

### What it does / does not do

The app:

- registers an `AccessibilityService`;
- shows whether that service is actually enabled in Android;
- captures accessibility events and parent/child node trees;
- shows the last observed package name so you can discover any app’s package;
- can capture **all apps** or **only a selected package** (the UI label may say
  “Cabify only”, but the filter accepts whatever package you enter);
- redacts password-field text;
- suppresses consecutive duplicate trees, caps traversal at depth 50, waits
  750 ms of quiet time but forces a snapshot every 3 seconds during continuous
  changes, and keeps at most 10 sessions;
- caps each session at 25 MiB, writes `LOG_SIZE_LIMIT_REACHED`, and still closes
  the session cleanly;
- stores `.txt` logs in app-private storage;
- shares a log only when you tap **Share log**, via the Android Sharesheet and a
  temporary `content://` URI.

It does **not** automate any app. It does not click, perform node actions,
inject gestures, type text, navigate automatically, take screenshots, record
the screen, use OCR, computer vision, AI, HTTP, WebSockets, MQTT, analytics,
backends, or the cloud. The manifest does not request
`android.permission.INTERNET` or broad storage permissions.

This is a native Kotlin + Jetpack Compose Android project. **npm / npx are not
used**; those belong to the JavaScript/Node.js ecosystem and are not part of
building or installing this app.

### Requirements

- Windows and a current Android Studio build compatible with Android Gradle
  Plugin 9.3 (for example Android Studio Quail 2 / 2026.1.2 or newer).
- JDK 17+. Android Studio’s bundled JDK is fine.
- Android SDK Platform 37 and Android SDK Build-Tools 36.0.0.
- A physical phone with Android 8.0 (API 26) or newer.
- A data-capable USB cable (charge-only cables will not work with ADB).

Pinned versions: Kotlin 2.3.21, Android Gradle Plugin 9.3.0, Gradle 9.5.0,
Compose BOM 2026.06.01, `compileSdk`/`targetSdk` 37, `minSdk` 26. AGP 9 has
built-in Kotlin support, so the legacy `org.jetbrains.kotlin.android` plugin is
not applied.

### Open and build in Android Studio

1. Install or open current **Android Studio**.
2. On the welcome screen choose **Open**. If a project is already open, use
   **File > Open**.
3. Select this repository root folder (not the `app` subfolder), for example:

   `E:\_GIT\accessibility-inspector-android`

4. If prompted, review the path and click **Trust Project**.
5. Open **Tools > SDK Manager**.
6. Under **SDK Platforms**, enable **Show Package Details** if needed, select
   **Android API 37**, then **Apply > OK**.
7. Under **SDK Tools**, enable **Show Package Details**, select **Android SDK
   Build-Tools 36.0.0**, **Android SDK Platform-Tools**, and **Android SDK
   Command-line Tools (latest)**. Click **Apply > OK** and accept licenses.
8. Wait for **Gradle Sync**. If it does not start, click
   **File > Sync Project with Gradle Files**.
9. If Android Studio asks for the SDK location, choose your local SDK (often
   `C:\Users\<you>\AppData\Local\Android\Sdk`). `local.properties` is
   machine-local, Git-ignored, and must not be shared.
10. Wait until indexing/sync tasks finish in the bottom status bar.

### Prepare and connect a physical phone

1. On the phone open **Settings > About phone**.
2. Find **Build number** (on Samsung often under **Software information**).
3. Tap **Build number** seven times and confirm your PIN if asked.
4. Go back and open **System > Developer options** (label varies by OEM).
5. Enable **USB debugging** and confirm the warning.
6. Connect the phone with a data USB cable. If a USB mode picker appears,
   choose **File transfer**.
7. On **Allow USB debugging?**, verify the computer, optionally check **Always
   allow from this computer**, then **Allow**.
8. In Android Studio select the **app** run configuration and your phone in the
   device dropdown.
9. Click the green **Run** button. Android Studio builds, installs, and opens
   Accessibility Inspector.

If the phone does not appear, reconnect, change USB mode, try another
cable/port, re-accept the RSA fingerprint, and run `adb devices`.

### Build and install from PowerShell

Open PowerShell in the repository root. Global Gradle is not required; the
wrapper is included.

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat build
```

Install with Gradle when one authorized phone is connected:

```powershell
.\gradlew.bat installDebug
```

Or with ADB:

```powershell
adb devices
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Debug APK path:

`app\build\outputs\apk\debug\app-debug.apk`

### Enable the accessibility service

Android will not grant this permission silently:

1. Open **Accessibility Inspector**.
2. Tap **Open accessibility settings**.
3. On the phone open, depending on OEM labels:
   **Downloaded apps > Accessibility Inspector**,
   **Installed services > Accessibility Inspector**, or
   **Installed apps > Accessibility Inspector**.
4. Tap **Accessibility Inspector**.
5. Enable **Use Accessibility Inspector** / **Allow service** (or equivalent).
6. Read the system warning and tap **Allow/OK**. Some OEMs ask twice or for a
   PIN.
7. Return to the app with Back. Status should show **Enabled**.

Names differ across Pixel, Samsung, Xiaomi, Motorola, and others. Status is
read from Android, not from a fake preference.

### Recommended capture flow (any app)

#### Session 1: discover the package

1. Select **All apps**.
2. Tap **Start**. Status should show **RECORDING**.
3. Open the target app (Cabify or any other) and navigate manually. The
   inspector never taps or types for you.
4. Return to Accessibility Inspector and tap **Stop**.
5. Check **Last observed package**, or share the log and search for
   `packageName:` lines to confirm the exact package.
6. Tap **Use observed package**, or paste the confirmed package into the
   package field.

The last observed package may briefly be the launcher or System UI. If so, open
the session `.txt` and copy the `packageName` from events captured while your
target app was visible.

#### Later sessions: selected package only

1. Select the single-package mode (UI may say **Cabify only**) and confirm the
   package field is filled.
2. Tap **Start**.
3. Open the target app and walk the screens you care about.
4. Return and tap **Stop**. Stopping ends writes even if the accessibility
   service stays enabled in Settings.
5. Tap **Share log**, pick Gmail, Outlook, WhatsApp, Drive, etc., and send the
   `.txt` to yourself.
6. For a clean run, tap **Clear logs > Delete**. This deletes only this app’s
   private diagnostic files.

### Privacy and safety

An accessibility service can see on-screen text from other apps. Prefer
single-package filtering after you know the package, stop capture when done,
and review the file before sharing. Password fields are replaced with
`<REDACTED_PASSWORD_FIELD>`.

No IMEI, serial, Advertising ID, phone number, or accounts are collected. There
is no network permission and no automatic upload. Logs stay in app-private
storage until you explicitly share them.

### Troubleshooting

- **Gradle cannot find the SDK:** set `sdk.dir=...` in `local.properties`, or
  configure **File > Project Structure > SDK Location**.
- **Missing API 37 / Build-Tools 36:** install them in **Tools > SDK Manager**
  and sync again.
- **Java/Gradle mismatch:** set **Gradle JDK** to Android Studio’s JDK or JDK
  17 under **Settings > Build, Execution, Deployment > Build Tools > Gradle**.
- **Service still Disabled:** re-check the real toggle in Settings, then return
  to the app.
- **No events:** confirm **RECORDING**, service enabled, and exact package
  match in single-package mode.
- **Share disabled:** start then stop a capture first. Sharing is blocked while
  recording so the file can finish cleanly.
- **ADB `unauthorized`:** unlock the phone, accept the RSA prompt, run
  `adb kill-server` / `adb start-server`, reconnect.
- **Multiple devices:** `adb -s SERIAL install -r PATH_TO_APK`.
- **Stale build:** `.\gradlew.bat clean assembleDebug`, then reinstall with
  `-r`.

### Manual validation on a phone

1. Install and open the APK.
2. Enable the service and confirm the app shows the real status.
3. Start capture in **All apps**.
4. Open Android Settings and browse at least two screens.
5. Stop and share the log.
6. Open the `.txt` elsewhere and confirm events, package, full tree,
   paths/indentation, text, bounds, `clickable`, and readable actions.
7. Revisit an unchanged screen and look for `TREE_UNCHANGED`.
8. Exercise a password field and confirm content is redacted.
9. Clear logs and confirm Share is disabled.
10. Confirm permissions do not include Internet.
11. Optionally repeat with Cabify or any other app of interest.

A successful build does **not** prove that any specific app (including Cabify)
exposes useful semantic nodes. That conclusion only comes from inspecting logs
captured on a real device.

---

## Español

Aplicación Android nativa de diagnóstico que inspecciona los metadatos de
accesibilidad expuestos por **cualquier aplicación en primer plano** y guarda
registros legibles de eventos y árboles que la persona usuaria puede compartir
explícitamente.

### No es exclusivo de Cabify

Esta herramienta es **genérica**. Sirve con Ajustes, lanzadores, bancos,
apps de movilidad o cualquier otro paquete que genere eventos de accesibilidad.

**Cabify es solo un caso de uso** que motivó la prueba de concepto: comprobar si
la interfaz de Cabify expone nodos semánticos suficientes para un futuro
experimento de automatización. El motor de captura **no** tiene hardcodeado el
paquete de Cabify. El paquete real se descubre en el teléfono y, si quieres,
puedes filtrar por él.

### Qué hace y qué no hace

La aplicación:

- registra un `AccessibilityService`;
- muestra si el servicio está realmente habilitado en Android;
- captura eventos y árboles de accesibilidad con rutas padre/hijo;
- muestra el último nombre de paquete observado para descubrir el de cualquier
  app;
- permite capturar **todas las aplicaciones** o **solo el paquete seleccionado**
  (la etiqueta de la UI puede decir “Solo Cabify”, pero el filtro acepta el
  paquete que introduzcas);
- oculta todos los campos textuales de nodos de contraseña;
- evita árboles duplicados consecutivos, limita el recorrido a 50 niveles,
  espera 750 ms de calma pero fuerza una captura cada 3 segundos durante
  cambios continuos, y conserva como máximo 10 sesiones;
- limita cada sesión a 25 MiB, escribe una marca `LOG_SIZE_LIMIT_REACHED` al
  alcanzar el límite y aun así finaliza correctamente la sesión;
- guarda archivos `.txt` en el almacenamiento privado de la aplicación;
- comparte un archivo únicamente al pulsar **Compartir registro**, mediante el
  menú estándar de Android y un `content://` temporal.

**No** automatiza ninguna aplicación. No pulsa botones, no ejecuta acciones de
nodos, no hace gestos, no escribe texto, no navega automáticamente, no toma
capturas de pantalla, no graba la pantalla, no usa OCR, visión artificial, IA,
HTTP, WebSockets, MQTT, analítica, backend ni nube. El manifiesto no solicita
`android.permission.INTERNET` ni permisos generales de almacenamiento.

Este es un proyecto Android nativo con Kotlin y Jetpack Compose. **No se usan
npm ni npx**: esas herramientas pertenecen al ecosistema JavaScript/Node.js y
no forman parte de la compilación o instalación de esta aplicación.

### Requisitos

- Windows y una versión actual de Android Studio compatible con Android Gradle
  Plugin 9.3 (por ejemplo, Android Studio Quail 2 / 2026.1.2 o posterior).
- JDK 17 o posterior. Android Studio puede utilizar su JDK integrado.
- Android SDK Platform 37 y Android SDK Build-Tools 36.0.0.
- Un teléfono físico con Android 8.0 (API 26) o posterior.
- Cable USB de datos. Algunos cables solamente cargan y no permiten ADB.

Versiones principales fijadas por el proyecto: Kotlin 2.3.21, Android Gradle
Plugin 9.3.0, Gradle 9.5.0, Compose BOM 2026.06.01, `compileSdk`/`targetSdk` 37
y `minSdk` 26. AGP 9 incorpora Kotlin directamente; por eso no se aplica el
antiguo plugin `org.jetbrains.kotlin.android`.

### Abrir y compilar con Android Studio, paso a paso

1. Instale o abra la versión actual de **Android Studio**.
2. En la pantalla inicial elija **Open**. Si ya hay otro proyecto abierto, use
   **File > Open**.
3. Seleccione la carpeta raíz de este repositorio, no la subcarpeta `app`, por
   ejemplo:

   `E:\_GIT\accessibility-inspector-android`

4. Si Android Studio pregunta si confía en el proyecto, revise la ruta y pulse
   **Trust Project**.
5. Abra **Tools > SDK Manager**.
6. En **SDK Platforms**, active **Show Package Details** si fuera necesario,
   seleccione **Android API 37** y pulse **Apply > OK**.
7. En **SDK Tools**, active **Show Package Details**, seleccione **Android SDK
   Build-Tools 36.0.0**, **Android SDK Platform-Tools** y **Android SDK
   Command-line Tools (latest)**. Pulse **Apply > OK** y acepte las licencias.
8. Espere a que termine **Gradle Sync**. Si no empieza automáticamente, pulse
   **File > Sync Project with Gradle Files**.
9. Si Android Studio solicita el SDK, elija su SDK local (a menudo
   `C:\Users\<usuario>\AppData\Local\Android\Sdk`). El archivo
   `local.properties` es local, está ignorado por Git y no debe compartirse.
10. Espere a que desaparezcan las tareas de indexado y sincronización de la
    barra inferior.

### Preparar y conectar un teléfono físico

1. En el teléfono abra **Ajustes > Acerca del teléfono**.
2. Busque **Número de compilación**. En Samsung suele estar en
   **Información de software > Número de compilación**.
3. Pulse **Número de compilación** siete veces y confirme el PIN si se pide.
4. Regrese a Ajustes y abra **Sistema > Opciones para desarrolladores**. Según
   el fabricante puede llamarse directamente **Opciones de desarrollador**,
   estar en **Ajustes adicionales**, o aparecer al final del menú principal.
5. Active **Depuración USB** y confirme la advertencia.
6. Conecte el teléfono al PC con un cable de datos. Si aparece un selector USB,
   elija **Transferencia de archivos**.
7. En el diálogo **¿Permitir depuración USB?**, verifique el PC, opcionalmente
   marque **Permitir siempre desde este equipo** y pulse **Permitir**.
8. En Android Studio, seleccione el módulo/configuración **app** y el teléfono
   en el selector de dispositivos de la barra superior.
9. Pulse el botón verde **Run** (triángulo). Android Studio compilará, instalará
   y abrirá Accessibility Inspector.

Si el teléfono no aparece, desconéctelo, cambie el modo USB, pruebe otro cable o
puerto, acepte de nuevo la huella RSA y ejecute `adb devices`.

### Compilar e instalar desde PowerShell

Abra PowerShell en la raíz del repositorio. No hace falta instalar Gradle
globalmente porque se incluye Gradle Wrapper.

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat build
```

Para instalar directamente mediante Gradle con un único teléfono autorizado:

```powershell
.\gradlew.bat installDebug
```

Alternativa con ADB:

```powershell
adb devices
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

El APK de depuración queda en:

`app\build\outputs\apk\debug\app-debug.apk`

### Habilitar el servicio de accesibilidad

Android no permite que una aplicación se conceda este acceso por sí sola:

1. Abra **Accessibility Inspector**.
2. Pulse **Abrir ajustes de accesibilidad**.
3. En el teléfono abra, según aparezca:
   **Aplicaciones descargadas > Accessibility Inspector**,
   **Servicios instalados > Accessibility Inspector** o
   **Aplicaciones instaladas > Accessibility Inspector**.
4. Pulse **Accessibility Inspector**.
5. Active **Usar Accessibility Inspector**, **Permitir servicio** o el
   interruptor equivalente.
6. Lea la advertencia del sistema y pulse **Permitir/Aceptar**. En algunos
   fabricantes hay que confirmar una segunda vez o introducir el PIN.
7. Vuelva a la aplicación con Atrás. Debe mostrar **Habilitado**.

Los nombres cambian entre Pixel, Samsung, Xiaomi, Motorola y otros fabricantes.
Si el botón no abre la lista exacta, busque manualmente
**Ajustes > Accesibilidad > Aplicaciones descargadas/Servicios instalados**.
El estado mostrado se lee de Android; no es una preferencia simulada.

### Flujo recomendado de captura (cualquier app)

#### Primera sesión: descubrir el paquete

1. En Accessibility Inspector seleccione **Todas las aplicaciones**.
2. Pulse **Iniciar**. El indicador debe cambiar a **GRABANDO**.
3. Abra la app objetivo (Cabify o cualquier otra) y navegue manualmente. No
   espere que el inspector pulse ni escriba nada.
4. Regrese a Accessibility Inspector y pulse **Detener**.
5. Revise **Último paquete observado**. También puede compartir el registro y
   buscar las líneas `packageName:` para confirmar el paquete exacto.
6. Pulse **Usar el paquete observado**, o escriba/copie el paquete confirmado
   en el campo de paquete.

El último paquete puede corresponder al lanzador o a la interfaz del sistema si
esta produjo el evento más reciente. En ese caso, use el registro de la sesión:
busque los eventos generados mientras la app objetivo estaba visible y copie su
`packageName`.

#### Sesiones siguientes: solo el paquete seleccionado

1. Seleccione el modo de un solo paquete (la UI puede decir **Solo Cabify**) y
   confirme que el campo de paquete no esté vacío.
2. Pulse **Iniciar**.
3. Abra la app objetivo y recorra manualmente las pantallas que quiera estudiar.
4. Regrese al inspector y pulse **Detener**. Detener evita nuevas escrituras
   aunque el servicio siga habilitado en Ajustes.
5. Pulse **Compartir registro**, elija Gmail, Outlook, WhatsApp, Drive u otra
   aplicación y envíese el `.txt`.
6. Para una prueba limpia, pulse **Borrar registros > Eliminar**. Esta acción
   borra solamente los archivos de diagnóstico privados de Accessibility
   Inspector.

### Privacidad y seguridad

Un servicio de accesibilidad puede encontrar texto visible de otras
aplicaciones. Prefiera el filtro de un solo paquete después de confirmarlo,
detenga la captura al terminar y revise el archivo antes de compartirlo. Los
campos marcados por Android como contraseña se sustituyen por
`<REDACTED_PASSWORD_FIELD>` en texto, pista, descripción, estado y error.

No se recogen IMEI, número de serie, Advertising ID, teléfono ni cuentas. No
hay permiso de red y no existe subida automática. Los registros permanecen en
el directorio privado de la aplicación y solamente salen del dispositivo por
una acción explícita de compartir. Desinstalar o borrar los datos de la
aplicación elimina esos registros.

### Solución de problemas

- **Gradle no encuentra el SDK:** cree/revise `local.properties` con
  `sdk.dir=...`, o configure la ruta en **File > Project Structure > SDK
  Location**.
- **Falta API 37 o Build-Tools 36:** instálelos desde **Tools > SDK Manager** y
  sincronice otra vez.
- **Java/Gradle incompatibles:** en **File > Settings > Build, Execution,
  Deployment > Build Tools > Gradle > Gradle JDK**, seleccione el JDK integrado
  de Android Studio o JDK 17.
- **El servicio sigue como deshabilitado:** vuelva a Ajustes, compruebe el
  interruptor real y luego regrese a la app.
- **No aparece ningún evento:** confirme **GRABANDO**, el servicio habilitado y,
  en modo de un solo paquete, que el paquete coincida exactamente.
- **Compartir está desactivado:** primero inicie y detenga una captura. No se
  comparte durante la grabación para garantizar un archivo finalizado.
- **ADB muestra `unauthorized`:** desbloquee el teléfono, acepte la huella RSA,
  ejecute `adb kill-server`, `adb start-server` y vuelva a conectar.
- **ADB muestra más de un dispositivo:** use
  `adb -s NUMERO_DE_SERIE install -r RUTA_DEL_APK`.
- **La compilación parece usar archivos antiguos:** ejecute
  `.\gradlew.bat clean assembleDebug` y vuelva a instalar con `-r`.

### Validación manual en un teléfono

Estas comprobaciones no pueden completarse únicamente durante la compilación:

1. Instalar y abrir el APK.
2. Habilitar el servicio y confirmar que la app muestre el estado real.
3. Iniciar una captura en Todas las aplicaciones.
4. Abrir Ajustes de Android y recorrer al menos dos pantallas.
5. Detener y compartir el registro.
6. Abrir el `.txt` en otra aplicación y confirmar eventos, paquete, árbol
   completo, rutas/indentación, textos, límites, indicadores `clickable` y
   acciones legibles.
7. Repetir una pantalla sin cambios y comprobar una entrada `TREE_UNCHANGED`.
8. Probar un campo de contraseña y confirmar que no aparece su contenido.
9. Borrar registros y confirmar que Compartir queda desactivado.
10. Confirmar que la lista de permisos no contiene acceso a Internet.
11. Opcionalmente repetir el flujo con Cabify o con cualquier otra app de
    interés.

La compilación exitosa **no** demuestra que una app concreta (incluida Cabify)
exponga nodos semánticos útiles. Esa conclusión solamente puede obtenerse
revisando el registro producido en un teléfono físico.
