package pe.lacalera.scanner.camera

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pe.lacalera.scanner.core.config.CameraLens
import pe.lacalera.scanner.core.engine.CameraEngine
import pe.lacalera.scanner.core.engine.CameraRuntimeConfig
import pe.lacalera.scanner.core.engine.CameraState
import pe.lacalera.scanner.core.model.ScanResult
import pe.lacalera.scanner.core.model.ScannerError

public fun createCameraEngine(): CameraEngine = IosCameraEngine()

/**
 * Stub de Fase 2. La implementación real (AVCaptureSession + Vision) llega en la Fase 4.
 */
public class IosCameraEngine internal constructor() : CameraEngine {

    private val _detections = MutableSharedFlow<ScanResult>(extraBufferCapacity = 8)
    override val detections: Flow<ScanResult> = _detections.asSharedFlow()

    private val _cameraState = MutableStateFlow(CameraState())
    override val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _errors = MutableSharedFlow<ScannerError>(extraBufferCapacity = 4)
    override val errors: Flow<ScannerError> = _errors.asSharedFlow()

    override fun start(config: CameraRuntimeConfig) {
        // TODO(Fase 4): AVCaptureSession + VNDetectBarcodesRequest
    }

    override fun stop() {}

    override fun pauseAnalysis() {
        _cameraState.update { it.copy(isAnalysisPaused = true) }
    }

    override fun resumeAnalysis() {
        _cameraState.update { it.copy(isAnalysisPaused = false) }
    }

    override fun setTorch(enabled: Boolean) {}

    override fun setZoom(ratio: Float) {}

    override fun focusAt(x: Float, y: Float) {}

    override fun switchLens(lens: CameraLens) {}
}
