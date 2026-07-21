package pe.lacalera.scanner.core.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pe.lacalera.scanner.core.config.CameraLens
import pe.lacalera.scanner.core.config.ScanRegion
import pe.lacalera.scanner.core.model.BarcodeFormat
import pe.lacalera.scanner.core.model.ScanResult
import pe.lacalera.scanner.core.model.ScannerError

/**
 * Contrato del motor de cámara + detección por plataforma.
 * Android: CameraX + ML Kit. iOS: AVFoundation + Vision.
 *
 * Regla de eficiencia: con [pauseAnalysis] activo, los frames se descartan
 * inmediatamente SIN invocar al motor ML; la preview sigue viva.
 */
public interface CameraEngine {

    /** Detecciones crudas (sin filtrar por [pe.lacalera.scanner.core.model.ScanMode]). */
    public val detections: Flow<ScanResult>

    /** Estado observable de la cámara (torch, zoom, lente, análisis pausado). */
    public val cameraState: StateFlow<CameraState>

    /** Errores del motor (cámara no disponible, fallo de análisis…). */
    public val errors: Flow<ScannerError>

    public fun start(config: CameraRuntimeConfig)

    /** Libera la sesión de cámara (llamar SIEMPRE al ir a background). */
    public fun stop()

    /** Detiene el análisis de frames sin apagar la preview. */
    public fun pauseAnalysis()

    public fun resumeAnalysis()

    public fun setTorch(enabled: Boolean)

    public fun setZoom(ratio: Float)

    /** Enfoca en el punto normalizado (0f..1f) del preview. */
    public fun focusAt(x: Float, y: Float)

    public fun switchLens(lens: CameraLens)
}

/** Estado observable de la cámara. */
public data class CameraState(
    val torchOn: Boolean = false,
    val torchAvailable: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val lens: CameraLens = CameraLens.Back,
    val isAnalysisPaused: Boolean = false,
)

/** Parámetros con los que arranca el motor. */
public data class CameraRuntimeConfig(
    val formats: Set<BarcodeFormat>,
    val lens: CameraLens = CameraLens.Back,
    val torchOnStart: Boolean = false,
    val initialZoomRatio: Float = 1f,
    val scanRegion: ScanRegion = ScanRegion.FullFrame,
)
