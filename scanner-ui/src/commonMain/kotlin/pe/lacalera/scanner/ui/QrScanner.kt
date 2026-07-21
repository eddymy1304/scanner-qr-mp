package pe.lacalera.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import pe.lacalera.scanner.core.config.ScannerConfig
import pe.lacalera.scanner.core.model.BarcodeFormat
import pe.lacalera.scanner.core.model.ScanMode
import pe.lacalera.scanner.core.model.ScanResult

/**
 * Scanner de una línea con defaults sensatos:
 * solo QR, [ScanMode.SingleShot] (detecta uno y deja de analizar), overlay con
 * viewfinder + láser, permisos integrados.
 *
 * ```
 * QrScanner(onScanned = { result -> println(result.rawValue) })
 * ```
 *
 * Fullscreen o embebido lo decide el [modifier]
 * (`Modifier.fillMaxSize()` vs `Modifier.size(280.dp)`).
 * Para configuración completa usa [CodeScanner].
 */
@Composable
public fun QrScanner(
    onScanned: (ScanResult) -> Unit,
    modifier: Modifier = Modifier,
    formats: Set<BarcodeFormat> = BarcodeFormat.QR_ONLY,
    scanMode: ScanMode = ScanMode.SingleShot,
) {
    val currentOnScanned by rememberUpdatedState(onScanned)
    CodeScanner(
        onEvent = { event ->
            if (event is ScannerEvent.Scanned) currentOnScanned(event.result)
        },
        modifier = modifier,
        config = remember(formats, scanMode) {
            ScannerConfig(formats = formats, scanMode = scanMode)
        },
    )
}
