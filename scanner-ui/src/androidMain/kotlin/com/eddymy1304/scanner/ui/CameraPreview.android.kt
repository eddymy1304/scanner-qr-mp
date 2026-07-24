package com.eddymy1304.scanner.ui

import androidx.camera.compose.CameraXViewfinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eddymy1304.scanner.camera.AndroidCameraEngine
import com.eddymy1304.scanner.camera.createCameraEngine
import com.eddymy1304.scanner.core.engine.CameraEngine

@Composable
internal actual fun platformCameraEngineFactory(): () -> CameraEngine {
    val appContext = LocalContext.current.applicationContext
    return remember { { createCameraEngine(appContext) } }
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
