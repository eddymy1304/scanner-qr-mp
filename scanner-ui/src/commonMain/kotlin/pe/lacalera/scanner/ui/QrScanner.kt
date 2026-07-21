package pe.lacalera.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import pe.lacalera.scanner.core.detection.DetectionFilter
import pe.lacalera.scanner.core.engine.CameraRuntimeConfig
import pe.lacalera.scanner.core.model.BarcodeFormat
import pe.lacalera.scanner.core.model.ScanMode
import pe.lacalera.scanner.core.model.ScanResult

/**
 * Composable simple: preview + detección con defaults sensatos.
 *
 * Fullscreen o embebido lo decide el [modifier] del caller
 * (`Modifier.fillMaxSize()` vs `Modifier.size(280.dp)`).
 *
 * NOTA Fase 2: versión provisional sin ViewModel/permisos/overlay (llegan en Fase 3).
 * El caller debe garantizar el permiso de cámara antes de componer esto.
 */
@Composable
public fun QrScanner(
    onScanned: (ScanResult) -> Unit,
    modifier: Modifier = Modifier,
    formats: Set<BarcodeFormat> = BarcodeFormat.QR_ONLY,
    scanMode: ScanMode = ScanMode.SingleShot,
) {
    val engine = rememberCameraEngine()
    val filter = remember(scanMode) { DetectionFilter(scanMode) }

    LaunchedEffect(engine, filter, formats) {
        engine.start(CameraRuntimeConfig(formats = formats))
        filter.apply(engine.detections).collect { result ->
            // SingleShot: dejar de analizar frames apenas hay resultado (la preview sigue viva).
            if (scanMode is ScanMode.SingleShot) engine.pauseAnalysis()
            onScanned(result)
        }
    }
    DisposableEffect(engine) {
        onDispose { engine.stop() }
    }

    CameraPreview(engine = engine, modifier = modifier)
}
