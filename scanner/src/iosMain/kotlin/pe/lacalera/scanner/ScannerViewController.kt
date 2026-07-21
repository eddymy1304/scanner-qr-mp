package pe.lacalera.scanner

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import pe.lacalera.scanner.core.model.ScanResult
import pe.lacalera.scanner.core.model.ScannerError
import pe.lacalera.scanner.ui.CodeScanner
import pe.lacalera.scanner.ui.OverlayConfig
import pe.lacalera.scanner.ui.ScannerEvent
import platform.UIKit.UIViewController

/**
 * Entry point para apps iOS nativas (Swift/UIKit/SwiftUI):
 * un UIViewController listo para presentar modal o embeber como child.
 *
 * ```swift
 * let vc = ScannerViewControllerKt.ScannerViewController(
 *     config: builder,
 *     onScanned: { result in print(result.rawValue) },
 *     onError: { _ in },
 *     onDismissRequest: { self.dismiss(animated: true) }
 * )
 * ```
 */
public fun ScannerViewController(
    config: ScannerConfigBuilder,
    onScanned: (ScanResult) -> Unit,
    onError: (ScannerError) -> Unit,
    onDismissRequest: () -> Unit,
): UIViewController = ComposeUIViewController {
    CodeScanner(
        onEvent = { event ->
            when (event) {
                is ScannerEvent.Scanned -> onScanned(event.result)
                is ScannerEvent.Failed -> onError(event.error)
                ScannerEvent.PermissionPermanentlyDenied -> Unit // la UI integrada guía a Ajustes
                ScannerEvent.Dismissed -> onDismissRequest()
            }
        },
        modifier = Modifier.fillMaxSize(),
        config = config.buildConfig(),
        overlayConfig = OverlayConfig(
            showTorchButton = config.showTorchButton,
            showCloseButton = config.showCloseButton,
            laser = if (config.laserEnabled) pe.lacalera.scanner.ui.LaserConfig() else null,
        ),
    )
}

/** Sobrecarga con defaults (ObjC no exporta default args). */
public fun ScannerViewController(
    onScanned: (ScanResult) -> Unit,
    onDismissRequest: () -> Unit,
): UIViewController = ScannerViewController(
    config = ScannerConfigBuilder(),
    onScanned = onScanned,
    onError = {},
    onDismissRequest = onDismissRequest,
)
