package pe.lacalera.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.lacalera.scanner.core.engine.CameraEngine

/**
 * Crea (y recuerda) el motor de cámara de la plataforma.
 * Android: CameraX + ML Kit. iOS: AVFoundation + Vision.
 */
@Composable
public expect fun rememberCameraEngine(): CameraEngine

/**
 * Preview de la cámara del [engine]. No arranca ni detiene el motor:
 * eso lo orquesta el ViewModel/caller vía [CameraEngine.start]/[CameraEngine.stop].
 */
@Composable
public expect fun CameraPreview(
    engine: CameraEngine,
    modifier: Modifier = Modifier,
)
