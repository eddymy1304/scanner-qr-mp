# scanner-qr-mp

Librería de escaneo **QR / códigos de barras** para **Android**, **iOS** y **KMP**, con UI incluida y configurable.

- ✅ Dos niveles de API: **una línea** con defaults sensatos, o **todo configurable** (UI/UX y lectura)
- ✅ Fullscreen o embebido en cualquier layout — lo decide tu `Modifier`
- ✅ Lectura eficiente garantizada: nunca analiza frames de más, se pausa al detectar
- ✅ Permisos de cámara resueltos por la librería (UI incluida y reemplazable)
- ✅ Android sin Google Play Services (ML Kit bundled) · iOS 100% nativo (AVFoundation + Vision)

---

## Requisitos

| Plataforma | Mínimo | Debes declarar |
|---|---|---|
| Android | minSdk 24 | Nada — el permiso `CAMERA` llega solo por manifest merge |
| iOS | iOS 15 | `NSCameraUsageDescription` y `CADisableMinimumFrameDurationOnPhone = true` en Info.plist |

---

## Instalación

### Proyecto Android o KMP (Gradle)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/eddymy1304/scanner-qr-mp")
            credentials {
                username = providers.gradleProperty("gpr.user").get()   // tu usuario GitHub
                password = providers.gradleProperty("gpr.key").get()    // token con read:packages
            }
        }
    }
}

// build.gradle.kts del módulo
dependencies {
    implementation("com.eddymy1304.scanner:scanner:0.2.0")
}
```

> `scanner` trae todo. Si solo necesitas el motor sin UI: `scanner-camera`; solo el dominio: `scanner-core`.

### Proyecto iOS nativo (Swift Package Manager)

En Xcode: **File → Add Package Dependencies** → `https://github.com/eddymy1304/scanner-qr-mp`, o en `Package.swift`:

```swift
.package(url: "https://github.com/eddymy1304/scanner-qr-mp", from: "0.2.0")
```

Y en el `Info.plist` de tu app:

```xml
<key>NSCameraUsageDescription</key>
<string>Usamos la cámara para escanear códigos.</string>
<key>CADisableMinimumFrameDurationOnPhone</key>
<true/>
```

---

## Uso — Nivel 1: una línea

```kotlin
import com.eddymy1304.scanner.ui.QrScanner

QrScanner(onScanned = { result -> println(result.rawValue) })
```

Con eso obtienes: solo QR, un escaneo y se pausa (`SingleShot`), overlay con viewfinder + láser animado, vibración al detectar, y la UI de permisos resuelta.

**¿Fullscreen o embebido? Lo decide tu `Modifier`, no hay flag:**

```kotlin
QrScanner(onScanned = { ... }, modifier = Modifier.fillMaxSize())                         // fullscreen
QrScanner(onScanned = { ... }, modifier = Modifier.size(280.dp).clip(RoundedCornerShape(24.dp))) // embebido
```

Parámetros opcionales del nivel simple:

```kotlin
QrScanner(
    onScanned = { ... },
    formats = BarcodeFormat.ALL,                       // default: QR_ONLY
    scanMode = ScanMode.ContinuousDistinct(2.seconds), // default: SingleShot
)
```

---

## Uso — Nivel 2: `CodeScanner` (todo configurable)

```kotlin
val controller = rememberScannerController()

CodeScanner(
    onEvent = { event -> /* ver tabla de eventos */ },
    modifier = Modifier.fillMaxSize(),
    config = ScannerConfig(...),          // QUÉ y CÓMO se lee
    overlayConfig = OverlayConfig(...),   // cómo SE VE el overlay default
    controller = controller,              // control imperativo externo
    overlay = null,                       // null = overlay default | slot 100% custom
    permissionContent = null,             // null = UI de permisos default | slot custom
)
```

### `ScannerConfig` — qué y cómo se lee

```kotlin
ScannerConfig(
    formats = setOf(BarcodeFormat.QrCode, BarcodeFormat.Ean13),
    scanMode = ScanMode.SingleShot,
    camera = CameraConfig(
        lens = CameraLens.Back,            // Back | Front
        torchOnStart = false,              // linterna encendida al abrir
        pinchToZoom = true,                // gesto pinch para zoom
        tapToFocus = true,                 // tap para enfocar
        initialZoomRatio = 1f,
        scanRegion = ScanRegion.FullFrame, // o Normalized(left, top, width, height) 0f..1f
    ),
    feedback = FeedbackConfig(
        haptic = true,                     // vibración al detectar
        sound = false,                     // beep al detectar
    ),
    strings = ScannerStrings(              // textos de la UI integrada — localízalos aquí
        permissionRationale = "Necesitamos la cámara para escanear.",
        permissionRequestButton = "Permitir cámara",
        permissionDeniedMessage = "Actívalo en Ajustes.",
        openSettingsButton = "Abrir ajustes",
        cameraUnavailableMessage = "La cámara no está disponible.",
    ),
)
```

**Formatos** — individuales (`QrCode`, `Ean13`, `Ean8`, `Code128`, `Code39`, `Code93`, `Codabar`, `Itf`, `UpcA`, `UpcE`, `Pdf417`, `Aztec`, `DataMatrix`) o presets:

| Preset | Contenido |
|---|---|
| `BarcodeFormat.QR_ONLY` | solo QR (default) |
| `BarcodeFormat.ONE_DIMENSIONAL` | todos los de barras lineales |
| `BarcodeFormat.TWO_DIMENSIONAL` | QR, PDF417, Aztec, DataMatrix |
| `BarcodeFormat.ALL` | todos |

**`ScanMode`** — el corazón de la eficiencia de lectura:

| Modo | Comportamiento | Úsalo para |
|---|---|---|
| `SingleShot` | Emite **1** resultado y **deja de analizar frames** (la preview sigue viva). Rearmas con `controller.resume()` | "Escanear un código y continuar" (login, pago, ticket) |
| `Continuous(emitInterval)` | Emite continuamente con intervalo mínimo entre emisiones | Lectura libre con throttle |
| `ContinuousDistinct(cooldownPerValue)` | Emite continuamente pero **no repite el mismo código** hasta pasar el cooldown; códigos distintos pasan al instante | **Lotes**: picking, inventario, asistencia |

**`ScanRegion`** — analizar solo una parte del frame (menos CPU):

```kotlin
scanRegion = ScanRegion.Normalized(left = 0.1f, top = 0.3f, width = 0.8f, height = 0.4f)
```

### `OverlayConfig` — cómo se ve

```kotlin
OverlayConfig(
    shape = ViewfinderShape.RoundedSquare(cornerRadius = 16.dp, sizeFraction = 0.7f),
    scrimColor = Color.Black.copy(alpha = 0.55f),  // oscurecido fuera de la ventana
    borderColor = Color.White,
    borderWidth = 3.dp,
    laser = LaserConfig(                            // null = sin láser
        color = Color(0xFFFF3B30),
        strokeWidth = 2.dp,
        sweepDuration = 2.seconds,
    ),
    showTorchButton = true,                         // botón linterna (si el lente la tiene)
    showCloseButton = false,                        // botón X -> evento Dismissed
    showSwitchCameraButton = false,                 // botón cambiar frontal/trasera
    successHighlight = true,                        // dibuja el contorno del código detectado
)
```

**Formas del viewfinder:**

| Shape | Para |
|---|---|
| `RoundedSquare(cornerRadius, sizeFraction)` | QR (default) |
| `Rectangle(cornerRadius, widthFraction, aspectRatio)` | códigos de barras 1D |
| `Circle(sizeFraction)` | estético circular |
| `None` | sin scrim ni ventana (preview limpia) |

### `ScannerController` — control desde fuera

```kotlin
val controller = rememberScannerController()
// ...
controller.toggleTorch()
controller.setZoom(2f)
controller.switchCamera()
controller.pause()      // pausa el análisis (preview sigue)
controller.resume()     // rearma (p.ej. tras un SingleShot)
controller.uiState      // StateFlow<ScannerUiState> observable
```

### Eventos (`onEvent`)

| Evento | Cuándo | Qué hacer |
|---|---|---|
| `ScannerEvent.Scanned(result)` | Código detectado | usar `result.rawValue`, `result.format`, `result.cornerPoints` |
| `ScannerEvent.Failed(error)` | Error del motor | log / mensaje |
| `ScannerEvent.PermissionPermanentlyDenied` | Usuario negó definitivo | la UI integrada ya ofrece ir a Ajustes |
| `ScannerEvent.Dismissed` | Botón cerrar del overlay | navegar atrás |

**No necesitas manejar el ciclo de vida**: al ir a background la cámara se libera y al volver se rearma sola.

---

## Recetas (combinaciones típicas)

### 1) Escanear uno, confirmar y repetir

```kotlin
var last by remember { mutableStateOf<ScanResult?>(null) }
val controller = rememberScannerController()

Box(Modifier.fillMaxSize()) {
    CodeScanner(
        onEvent = { if (it is ScannerEvent.Scanned) last = it.result },
        config = ScannerConfig(scanMode = ScanMode.SingleShot),
        controller = controller,
    )
    last?.let {
        Card(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            Text("Leído: ${it.rawValue}")
            Button(onClick = { last = null; controller.resume() }) { Text("Escanear otro") }
        }
    }
}
```

### 2) Picking / inventario (lote con dedupe)

```kotlin
QrScanner(
    onScanned = { items += it.rawValue },   // el mismo código no se repite por 2s
    formats = BarcodeFormat.ONE_DIMENSIONAL + BarcodeFormat.QrCode,
    scanMode = ScanMode.ContinuousDistinct(cooldownPerValue = 2.seconds),
    modifier = Modifier.size(300.dp).clip(RoundedCornerShape(24.dp)),
)
```

### 3) Solo códigos de barras 1D (viewfinder apaisado)

```kotlin
CodeScanner(
    onEvent = { ... },
    config = ScannerConfig(formats = BarcodeFormat.ONE_DIMENSIONAL),
    overlayConfig = OverlayConfig(shape = ViewfinderShape.Rectangle(aspectRatio = 2.5f)),
)
```

### 4) Overlay 100% tuyo

```kotlin
CodeScanner(
    onEvent = { ... },
    overlay = { state ->            // BoxScope.(ScannerUiState.Scanning)
        MiMarcoCustom()
        if (state.torchAvailable) MiBotonLinterna(encendida = state.torchOn)
        state.lastDetection?.let { MiAnimacionDeExito(it.cornerPoints) }
    },
)
// overlay = {}  ->  preview sin nada encima
```

### 5) UI de permisos propia

```kotlin
CodeScanner(
    onEvent = { ... },
    permissionContent = { canRequestAgain, request, openSettings ->
        MiPantallaPermiso(
            onAceptar = if (canRequestAgain) request else openSettings,
        )
    },
)
```

### 6) Cámara frontal + linterna al abrir + zoom inicial

```kotlin
config = ScannerConfig(
    camera = CameraConfig(lens = CameraLens.Front, torchOnStart = true, initialZoomRatio = 1.5f),
)
```

---

## iOS nativo (Swift)

```swift
import ScannerQrMp

let config = ScannerConfigBuilder()
config.formats = [BarcodeFormat.qrcode, BarcodeFormat.ean13]   // entries en minúsculas
config.singleShot = false
config.distinctCooldownMillis = 2000
config.showCloseButton = true
config.showSwitchCameraButton = true
config.soundFeedback = true
config.permissionRationale = "Necesitamos la cámara para escanear."

let vc = ScannerViewControllerKt.ScannerViewController(
    config: config,
    onScanned: { result in print(result.rawValue) },
    onError: { error in },
    onDismissRequest: { /* cerrar */ }
)
present(vc, animated: true)          // modal, o addChild(vc) para embeberlo
```

Wrapper SwiftUI listo para copiar:

```swift
struct ScannerScreen: UIViewControllerRepresentable {
    let onScanned: (String) -> Void
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        ScannerViewControllerKt.ScannerViewController(
            config: ScannerConfigBuilder(),
            onScanned: { onScanned($0.rawValue) },
            onError: { _ in },
            onDismissRequest: onDismiss
        )
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}
```

Todas las opciones del `ScannerConfigBuilder`: `formats`, `singleShot`, `distinctCooldownMillis`, `useFrontCamera`, `torchOnStart`, `pinchToZoom`, `tapToFocus`, `hapticFeedback`, `soundFeedback`, `showTorchButton`, `showCloseButton`, `showSwitchCameraButton`, `laserEnabled`, y los 5 textos de permisos/errores.

---

## Qué recibes en cada detección

```kotlin
ScanResult(
    rawValue: String,                    // el contenido decodificado
    format: BarcodeFormat,               // formato detectado
    cornerPoints: List<NormalizedPoint>, // esquinas en coords 0f..1f del preview
    timestampMillis: Long,               // epoch del frame
)
```

## Notas

- **Tamaño**: en Android el modelo bundled de ML Kit agrega ~3 MB (a cambio: funciona sin Google Play Services). En iOS el framework incluye el runtime de Compose (~10 MB); si tu app iOS ya usa Compose Multiplatform no se duplica.
- **Emuladores**: el detector en el emulador de Android solo reconoce códigos que ocupan gran parte del frame; en dispositivos reales la sensibilidad es la normal. El simulador de iOS no tiene cámara (verás el estado de error de la librería).
- ¿Encontraste un problema? Abre un issue con el formato del código y, si puedes, una foto del código que falla.
