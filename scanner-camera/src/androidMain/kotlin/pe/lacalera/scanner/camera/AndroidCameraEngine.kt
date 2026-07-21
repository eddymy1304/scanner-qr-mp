package pe.lacalera.scanner.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pe.lacalera.scanner.core.config.CameraLens
import pe.lacalera.scanner.core.engine.CameraEngine
import pe.lacalera.scanner.core.engine.CameraRuntimeConfig
import pe.lacalera.scanner.core.engine.CameraState
import pe.lacalera.scanner.core.model.NormalizedPoint
import pe.lacalera.scanner.core.model.ScanResult
import pe.lacalera.scanner.core.model.ScannerError
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Motor Android: CameraX + ML Kit Barcode Scanning (bundled).
 *
 * Eficiencia:
 * - `STRATEGY_KEEP_ONLY_LATEST`: nunca se encolan frames viejos.
 * - El frame se cierra recién al terminar ML Kit → el siguiente llega cuando hay capacidad
 *   real de análisis (throttling natural, sin cola).
 * - Con [pauseAnalysis] el frame se cierra al instante sin tocar ML Kit.
 */
public fun createCameraEngine(context: Context): CameraEngine = AndroidCameraEngine(context)

public class AndroidCameraEngine internal constructor(
    context: Context,
) : CameraEngine {

    private val appContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val _detections = MutableSharedFlow<ScanResult>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val detections: Flow<ScanResult> = _detections.asSharedFlow()

    private val _cameraState = MutableStateFlow(CameraState())
    override val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _errors = MutableSharedFlow<ScannerError>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val errors: Flow<ScannerError> = _errors.asSharedFlow()

    /** SurfaceRequest del use case Preview, consumido por `CameraXViewfinder` en scanner-ui. */
    private val _surfaceRequests = MutableStateFlow<SurfaceRequest?>(null)
    public val surfaceRequests: StateFlow<SurfaceRequest?> = _surfaceRequests.asStateFlow()

    private val lifecycleOwner = EngineLifecycleOwner()
    private val paused = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: androidx.camera.core.Camera? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var runtimeConfig: CameraRuntimeConfig? = null

    override fun start(config: CameraRuntimeConfig) {
        runtimeConfig = config
        paused.set(false)
        ProcessCameraProvider.getInstance(appContext).let { future ->
            future.addListener({
                runCatching { future.get() }
                    .onSuccess { provider ->
                        cameraProvider = provider
                        bind(provider, config)
                    }
                    .onFailure {
                        _errors.tryEmit(ScannerError.CameraUnavailable(it.message))
                    }
            }, mainExecutor)
        }
    }

    override fun stop() {
        mainExecutor.execute {
            lifecycleOwner.moveToCreated()
            cameraProvider?.unbindAll()
            camera = null
            _surfaceRequests.value = null
            barcodeScanner?.close()
            barcodeScanner = null
        }
    }

    override fun pauseAnalysis() {
        paused.set(true)
        _cameraState.update { it.copy(isAnalysisPaused = true) }
    }

    override fun resumeAnalysis() {
        paused.set(false)
        _cameraState.update { it.copy(isAnalysisPaused = false) }
    }

    override fun setTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    override fun setZoom(ratio: Float) {
        val state = _cameraState.value
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio))
    }

    override fun focusAt(x: Float, y: Float) {
        val point = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(x, y)
        camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
    }

    override fun switchLens(lens: CameraLens) {
        val config = runtimeConfig ?: return
        val provider = cameraProvider ?: return
        runtimeConfig = config.copy(lens = lens)
        mainExecutor.execute {
            provider.unbindAll()
            bind(provider, runtimeConfig ?: return@execute)
        }
    }

    // -- interno --

    private fun bind(provider: ProcessCameraProvider, config: CameraRuntimeConfig) {
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(config.formats.toMlKitFormats())
                .build(),
        ).also { barcodeScanner = it }

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { request -> _surfaceRequests.value = request }
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .build()
            .apply { setAnalyzer(analysisExecutor, BarcodeAnalyzer(scanner)) }

        val selector = when (config.lens) {
            CameraLens.Back -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraLens.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
        }

        runCatching {
            provider.unbindAll()
            lifecycleOwner.moveToResumed()
            val boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            camera = boundCamera
            observeCameraInfo(boundCamera, config)
            if (config.torchOnStart) boundCamera.cameraControl.enableTorch(true)
            if (config.initialZoomRatio != 1f) boundCamera.cameraControl.setZoomRatio(config.initialZoomRatio)
        }.onFailure {
            _errors.tryEmit(ScannerError.CameraUnavailable(it.message))
        }
    }

    private fun observeCameraInfo(camera: androidx.camera.core.Camera, config: CameraRuntimeConfig) {
        _cameraState.update {
            it.copy(
                lens = config.lens,
                torchAvailable = camera.cameraInfo.hasFlashUnit(),
                isAnalysisPaused = paused.get(),
            )
        }
        camera.cameraInfo.torchState.observe(lifecycleOwner) { torch ->
            _cameraState.update { it.copy(torchOn = torch == androidx.camera.core.TorchState.ON) }
        }
        camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoom ->
            _cameraState.update {
                it.copy(
                    zoomRatio = zoom.zoomRatio,
                    minZoomRatio = zoom.minZoomRatio,
                    maxZoomRatio = zoom.maxZoomRatio,
                )
            }
        }
    }

    @ExperimentalGetImage
    private inner class BarcodeAnalyzer(
        private val scanner: BarcodeScanner,
    ) : ImageAnalysis.Analyzer {

        override fun analyze(imageProxy: ImageProxy) {
            // Regla de eficiencia: pausado → cerrar el frame YA, sin invocar ML Kit.
            if (paused.get()) {
                imageProxy.close()
                return
            }
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }
            val rotation = imageProxy.imageInfo.rotationDegrees
            val input = InputImage.fromMediaImage(mediaImage, rotation)
            val frameTimestamp = System.currentTimeMillis()

            scanner.process(input)
                .addOnSuccessListener { barcodes ->
                    if (paused.get()) return@addOnSuccessListener
                    barcodes.forEach { barcode ->
                        val format = barcode.format.toBarcodeFormat() ?: return@forEach
                        val value = barcode.rawValue ?: return@forEach
                        _detections.tryEmit(
                            ScanResult(
                                rawValue = value,
                                format = format,
                                cornerPoints = barcode.cornerPoints
                                    ?.map { it.toNormalized(input, rotation) }
                                    .orEmpty(),
                                timestampMillis = frameTimestamp,
                            ),
                        )
                    }
                }
                .addOnFailureListener { _errors.tryEmit(ScannerError.AnalysisFailure(it.message)) }
                // Cerrar recién al completar: con KEEP_ONLY_LATEST esto limita el análisis
                // a la capacidad real del dispositivo, sin encolar frames.
                .addOnCompleteListener { imageProxy.close() }
        }

        private fun android.graphics.Point.toNormalized(
            input: InputImage,
            rotation: Int,
        ): NormalizedPoint {
            // Dimensiones del frame ya rotado (como lo ve el usuario).
            val (w, h) = if (rotation == 90 || rotation == 270) {
                input.height.toFloat() to input.width.toFloat()
            } else {
                input.width.toFloat() to input.height.toFloat()
            }
            return NormalizedPoint(x = x / w, y = y / h)
        }
    }

    /**
     * LifecycleOwner interno: el motor controla su propio ciclo con start/stop,
     * desacoplado del lifecycle de la UI (que lo maneja el ViewModel).
     */
    private class EngineLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry

        fun moveToResumed() {
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun moveToCreated() {
            if (registry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                registry.currentState = Lifecycle.State.CREATED
            }
        }
    }
}
