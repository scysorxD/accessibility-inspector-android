# Accessibility Inspector para Android

Aplicación Android nativa de diagnóstico para averiguar qué metadatos de
accesibilidad expone Cabify en un teléfono real. Registra eventos y la jerarquía
de nodos en archivos de texto locales que la persona usuaria puede compartir
explícitamente.

## Qué hace y qué no hace

La aplicación:

- registra un `AccessibilityService`;
- muestra si el servicio está realmente habilitado en Android;
- captura eventos y árboles de accesibilidad con rutas padre/hijo;
- muestra el último nombre de paquete observado para descubrir el paquete real
  de Cabify instalado en el teléfono;
- permite capturar todas las aplicaciones o solamente el paquete seleccionado;
- oculta todos los campos textuales de nodos de contraseña;
- evita árboles duplicados consecutivos, limita el recorrido a 50 niveles,
  espera 750 ms de calma pero fuerza una captura cada 3 segundos durante
  cambios continuos, y conserva como máximo 10 sesiones;
- limita cada sesión a 25 MiB, escribe una marca `LOG_SIZE_LIMIT_REACHED` al
  alcanzar el límite y aun así finaliza correctamente la sesión;
- guarda archivos `.txt` en el almacenamiento privado de la aplicación;
- comparte un archivo únicamente al pulsar **Compartir registro**, mediante el
  menú estándar de Android y un `content://` temporal.

No automatiza Cabify. No pulsa botones, no ejecuta acciones de nodos, no hace
gestos, no escribe texto, no navega automáticamente, no toma capturas de
pantalla, no graba la pantalla, no usa OCR, visión artificial, IA, HTTP,
WebSockets, MQTT, analítica, backend ni nube. El manifiesto no solicita
`android.permission.INTERNET` ni permisos generales de almacenamiento.

Este es un proyecto Android nativo con Kotlin y Jetpack Compose. **No se usan
npm ni npx**: esas herramientas pertenecen al ecosistema JavaScript/Node.js y
no forman parte de la compilación o instalación de esta aplicación.

## Requisitos

- Windows y una versión actual de Android Studio compatible con Android Gradle
  Plugin 9.3 (por ejemplo, Android Studio Quail 2 / 2026.1.2 o posterior).
- JDK 17 o posterior. Android Studio puede utilizar su JDK integrado.
- Android SDK Platform 37 y Android SDK Build-Tools 36.0.0.
- Un teléfono físico con Android 8.0 (API 26) o posterior.
- Cable USB de datos. Algunos cables solamente cargan y no permiten ADB.

Versiones principales fijadas por el proyecto: Kotlin 2.3.21, Android Gradle
Plugin 9.3.0, Gradle 9.5.0, Compose BOM 2026.06.01, `compileSdk`/`targetSdk` 37
y `minSdk` 26. AGP 9 incorpora Kotlin directamente; por eso no se aplica el
antiguo plugin `org.jetbrains.kotlin.android`, que AGP 9.3 rechaza. Se mantiene
Kotlin/Compose Compiler 2.3.21.

## Abrir y compilar con Android Studio, paso a paso

1. Instale o abra la versión actual de **Android Studio**.
2. En la pantalla inicial elija **Open**. Si ya hay otro proyecto abierto, use
   **File > Open**.
3. Seleccione exactamente esta carpeta, no la subcarpeta `app`:

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
9. Si Android Studio solicita el SDK, elija
   `C:\Users\scyso\AppData\Local\Android\Sdk`. El archivo `local.properties` es
   local, está ignorado por Git y no debe compartirse.
10. Espere a que desaparezcan las tareas de indexado y sincronización de la
    barra inferior.

## Preparar y conectar un teléfono físico

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

## Compilar e instalar desde PowerShell

Abra PowerShell en `E:\_GIT\accessibility-inspector-android`. No hace falta
instalar Gradle globalmente porque se incluye Gradle Wrapper.

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
C:\Users\scyso\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
C:\Users\scyso\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Si `adb` ya está en `PATH`, puede usar simplemente `adb devices` y
`adb install -r ...`. El APK de depuración queda en:

`E:\_GIT\accessibility-inspector-android\app\build\outputs\apk\debug\app-debug.apk`

## Habilitar el servicio de accesibilidad

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

## Flujo recomendado: descubrir el paquete y capturar Cabify

### Primera sesión: descubrir el paquete

1. En Accessibility Inspector seleccione **Todas las aplicaciones**.
2. Pulse **Iniciar**. El indicador debe cambiar a **GRABANDO**.
3. Abra Cabify manualmente y navegue por varias pantallas. No espere que el
   inspector pulse ni escriba nada: toda navegación debe hacerla usted.
4. Regrese a Accessibility Inspector y pulse **Detener**.
5. Revise **Último paquete observado**. También puede compartir el registro y
   buscar las líneas `packageName:` para confirmar el paquete exacto. No se
   presupone que sea `com.cabify.rider`.
6. Pulse **Usar el paquete observado**, o escriba/copíe el paquete confirmado
   en **Paquete de Cabify**.

El último paquete puede corresponder al lanzador o a la interfaz del sistema si
esta produjo el evento más reciente. En ese caso, use el registro de la sesión:
busque los eventos generados mientras Cabify estaba visible y copie su
`packageName`.

### Sesiones siguientes: solo Cabify

1. Seleccione **Solo Cabify** y confirme que el campo de paquete no esté vacío.
2. Pulse **Iniciar**.
3. Abra Cabify y recorra manualmente las pantallas que quiera estudiar:
   inicio, búsqueda de destino, resultados, categoría/precio, confirmación,
   búsqueda de conductor y conductor asignado, sin solicitar un viaje real si
   no corresponde a la prueba.
4. Regrese al inspector y pulse **Detener**. Detener evita nuevas escrituras
   aunque el servicio siga habilitado en Ajustes.
5. Pulse **Compartir registro**, elija Gmail, Outlook, WhatsApp, Drive u otra
   aplicación y envíese el `.txt`. Compartir siempre requiere una elección
   explícita en el menú estándar de Android.
6. Para una prueba limpia, pulse **Borrar registros > Eliminar**. Esta acción
   borra solamente los archivos de diagnóstico privados de Accessibility
   Inspector, nunca archivos de Cabify ni del almacenamiento general.

## Privacidad y seguridad

Un servicio de accesibilidad puede encontrar texto visible de otras
aplicaciones. Use **Solo Cabify** después de confirmar el paquete, detenga la
captura al terminar y revise el archivo antes de compartirlo. Los campos
marcados por Android como contraseña se sustituyen por
`<REDACTED_PASSWORD_FIELD>` en texto, pista, descripción, estado y error.

No se recogen IMEI, número de serie, Advertising ID, teléfono ni cuentas. No
hay permiso de red y no existe subida automática. Los registros permanecen en
el directorio privado de la aplicación y solamente salen del dispositivo por
una acción explícita de compartir. Desinstalar o borrar los datos de la
aplicación elimina esos registros.

## Solución de problemas

- **Gradle no encuentra el SDK:** cree/revise `local.properties` con
  `sdk.dir=C\:\\Users\\scyso\\AppData\\Local\\Android\\Sdk`, o configure la ruta
  en **File > Project Structure > SDK Location**.
- **Falta API 37 o Build-Tools 36:** instálelos desde **Tools > SDK Manager** y
  sincronice otra vez. Gradle puede ofrecer instalarlos si las licencias ya
  están aceptadas.
- **Java/Gradle incompatibles:** en **File > Settings > Build, Execution,
  Deployment > Build Tools > Gradle > Gradle JDK**, seleccione el JDK integrado
  de Android Studio o JDK 17.
- **El servicio sigue como deshabilitado:** vuelva a Ajustes, compruebe el
  interruptor real y luego regrese a la app. Algunos fabricantes desactivan
  servicios al ahorrar batería o después de reinstalar.
- **No aparece ningún evento:** confirme **GRABANDO**, el servicio habilitado y,
  en modo Solo Cabify, que el paquete coincida exactamente, incluyendo
  mayúsculas/minúsculas.
- **El último paquete no es Cabify:** inspeccione el `.txt` de la sesión Todas
  las aplicaciones y busque `packageName:` en eventos capturados con Cabify
  visible.
- **Compartir está desactivado:** primero inicie y detenga una captura. No se
  comparte durante la grabación para garantizar un archivo finalizado.
- **ADB muestra `unauthorized`:** desbloquee el teléfono, acepte la huella RSA,
  ejecute `adb kill-server`, `adb start-server` y vuelva a conectar.
- **ADB muestra más de un dispositivo:** use
  `adb -s NUMERO_DE_SERIE install -r RUTA_DEL_APK`.
- **La compilación parece usar archivos antiguos:** ejecute
  `.\gradlew.bat clean assembleDebug` y vuelva a instalar con `-r`.

## Validación manual en un teléfono

Estas comprobaciones no pueden completarse únicamente durante la compilación:

1. Instalar y abrir el APK.
2. Habilitar el servicio y confirmar que la app muestre el estado real.
3. Iniciar una captura en Todas las aplicaciones.
4. Abrir Ajustes de Android y recorrer al menos dos pantallas.
5. Detener y compartir el registro.
6. Abrir el `.txt` en otra aplicación y confirmar eventos, paquete de Ajustes,
   árbol completo, rutas/indentación, textos, límites, indicadores `clickable`
   y acciones legibles.
7. Repetir una pantalla sin cambios y comprobar una entrada `TREE_UNCHANGED`.
8. Probar un campo de contraseña y confirmar que no aparece su contenido.
9. Borrar registros y confirmar que Compartir queda desactivado.
10. Confirmar que la lista de permisos no contiene acceso a Internet.
11. Repetir el flujo con Cabify para determinar qué nodos expone realmente.

La compilación exitosa no demuestra que Cabify exponga destino, precio,
botones, conductor, vehículo, matrícula o ETA. Esa conclusión solamente puede
obtenerse revisando el registro producido por la versión real de Cabify en un
teléfono físico.