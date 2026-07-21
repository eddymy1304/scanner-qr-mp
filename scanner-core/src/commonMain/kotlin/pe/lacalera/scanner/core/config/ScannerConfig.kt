package pe.lacalera.scanner.core.config

import pe.lacalera.scanner.core.model.BarcodeFormat
import pe.lacalera.scanner.core.model.ScanMode

/**
 * Configuración de detección y cámara del scanner.
 *
 * La configuración VISUAL (overlay, viewfinder, colores, láser) vive en `scanner-ui`
 * (`OverlayConfig`) porque este módulo no depende de Compose.
 */
public data class ScannerConfig(
    val formats: Set<BarcodeFormat> = BarcodeFormat.QR_ONLY,
    val scanMode: ScanMode = ScanMode.SingleShot,
    val camera: CameraConfig = CameraConfig(),
    val feedback: FeedbackConfig = FeedbackConfig(),
    val strings: ScannerStrings = ScannerStrings(),
)

public enum class CameraLens { Back, Front }

/**
 * Región del frame que se analiza, en coordenadas normalizadas (0f..1f).
 * Reducir la región reduce los píxeles que procesa el motor ML (menos CPU).
 */
public sealed interface ScanRegion {
    /** Analiza el frame completo. */
    public data object FullFrame : ScanRegion

    /** Analiza solo el rectángulo indicado (normalizado respecto al preview). */
    public data class Normalized(
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float,
    ) : ScanRegion
}

public data class CameraConfig(
    val lens: CameraLens = CameraLens.Back,
    val torchOnStart: Boolean = false,
    val pinchToZoom: Boolean = true,
    val tapToFocus: Boolean = true,
    val initialZoomRatio: Float = 1f,
    val scanRegion: ScanRegion = ScanRegion.FullFrame,
)

public data class FeedbackConfig(
    val haptic: Boolean = true,
    val sound: Boolean = false,
)

/**
 * Textos que muestra la UI integrada (permisos, errores). El consumidor los
 * reemplaza para localizarlos.
 */
public data class ScannerStrings(
    val permissionRationale: String = "Necesitamos acceso a la cámara para escanear códigos.",
    val permissionRequestButton: String = "Permitir cámara",
    val permissionDeniedMessage: String = "El permiso de cámara está desactivado. Actívalo en Ajustes.",
    val openSettingsButton: String = "Abrir ajustes",
    val cameraUnavailableMessage: String = "La cámara no está disponible.",
)
