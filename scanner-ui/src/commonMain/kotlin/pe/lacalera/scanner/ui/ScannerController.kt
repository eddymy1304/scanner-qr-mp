package pe.lacalera.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Control imperativo del scanner desde fuera del composable:
 * torch, zoom, pausa/reanudar (rearmar un SingleShot), cambiar cámara.
 *
 * ```
 * val controller = rememberScannerController()
 * CodeScanner(onEvent = ..., controller = controller)
 * Button(onClick = { controller.resume() }) { Text("Escanear otro") }
 * ```
 */
@Stable
public class ScannerController internal constructor() {

    private var viewModel: ScannerViewModel? = null

    internal val internalState: MutableStateFlow<ScannerUiState> =
        MutableStateFlow(ScannerUiState.Initializing)

    /** Estado observable del scanner (espejo del ViewModel interno). */
    public val uiState: StateFlow<ScannerUiState> = internalState.asStateFlow()

    public fun toggleTorch() {
        viewModel?.onAction(ScannerAction.ToggleTorch)
    }

    public fun setZoom(ratio: Float) {
        viewModel?.onAction(ScannerAction.SetZoom(ratio))
    }

    public fun switchCamera() {
        viewModel?.onAction(ScannerAction.SwitchCamera)
    }

    /** Pausa el análisis de frames sin apagar la preview. */
    public fun pause() {
        viewModel?.onAction(ScannerAction.PauseScanning)
    }

    /** Reanuda el análisis (p.ej. para escanear otro código tras un SingleShot). */
    public fun resume() {
        viewModel?.onAction(ScannerAction.ResumeScanning)
    }

    internal fun attach(vm: ScannerViewModel) {
        viewModel = vm
    }

    internal fun detach(vm: ScannerViewModel) {
        if (viewModel === vm) viewModel = null
    }
}

@Composable
public fun rememberScannerController(): ScannerController =
    remember { ScannerController() }
