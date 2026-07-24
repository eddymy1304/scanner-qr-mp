package com.eddymy1304.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import com.eddymy1304.scanner.camera.IosCameraEngine
import com.eddymy1304.scanner.camera.createCameraEngine
import com.eddymy1304.scanner.core.engine.CameraEngine
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView

@Composable
internal actual fun platformCameraEngineFactory(): () -> CameraEngine =
    remember { { createCameraEngine() } }

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun CameraPreview(
    engine: CameraEngine,
    modifier: Modifier,
) {
    val iosEngine = engine as IosCameraEngine
    UIKitView(
        factory = { PreviewContainerView(iosEngine.previewLayer) },
        modifier = modifier,
    )
}

/**
 * UIView contenedor del [AVCaptureVideoPreviewLayer]: mantiene el layer del
 * tamaño de la vista en cada layout (rotaciones, resize de CMP, etc.).
 */
@OptIn(ExperimentalForeignApi::class)
private class PreviewContainerView(
    private val previewLayer: AVCaptureVideoPreviewLayer,
) : UIView(frame = CGRectZero.readValue()) {

    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        // Sin animación implícita al redimensionar el layer.
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        previewLayer.frame = bounds
        CATransaction.commit()
    }
}
