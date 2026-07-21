package pe.lacalera.scanner.ui

import androidx.compose.runtime.Composable

public enum class CameraPermissionStatus {
    Granted,
    NotDetermined,
    Denied,
    DeniedPermanently,
}

/** Manejo del permiso de cámara por plataforma. */
public interface CameraPermissionController {
    public val status: CameraPermissionStatus
    public fun request()
    public fun openSettings()
}

/**
 * Crea el controller de permiso de la plataforma.
 * [onResult] se invoca tras cada intento de request con (granted, canRequestAgain).
 */
@Composable
internal expect fun rememberCameraPermissionController(
    onResult: (granted: Boolean, canRequestAgain: Boolean) -> Unit,
): CameraPermissionController
