package pe.lacalera.scanner

import pe.lacalera.scanner.core.config.CameraConfig
import pe.lacalera.scanner.core.config.CameraLens
import pe.lacalera.scanner.core.config.FeedbackConfig
import pe.lacalera.scanner.core.config.ScannerConfig
import pe.lacalera.scanner.core.config.ScannerStrings
import pe.lacalera.scanner.core.model.BarcodeFormat
import pe.lacalera.scanner.core.model.ScanMode
import kotlin.time.Duration.Companion.milliseconds

/**
 * Builder Swift-friendly de la configuración del scanner.
 * (Los default args y Duration de Kotlin no viajan bien a ObjC/Swift.)
 *
 * ```swift
 * let builder = ScannerConfigBuilder()
 * builder.formats = [.qrCode, .ean13]
 * builder.singleShot = false
 * builder.distinctCooldownMillis = 2000
 * ```
 */
public class ScannerConfigBuilder {

    /** Formatos aceptados. Default: solo QR. */
    public var formats: List<BarcodeFormat> = listOf(BarcodeFormat.QrCode)

    /** `true` = detecta uno y pausa (default). `false` = continuo con dedupe. */
    public var singleShot: Boolean = true

    /** Cooldown por valor en modo continuo (ignorado si [singleShot]). */
    public var distinctCooldownMillis: Long = 2_000

    public var useFrontCamera: Boolean = false
    public var torchOnStart: Boolean = false
    public var pinchToZoom: Boolean = true
    public var tapToFocus: Boolean = true

    public var hapticFeedback: Boolean = true
    public var soundFeedback: Boolean = false

    // Overlay
    public var showTorchButton: Boolean = true
    public var showCloseButton: Boolean = true
    public var laserEnabled: Boolean = true

    // Textos (localizables por la app consumidora)
    public var permissionRationale: String = ScannerStrings().permissionRationale
    public var permissionRequestButton: String = ScannerStrings().permissionRequestButton
    public var permissionDeniedMessage: String = ScannerStrings().permissionDeniedMessage
    public var openSettingsButton: String = ScannerStrings().openSettingsButton
    public var cameraUnavailableMessage: String = ScannerStrings().cameraUnavailableMessage

    internal fun buildConfig(): ScannerConfig = ScannerConfig(
        formats = formats.toSet(),
        scanMode = if (singleShot) {
            ScanMode.SingleShot
        } else {
            ScanMode.ContinuousDistinct(cooldownPerValue = distinctCooldownMillis.milliseconds)
        },
        camera = CameraConfig(
            lens = if (useFrontCamera) CameraLens.Front else CameraLens.Back,
            torchOnStart = torchOnStart,
            pinchToZoom = pinchToZoom,
            tapToFocus = tapToFocus,
        ),
        feedback = FeedbackConfig(haptic = hapticFeedback, sound = soundFeedback),
        strings = ScannerStrings(
            permissionRationale = permissionRationale,
            permissionRequestButton = permissionRequestButton,
            permissionDeniedMessage = permissionDeniedMessage,
            openSettingsButton = openSettingsButton,
            cameraUnavailableMessage = cameraUnavailableMessage,
        ),
    )
}
