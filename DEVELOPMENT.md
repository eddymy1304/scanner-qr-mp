# Desarrollo de scanner-qr-mp

Documentación interna. Los consumidores de la librería solo necesitan el [README](README.md).

## Arquitectura

Clean + MVI (referencias: Now in Android, Pokedex-Compose). UDF estricto: la UI emite `ScannerAction`, el estado sale por `StateFlow<ScannerUiState>` y los one-shot por `Flow<ScannerEvent>`.

```
Composable ──ScannerAction──▶ ScannerViewModel ──▶ CameraEngine (expect/actual)
    ▲                              │  ▲                  │ Flow<ScanResult> crudo
    │  StateFlow<ScannerUiState>   │  └── DetectionFilter ◀┘   (aplica ScanMode)
    └──────────────────────────────┘
          Channel<ScannerEvent> (one-shot al caller)
```

## Módulos

| Módulo | Contenido | Depende de |
|---|---|---|
| `scanner-core` | Dominio puro: modelos, configs, `ScanMode`, `DetectionFilter`, contrato `CameraEngine`. Testeable en JVM | coroutines |
| `scanner-camera` | Motores: `AndroidCameraEngine` (CameraX + ML Kit bundled) / `IosCameraEngine` (AVCaptureSession + Vision) | core |
| `scanner-ui` | Composables, `ScannerViewModel`, overlay, permisos, preview expect/actual | core, camera, CMP |
| `scanner` | Umbrella: `api()` de los 3, XCFramework `ScannerQrMp`, `ScannerViewController` para Swift | los 3 |
| `sample/composeApp` | Demo KMP (Android + iOS) | scanner |
| `sample/androidApp` | App Android del demo (AGP 9 no permite application+KMP en un módulo) | composeApp |
| `sample/iosApp` | App Swift consumiendo el XCFramework. Proyecto generado: `xcodegen generate` | XCFramework |

## Garantías de eficiencia (no romper)

- Android: `STRATEGY_KEEP_ONLY_LATEST`; el `ImageProxy` se cierra recién al completar ML Kit (throttling natural); con `pauseAnalysis` se cierra al instante sin invocar ML; ROI recorta YUV→NV21 **antes** del motor.
- iOS: `alwaysDiscardsLateVideoFrames = true`; Vision corre **síncrono** en la cola serial del delegate; con pausa el delegate retorna al instante; `regionOfInterest` nativo; `startRunning` jamás en main.

## Build y verificación

```bash
./gradlew :scanner-core:allTests                                  # tests core (JVM + iOS sim)
./gradlew :sample:androidApp:assembleDebug                        # demo Android
./gradlew :sample:composeApp:linkDebugFrameworkIosSimulatorArm64  # demo iOS (CMP)
./gradlew :scanner:assembleScannerQrMpReleaseXCFramework      # XCFramework
./gradlew apiCheck                                                # API pública (BCV, dumps en */api/)
./gradlew publishToMavenLocal                                     # ensayo de publicación
cd sample/iosApp && xcodegen generate                             # proyecto Xcode del sample Swift
```

Requisitos de entorno: JDK 21 para el daemon (lo aprovisiona solo el foojay resolver vía
`gradle/gradle-daemon-jvm.properties`), toolchain de compilación 17, Xcode con SDK iOS.

### Probar detección en el emulador Android

```bash
adb emu virtualscene-image wall /ruta/qr.png       # pone el QR en la escena virtual
adb emu automation play $ANDROID_SDK/emulator/resources/macros/Walk_to_image_room
```

La cámara virtual también se controla por gRPC del emulador (`setPhysicalModel` POSITION/ROTATION;
token en `~/Library/Caches/TemporaryItems/avd/running/pid_*.ini`). Ojo: el detector de ML Kit en el
emulador solo ve códigos que ocupan gran parte del frame — en dispositivo real no pasa.

## Release

Workflow **Release** (dispatch con la versión):
1. tests + `apiCheck`
2. publica los 4 módulos en GitHub Packages (`publishAllPublicationsToGitHubPackagesRepository`)
3. XCFramework Release → zip → `swift package compute-checksum`
4. actualiza `Package.swift` (url + checksum) y `VERSION_NAME`, commitea, tagea `vX.Y.Z` y crea el GitHub Release con el zip

Si cambia la API pública: `./gradlew apiDump` y commitear los `*.klib.api`.

## Pendientes conocidos

- **SKIE**: re-agregar en `scanner/build.gradle.kts` cuando soporte Kotlin 2.4.10 (mejora sealed→enums Swift y Flow→AsyncSequence).
- **Validación en iPhone físico**: detección en vivo, precisión de corners, torch/focus, CPU en Instruments (< ~30 % objetivo).
- **CoordinateMapper fino**: el mapeo de corners al preview no compensa el crop exacto del AspectFill (el highlight puede desviarse unos px en pantallas con aspect ratio muy distinto al del sensor).
