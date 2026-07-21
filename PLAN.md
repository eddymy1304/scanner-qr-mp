# Plan — `scanner-qr-mp`

Librería KMP de escaneo QR/Barcode reusable en proyectos Android, iOS (Swift) y KMP.

## Decisiones

| Tema | Decisión |
|---|---|
| UI | Compose Multiplatform total; iOS nativo consume `ScannerViewController` (UIViewController) |
| Motor Android | CameraX 1.5 (`camera-compose` / `CameraXViewfinder`) + ML Kit barcode **bundled** (sin Play Services) |
| Motor iOS | AVFoundation (`AVCaptureSession`) + Vision (`VNDetectBarcodesRequest`), preview vía `UIKitView` |
| Distribución | Maven privado (GitHub Packages) + XCFramework vía SPM |
| Arquitectura | Clean + MVI/MVVM estilo NIA / Pokedex-Compose (convention plugins, UDF, StateFlow, sealed interfaces) |

## Módulos

- `scanner-core` — KMP puro: `ScanResult`, `ScannerConfig`, `ScanMode`, `DetectionFilter` (dedupe/debounce, testeable), interfaz `CameraEngine`.
- `scanner-camera` — expect/actual del motor por plataforma.
- `scanner-ui` — Composables (`QrScanner` simple / `CodeScanner` avanzado), `ScannerViewModel` MVI, overlay, permisos.
- `scanner` — Umbrella: exporta los 3, XCFramework `LaCaleraScanner`, `ScannerViewController` para Swift.
- `sample/composeApp` — Demo Android+iOS. `sample/iosApp` — Xcode Swift consumiendo el framework (Fase 5).

## Requisitos clave

- Fullscreen vs embebido lo decide el `Modifier` del caller (no es flag).
- Eficiencia: `STRATEGY_KEEP_ONLY_LATEST` / `alwaysDiscardsLateVideoFrames`, análisis pausable (SingleShot detiene el análisis, no la preview), dedupe por valor con cooldown, ROI recortado antes del motor ML.
- `ScanMode`: `SingleShot` | `Continuous(debounce)` | `ContinuousDistinct(cooldownPerValue)`.
- Configurable (MVP): formatos, overlay (shape/scrim/borde/láser), torch, tap-to-focus, pinch-to-zoom, lente + switch, haptic/sound, textos de permisos, success highlight, overlay 100% custom (slot).

## Fases

- [x] **Fase 0** — Scaffold: build-logic (convention plugins), catalog, módulos compilando Android + iOS.
- [x] **Fase 1** — Core: modelos, configs, `DetectionFilter` + tests (Turbine).
- [ ] **Fase 2** — Motor Android: CameraX analyzer manual + ML Kit bundled, `CameraPreview` con `CameraXViewfinder`.
- [ ] **Fase 3** — UI + MVI: `ScannerViewModel`, permisos Android, `DefaultScannerOverlay`, `QrScanner`/`CodeScanner`, `ScannerController`, lifecycle.
- [ ] **Fase 4** — Motor iOS: `AVCaptureSession` + Vision en Kotlin/Native, `UIKitView` preview, permisos iOS. Verificar en dispositivo físico.
- [ ] **Fase 5** — Umbrella iOS: `ScannerViewController`, `ScannerConfigBuilder`, SKIE, XCFramework, sample Swift.
- [ ] **Fase 6** — Features MVP: haptics/sonido, success highlight con corners, ROI, switch cámara, pinch/tap.
- [ ] **Fase 7** — Publicación: vanniktech maven-publish → GH Packages, Package.swift + checksum en CI, README, binary-compatibility-validator, `v0.1.0`.

## Verificación por fase

- F1: `./gradlew :scanner-core:allTests`
- F2/F3: sample Android — fullscreen y embebido 280.dp, torch, SingleShot+resume, background libera cámara, pausa = 0 invocaciones a ML Kit.
- F4: sample iOS en dispositivo físico, CPU < ~30 % en Instruments durante escaneo continuo.
- F5: app Swift recibe `ScanResult` con enums limpios (SKIE).
- F7: proyecto Android externo (GH Packages) + proyecto Xcode externo (SPM) consumiendo.
