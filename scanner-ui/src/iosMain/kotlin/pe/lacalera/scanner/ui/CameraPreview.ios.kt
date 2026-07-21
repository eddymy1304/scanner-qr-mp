package pe.lacalera.scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import pe.lacalera.scanner.camera.createCameraEngine
import pe.lacalera.scanner.core.engine.CameraEngine

@Composable
internal actual fun platformCameraEngineFactory(): () -> CameraEngine =
    remember { { createCameraEngine() } }

/**
 * Placeholder de Fase 3. En la Fase 4: UIKitView + AVCaptureVideoPreviewLayer.
 */
@Composable
public actual fun CameraPreview(
    engine: CameraEngine,
    modifier: Modifier,
) {
    Box(modifier = modifier.background(Color.Black))
}
