package pe.lacalera.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.lacalera.scanner.core.engine.CameraEngine

/**
 * Fábrica del motor de cámara de la plataforma (capturando el contexto necesario).
 * Android: CameraX + ML Kit. iOS: AVFoundation + Vision.
 */
@Composable
internal expect fun platformCameraEngineFactory(): () -> CameraEngine

/**
 * Preview de la cámara del [engine]. No arranca ni detiene el motor:
 * eso lo orquesta el [ScannerViewModel] vía [CameraEngine.start]/[CameraEngine.stop].
 */
@Composable
public expect fun CameraPreview(
    engine: CameraEngine,
    modifier: Modifier = Modifier,
)
