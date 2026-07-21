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
import pe.lacalera.scanner.core.config.ScanRegion
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
            .apply { setAnalyzer(analysisExecutor, BarcodeAnalyzer(scanner, config.scanRegion)) }

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
        private val scanRegion: ScanRegion,
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
            val frameTimestamp = System.currentTimeMillis()

            // ROI: recortar ANTES de invocar ML Kit = menos píxeles, menos CPU.
            val region = scanRegion as? ScanRegion.Normalized
            val input = region?.let { mediaImage.cropForRegion(it, rotation) }
                ?: InputImage.fromMediaImage(mediaImage, rotation)
            val effectiveRegion = if (input.width != mediaImage.width || input.height != mediaImage.height) {
                region
            } else {
                null // fallback a frame completo (región inválida o formato no soportado)
            }

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
                                    ?.map { it.toNormalized(input.width, input.height, rotation, effectiveRegion) }
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
            imageWidth: Int,
            imageHeight: Int,
            rotation: Int,
            region: ScanRegion.Normalized?,
        ): NormalizedPoint {
            // Dimensiones del frame ya rotado (como lo ve el usuario).
            val (w, h) = if (rotation == 90 || rotation == 270) {
                imageHeight.toFloat() to imageWidth.toFloat()
            } else {
                imageWidth.toFloat() to imageHeight.toFloat()
            }
            val local = NormalizedPoint(x = x / w, y = y / h)
            // Si se analizó un recorte, remapear del recorte al frame completo.
            return if (region != null) {
                NormalizedPoint(
                    x = region.left + local.x * region.width,
                    y = region.top + local.y * region.height,
                )
            } else {
                local
            }
        }

        /**
         * Recorta la región (dada en coords upright del preview) del buffer YUV
         * y la empaqueta como NV21 para ML Kit. Devuelve null si no se puede
         * (formato inesperado o región minúscula) => se analiza el frame completo.
         */
        private fun android.media.Image.cropForRegion(
            region: ScanRegion.Normalized,
            rotation: Int,
        ): InputImage? {
            if (planes.size != 3) return null

            // Región upright -> rect normalizado en coords del buffer (pre-rotación).
            val b = when (rotation) {
                90 -> floatArrayOf(region.top, 1f - region.left - region.width, region.height, region.width)
                180 -> floatArrayOf(
                    1f - region.left - region.width,
                    1f - region.top - region.height,
                    region.width,
                    region.height,
                )
                270 -> floatArrayOf(1f - region.top - region.height, region.left, region.height, region.width)
                else -> floatArrayOf(region.left, region.top, region.width, region.height)
            }
            // Coordenadas pares (el chroma está submuestreado 2x).
            val cropLeft = ((b[0] * width).toInt().coerceIn(0, width - 2)) and -2
            val cropTop = ((b[1] * height).toInt().coerceIn(0, height - 2)) and -2
            val cropW = ((b[2] * width).toInt().coerceAtMost(width - cropLeft)) and -2
            val cropH = ((b[3] * height).toInt().coerceAtMost(height - cropTop)) and -2
            if (cropW < MIN_CROP_SIZE || cropH < MIN_CROP_SIZE) return null

            val nv21 = ByteArray(cropW * cropH * 3 / 2)
            var pos = 0

            // Plano Y.
            val yPlane = planes[0]
            val yBuf = yPlane.buffer
            val yRowStride = yPlane.rowStride
            val yPixStride = yPlane.pixelStride
            val yRow = ByteArray(cropW)
            for (row in 0 until cropH) {
                val base = (cropTop + row) * yRowStride + cropLeft * yPixStride
                if (yPixStride == 1) {
                    yBuf.position(base)
                    yBuf.get(yRow, 0, cropW)
                    yRow.copyInto(nv21, pos)
                    pos += cropW
                } else {
                    var idx = base
                    repeat(cropW) {
                        nv21[pos++] = yBuf.get(idx)
                        idx += yPixStride
                    }
                }
            }

            // Chroma intercalado VU (NV21). planes[1]=U, planes[2]=V, submuestreo 2x.
            val uPlane = planes[1]
            val vPlane = planes[2]
            val uBuf = uPlane.buffer
            val vBuf = vPlane.buffer
            val cLeft = cropLeft / 2
            val cTop = cropTop / 2
            for (row in 0 until cropH / 2) {
                var vIdx = (cTop + row) * vPlane.rowStride + cLeft * vPlane.pixelStride
                var uIdx = (cTop + row) * uPlane.rowStride + cLeft * uPlane.pixelStride
                repeat(cropW / 2) {
                    nv21[pos++] = vBuf.get(vIdx)
                    nv21[pos++] = uBuf.get(uIdx)
                    vIdx += vPlane.pixelStride
                    uIdx += uPlane.pixelStride
                }
            }

            return InputImage.fromByteArray(nv21, cropW, cropH, rotation, InputImage.IMAGE_FORMAT_NV21)
        }
    }

    private companion object {
        const val MIN_CROP_SIZE = 64
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
