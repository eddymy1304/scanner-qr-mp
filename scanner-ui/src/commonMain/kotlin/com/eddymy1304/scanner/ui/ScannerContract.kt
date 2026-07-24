package com.eddymy1304.scanner.ui

import com.eddymy1304.scanner.core.config.CameraLens
import com.eddymy1304.scanner.core.model.ScanResult
import com.eddymy1304.scanner.core.model.ScannerError

/**
 * Contrato MVI del scanner: estado único observable, acciones de la UI
 * y eventos one-shot hacia el consumidor.
 */
public sealed interface ScannerUiState {

    /** Cámara aún no inicializada (arranque o detenida en background). */
    public data object Initializing : ScannerUiState

    /** Falta el permiso de cámara. */
    public data class PermissionRequired(
        val canRequestAgain: Boolean,
    ) : ScannerUiState

    /** Escaneando (cámara activa). */
    public data class Scanning(
        val torchOn: Boolean = false,
        val torchAvailable: Boolean = false,
        val zoomRatio: Float = 1f,
        val minZoomRatio: Float = 1f,
        val maxZoomRatio: Float = 1f,
        val lens: CameraLens = CameraLens.Back,
        val lastDetection: ScanResult? = null,
        val isAnalysisPaused: Boolean = false,
    ) : ScannerUiState

    public data class Error(
        val error: ScannerError,
    ) : ScannerUiState
}

/** Acciones que la UI (o el [ScannerController]) envía al ViewModel. */
public sealed interface ScannerAction {
    public data object Start : ScannerAction
    public data object Stop : ScannerAction
    public data class PermissionResult(
        val granted: Boolean,
        val canRequestAgain: Boolean,
    ) : ScannerAction

    public data object ToggleTorch : ScannerAction
    public data class SetZoom(val ratio: Float) : ScannerAction
    public data class TapToFocus(val x: Float, val y: Float) : ScannerAction
    public data object SwitchCamera : ScannerAction

    /** Pausa el análisis de frames (la preview sigue viva). */
    public data object PauseScanning : ScannerAction

    /** Rearma el análisis (p.ej. tras un [com.eddymy1304.scanner.core.model.ScanMode.SingleShot]). */
    public data object ResumeScanning : ScannerAction

    /** El usuario cerró el scanner (botón close del overlay). */
    public data object Close : ScannerAction
}

/** Eventos one-shot hacia el consumidor de la librería. */
public sealed interface ScannerEvent {
    public data class Scanned(val result: ScanResult) : ScannerEvent
    public data class Failed(val error: ScannerError) : ScannerEvent
    public data object PermissionPermanentlyDenied : ScannerEvent
    public data object Dismissed : ScannerEvent
}
