package com.eddymy1304.scanner.core.model

/**
 * Errores que la librería reporta al consumidor.
 */
public sealed interface ScannerError {

    /** El usuario negó el permiso de cámara. */
    public data class PermissionDenied(val permanently: Boolean) : ScannerError

    /** No hay cámara disponible o falló su inicialización. */
    public data class CameraUnavailable(val message: String? = null) : ScannerError

    /** iOS: la app consumidora no declaró NSCameraUsageDescription en su Info.plist. */
    public data object MissingUsageDescription : ScannerError

    /** Fallo del motor de análisis (ML Kit / Vision). */
    public data class AnalysisFailure(val message: String? = null) : ScannerError
}
