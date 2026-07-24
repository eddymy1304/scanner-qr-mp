package com.eddymy1304.scanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.eddymy1304.scanner.core.config.CameraLens
import com.eddymy1304.scanner.core.config.ScannerConfig
import com.eddymy1304.scanner.core.detection.DetectionFilter
import com.eddymy1304.scanner.core.engine.CameraEngine
import com.eddymy1304.scanner.core.engine.CameraRuntimeConfig
import com.eddymy1304.scanner.core.model.ScanMode
import com.eddymy1304.scanner.core.model.ScannerError

/**
 * ViewModel multiplataforma del scanner (MVI).
 *
 * UDF estricto: la UI solo emite [ScannerAction]; el estado sale por [uiState]
 * y los eventos one-shot por [events].
 */
public class ScannerViewModel internal constructor(
    private val config: ScannerConfig,
    engineFactory: () -> CameraEngine,
) : ViewModel() {

    internal val engine: CameraEngine = engineFactory()

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Initializing)
    public val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _events = Channel<ScannerEvent>(Channel.BUFFERED)
    public val events: Flow<ScannerEvent> = _events.receiveAsFlow()

    private var permissionGranted = false
    private var uiStarted = false
    private var cameraRunning = false
    private var detectionJob: Job? = null
    private var cameraStateJob: Job? = null
    private var errorsJob: Job? = null

    public fun onAction(action: ScannerAction) {
        when (action) {
            ScannerAction.Start -> {
                uiStarted = true
                if (permissionGranted) startCamera()
            }

            ScannerAction.Stop -> {
                uiStarted = false
                stopCamera()
            }

            is ScannerAction.PermissionResult -> {
                permissionGranted = action.granted
                if (action.granted) {
                    if (uiStarted) startCamera()
                } else {
                    _uiState.value = ScannerUiState.PermissionRequired(action.canRequestAgain)
                    if (!action.canRequestAgain) {
                        _events.trySend(ScannerEvent.PermissionPermanentlyDenied)
                    }
                }
            }

            ScannerAction.ToggleTorch ->
                engine.setTorch(!engine.cameraState.value.torchOn)

            is ScannerAction.SetZoom -> engine.setZoom(action.ratio)

            is ScannerAction.TapToFocus ->
                if (config.camera.tapToFocus) engine.focusAt(action.x, action.y)

            ScannerAction.SwitchCamera -> {
                val next = when (engine.cameraState.value.lens) {
                    CameraLens.Back -> CameraLens.Front
                    CameraLens.Front -> CameraLens.Back
                }
                engine.switchLens(next)
            }

            ScannerAction.PauseScanning -> engine.pauseAnalysis()

            ScannerAction.ResumeScanning -> {
                engine.resumeAnalysis()
                // Rearmar la colección: tras SingleShot el flujo filtrado ya completó.
                if (cameraRunning) launchDetection()
            }

            ScannerAction.Close -> _events.trySend(ScannerEvent.Dismissed)
        }
    }

    override fun onCleared() {
        engine.stop()
    }

    // -- interno --

    private fun startCamera() {
        if (cameraRunning) return
        cameraRunning = true
        engine.start(
            CameraRuntimeConfig(
                formats = config.formats,
                lens = config.camera.lens,
                torchOnStart = config.camera.torchOnStart,
                initialZoomRatio = config.camera.initialZoomRatio,
                scanRegion = config.camera.scanRegion,
            ),
        )
        _uiState.value = ScannerUiState.Scanning()
        observeCameraState()
        observeErrors()
        launchDetection()
    }

    private fun stopCamera() {
        cameraRunning = false
        detectionJob?.cancel()
        cameraStateJob?.cancel()
        errorsJob?.cancel()
        engine.stop()
        _uiState.value = ScannerUiState.Initializing
    }

    private fun launchDetection() {
        detectionJob?.cancel()
        detectionJob = viewModelScope.launch {
            DetectionFilter(config.scanMode).apply(engine.detections).collect { result ->
                // SingleShot: dejar de leer frames apenas hay resultado.
                if (config.scanMode is ScanMode.SingleShot) engine.pauseAnalysis()
                _uiState.update { state ->
                    if (state is ScannerUiState.Scanning) state.copy(lastDetection = result) else state
                }
                _events.trySend(ScannerEvent.Scanned(result))
            }
        }
    }

    private fun observeCameraState() {
        cameraStateJob?.cancel()
        cameraStateJob = viewModelScope.launch {
            engine.cameraState.collect { cam ->
                _uiState.update { state ->
                    if (state is ScannerUiState.Scanning) {
                        state.copy(
                            torchOn = cam.torchOn,
                            torchAvailable = cam.torchAvailable,
                            zoomRatio = cam.zoomRatio,
                            minZoomRatio = cam.minZoomRatio,
                            maxZoomRatio = cam.maxZoomRatio,
                            lens = cam.lens,
                            isAnalysisPaused = cam.isAnalysisPaused,
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun observeErrors() {
        errorsJob?.cancel()
        errorsJob = viewModelScope.launch {
            engine.errors.collect { error ->
                _events.trySend(ScannerEvent.Failed(error))
                if (error is ScannerError.CameraUnavailable) {
                    _uiState.value = ScannerUiState.Error(error)
                }
            }
        }
    }
}
