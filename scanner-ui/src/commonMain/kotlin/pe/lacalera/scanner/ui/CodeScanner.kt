package pe.lacalera.scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.lacalera.scanner.core.config.ScannerConfig

/**
 * Scanner completamente configurable (nivel avanzado).
 *
 * - Fullscreen o embebido lo decide el [modifier] (`fillMaxSize()` vs `size(280.dp)`).
 * - [config] controla detección/cámara/feedback; [overlayConfig] lo visual del overlay default.
 * - [overlay] reemplaza TODO el overlay (pasar `{}` para no dibujar nada).
 * - [permissionContent] reemplaza la UI de permisos integrada.
 * - [controller] permite torch/zoom/pause/resume/switch desde fuera.
 *
 * Para el caso simple usa [QrScanner].
 */
@Composable
public fun CodeScanner(
    onEvent: (ScannerEvent) -> Unit,
    modifier: Modifier = Modifier,
    config: ScannerConfig = ScannerConfig(),
    overlayConfig: OverlayConfig = OverlayConfig(),
    controller: ScannerController = rememberScannerController(),
    overlay: (@Composable BoxScope.(ScannerUiState.Scanning) -> Unit)? = null,
    permissionContent: (
        @Composable (canRequestAgain: Boolean, request: () -> Unit, openSettings: () -> Unit) -> Unit
    )? = null,
) {
    val engineFactory = platformCameraEngineFactory()
    val keyHash = currentCompositeKeyHash
    val viewModel: ScannerViewModel = viewModel(key = "pe.lacalera.scanner#$keyHash") {
        ScannerViewModel(config, engineFactory)
    }
    val currentOnEvent by rememberUpdatedState(onEvent)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel, controller) {
        controller.attach(viewModel)
        onDispose { controller.detach(viewModel) }
    }
    LaunchedEffect(viewModel, controller) {
        viewModel.uiState.collect { controller.internalState.value = it }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event -> currentOnEvent(event) }
    }

    val permission = rememberCameraPermissionController { granted, canRequestAgain ->
        viewModel.onAction(ScannerAction.PermissionResult(granted, canRequestAgain))
    }
    LaunchedEffect(viewModel) {
        when (permission.status) {
            CameraPermissionStatus.Granted ->
                viewModel.onAction(ScannerAction.PermissionResult(granted = true, canRequestAgain = true))

            CameraPermissionStatus.NotDetermined -> permission.request()

            CameraPermissionStatus.Denied ->
                viewModel.onAction(ScannerAction.PermissionResult(granted = false, canRequestAgain = true))

            CameraPermissionStatus.DeniedPermanently ->
                viewModel.onAction(ScannerAction.PermissionResult(granted = false, canRequestAgain = false))
        }
    }

    // Lifecycle: al ir a background se libera la cámara; al volver se rearma.
    LifecycleStartEffect(viewModel) {
        viewModel.onAction(ScannerAction.Start)
        onStopOrDispose { viewModel.onAction(ScannerAction.Stop) }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state) {
            is ScannerUiState.Scanning -> {
                var previewModifier: Modifier = Modifier.matchParentSize()
                if (config.camera.pinchToZoom) {
                    previewModifier = previewModifier.pointerInput(viewModel) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val ratio = viewModel.engine.cameraState.value.zoomRatio * zoom
                            viewModel.onAction(ScannerAction.SetZoom(ratio))
                        }
                    }
                }
                if (config.camera.tapToFocus) {
                    previewModifier = previewModifier.pointerInput(viewModel) {
                        detectTapGestures { offset ->
                            viewModel.onAction(
                                ScannerAction.TapToFocus(
                                    x = offset.x / size.width,
                                    y = offset.y / size.height,
                                ),
                            )
                        }
                    }
                }
                CameraPreview(engine = viewModel.engine, modifier = previewModifier)

                if (overlay != null) {
                    overlay(current)
                } else {
                    DefaultScannerOverlay(
                        state = current,
                        config = overlayConfig,
                        onToggleTorch = { viewModel.onAction(ScannerAction.ToggleTorch) },
                        onClose = { viewModel.onAction(ScannerAction.Close) },
                    )
                }
            }

            is ScannerUiState.PermissionRequired -> {
                if (permissionContent != null) {
                    permissionContent(current.canRequestAgain, permission::request, permission::openSettings)
                } else {
                    DefaultPermissionContent(
                        canRequestAgain = current.canRequestAgain,
                        strings = config.strings,
                        onRequest = permission::request,
                        onOpenSettings = permission::openSettings,
                    )
                }
            }

            is ScannerUiState.Error -> DefaultErrorContent(current.error, config.strings)

            ScannerUiState.Initializing -> Unit // fondo negro
        }
    }
}
