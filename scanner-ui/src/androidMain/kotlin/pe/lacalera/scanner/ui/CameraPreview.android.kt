package pe.lacalera.scanner.ui

import androidx.camera.compose.CameraXViewfinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.lacalera.scanner.camera.AndroidCameraEngine
import pe.lacalera.scanner.camera.createCameraEngine
import pe.lacalera.scanner.core.engine.CameraEngine

@Composable
public actual fun rememberCameraEngine(): CameraEngine {
    val context = LocalContext.current
    return remember { createCameraEngine(context) }
}

@Composable
public actual fun CameraPreview(
    engine: CameraEngine,
    modifier: Modifier,
) {
    val androidEngine = engine as AndroidCameraEngine
    val surfaceRequest by androidEngine.surfaceRequests.collectAsStateWithLifecycle()
    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            modifier = modifier,
        )
    }
}
