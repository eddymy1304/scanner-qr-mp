# scanner-qr-mp

Librería **Kotlin Multiplatform** de escaneo QR / códigos de barras para **Android**, **iOS** y **KMP**, con UI en **Compose Multiplatform**.

- Android: CameraX + ML Kit Barcode (modelo **bundled**, sin Google Play Services)
- iOS: AVFoundation + Vision (`VNDetectBarcodesRequest`)
- Arquitectura Clean + MVI (estilo Now in Android), lectura eficiente de frames garantizada

## Instalación

### Android / KMP (GitHub Packages)

```kotlin
// settings.gradle.kts
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/lacalera/scanner-qr-mp")
        credentials {
            username = providers.gradleProperty("gpr.user").get()
            password = providers.gradleProperty("gpr.key").get()
        }
    }
}

// build.gradle.kts
dependencies {
    implementation("pe.lacalera.scanner:scanner:0.1.0") // umbrella: todo en uno
    // o granular: scanner-core (dominio puro) / scanner-camera (motor) / scanner-ui (composables)
}
```

### iOS nativo (SPM)

```swift
.package(url: "https://github.com/lacalera/scanner-qr-mp", from: "0.1.0")
```

La app consumidora debe declarar en su `Info.plist`:

| Key | Valor |
|---|---|
| `NSCameraUsageDescription` | texto para el usuario |
| `CADisableMinimumFrameDurationOnPhone` | `true` (requerido por Compose) |

En Android no se declara nada: el permiso `CAMERA` llega por manifest merge.

## Uso

### Nivel 1 — una línea (Compose)

```kotlin
QrScanner(onScanned = { result -> println(result.rawValue) })
```

Defaults: solo QR, `SingleShot` (detecta uno y **deja de analizar frames**), overlay con viewfinder + láser, permisos integrados, haptic. **Fullscreen o embebido lo decide el `Modifier`**: `fillMaxSize()` vs `size(280.dp)`.

### Nivel 2 — configurable (Compose)

```kotlin
val controller = rememberScannerController() // torch, zoom, pause/resume, switchCamera

CodeScanner(
    onEvent = { event ->
        when (event) {
            is ScannerEvent.Scanned -> { /* event.result */ }
            is ScannerEvent.Failed -> { /* event.error */ }
            ScannerEvent.PermissionPermanentlyDenied -> { }
            ScannerEvent.Dismissed -> { }
        }
    },
    config = ScannerConfig(
        formats = BarcodeFormat.ONE_DIMENSIONAL + BarcodeFormat.QrCode,
        scanMode = ScanMode.ContinuousDistinct(cooldownPerValue = 2.seconds), // lote con dedupe
        camera = CameraConfig(torchOnStart = false, tapToFocus = true),
        feedback = FeedbackConfig(haptic = true, sound = true),
        strings = ScannerStrings(permissionRationale = "..."), // localizable
    ),
    overlayConfig = OverlayConfig(
        shape = ViewfinderShape.Rectangle(),   // RoundedSquare | Rectangle | Circle | None
        laser = LaserConfig(color = Color.Red),
        showCloseButton = true,
        showSwitchCameraButton = true,
    ),
    controller = controller,
    overlay = null,             // o slot 100% custom: { state -> MiOverlay(state) }
    permissionContent = null,   // o UI de permisos propia
)
```

`ScanMode`:
- `SingleShot` — emite 1 y pausa el análisis (la preview sigue); rearmar con `controller.resume()`
- `Continuous(emitInterval)` — throttle
- `ContinuousDistinct(cooldownPerValue)` — no repite el mismo código durante el cooldown

### iOS nativo (Swift)

```swift
import LaCaleraScanner

let config = ScannerConfigBuilder()
config.formats = [BarcodeFormat.qrcode, BarcodeFormat.ean13]
config.singleShot = true

let vc = ScannerViewControllerKt.ScannerViewController(
    config: config,
    onScanned: { result in print(result.rawValue) },
    onError: { error in },
    onDismissRequest: { /* cerrar */ }
)
// presentar modal o embeber como child VC
```

## Eficiencia de lectura (garantías)

- Ningún frame viejo se encola: `STRATEGY_KEEP_ONLY_LATEST` (Android) / `alwaysDiscardsLateVideoFrames` (iOS)
- El análisis corre al ritmo real del dispositivo (el siguiente frame entra recién al terminar el anterior)
- Con análisis pausado el frame se descarta al instante **sin invocar el motor ML**
- `ScanRegion.Normalized` recorta el frame **antes** del motor (menos píxeles, menos CPU)
- Resolución de análisis 720p (sweet spot de los motores)

## Desarrollo

```bash
./gradlew :scanner-core:allTests                                  # tests core (JVM + iOS sim)
./gradlew :sample:androidApp:assembleDebug                        # demo Android
./gradlew :sample:composeApp:linkDebugFrameworkIosSimulatorArm64  # framework demo iOS
./gradlew :scanner:assembleLaCaleraScannerReleaseXCFramework      # XCFramework
./gradlew apiCheck                                                # API pública (BCV)
cd sample/iosApp && xcodegen generate                             # proyecto Xcode del sample Swift
```

Release: workflow `Release` (dispatch con la versión) → publica Maven en GitHub Packages, sube el XCFramework al Release y actualiza `Package.swift`.
