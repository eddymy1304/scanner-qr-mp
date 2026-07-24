package com.eddymy1304.scanner.camera

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.eddymy1304.scanner.core.config.CameraLens
import com.eddymy1304.scanner.core.config.ScanRegion
import com.eddymy1304.scanner.core.engine.CameraEngine
import com.eddymy1304.scanner.core.engine.CameraRuntimeConfig
import com.eddymy1304.scanner.core.engine.CameraState
import com.eddymy1304.scanner.core.model.NormalizedPoint
import com.eddymy1304.scanner.core.model.ScanResult
import com.eddymy1304.scanner.core.model.ScannerError
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset1280x720
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.exposurePointOfInterest
import platform.AVFoundation.exposurePointOfInterestSupported
import platform.AVFoundation.focusPointOfInterest
import platform.AVFoundation.focusPointOfInterestSupported
import platform.AVFoundation.hasTorch
import platform.AVFoundation.isFocusModeSupported
import platform.AVFoundation.isExposureModeSupported
import platform.AVFoundation.AVCaptureExposureModeAutoExpose
import platform.AVFoundation.AVCaptureFocusModeAutoFocus
import platform.AVFoundation.exposureMode
import platform.AVFoundation.focusMode
import platform.AVFoundation.position
import platform.AVFoundation.torchMode
import platform.AVFoundation.videoZoomFactor
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDeviceFormat
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.ImageIO.kCGImagePropertyOrientationUp
import platform.Vision.VNBarcodeObservation
import platform.Vision.VNDetectBarcodesRequest
import platform.Vision.VNImageRequestHandler
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import kotlin.concurrent.Volatile
import kotlin.math.min

public fun createCameraEngine(): CameraEngine = IosCameraEngine()

/**
 * Motor iOS: AVCaptureSession + Vision (VNDetectBarcodesRequest).
 *
 * Paridad de eficiencia con el motor Android:
 * - `alwaysDiscardsLateVideoFrames = true` == STRATEGY_KEEP_ONLY_LATEST.
 * - Vision se ejecuta SÍNCRONO en la cola serial del delegate: mientras un request
 *   está en curso, AVFoundation descarta los frames tardíos solo (sin cola).
 * - Con [pauseAnalysis] el delegate retorna al instante, sin invocar Vision.
 * - Preset 1280x720 (mismo sweet spot que Android).
 * - Configuración y start/stop SIEMPRE en una cola serial dedicada, nunca en main.
 *
 * PENDIENTE validar en dispositivo físico (el simulador no tiene cámara):
 * CPU en Instruments, precisión de corners, tap-to-focus, torch.
 */
@OptIn(ExperimentalForeignApi::class)
public class IosCameraEngine internal constructor() : CameraEngine {

    private val session = AVCaptureSession()

    /** Preview layer que consume la UIKitView de scanner-ui. */
    public val previewLayer: AVCaptureVideoPreviewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    private val sessionQueue = dispatch_queue_create("com.eddymy1304.scanner.session", null)
    private val videoQueue = dispatch_queue_create("com.eddymy1304.scanner.video", null)

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

    @Volatile
    private var paused = false

    @Volatile
    private var runtimeConfig: CameraRuntimeConfig? = null

    private var device: AVCaptureDevice? = null
    private var videoOutput: AVCaptureVideoDataOutput? = null
    private var configured = false

    // Retenido como propiedad: AVFoundation guarda referencia débil al delegate.
    private val sampleDelegate = SampleBufferDelegate()

    override fun start(config: CameraRuntimeConfig) {
        runtimeConfig = config
        paused = false
        dispatch_async(sessionQueue) {
            if (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) != AVAuthorizationStatusAuthorized) {
                _errors.tryEmit(ScannerError.PermissionDenied(permanently = false))
                return@dispatch_async
            }
            if (!configured) {
                configureSession(config)
            }
            if (!session.running) {
                // startRunning es bloqueante: por eso corre en sessionQueue, jamás en main.
                session.startRunning()
            }
            applyInitialControls(config)
            publishCameraState(analysisPaused = paused)
        }
    }

    override fun stop() {
        dispatch_async(sessionQueue) {
            if (session.running) session.stopRunning()
        }
    }

    override fun pauseAnalysis() {
        paused = true
        _cameraState.update { it.copy(isAnalysisPaused = true) }
    }

    override fun resumeAnalysis() {
        paused = false
        _cameraState.update { it.copy(isAnalysisPaused = false) }
    }

    override fun setTorch(enabled: Boolean) {
        dispatch_async(sessionQueue) {
            val dev = device ?: return@dispatch_async
            if (!dev.hasTorch) return@dispatch_async
            withDeviceLock(dev) {
                dev.torchMode = if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            }
            _cameraState.update { it.copy(torchOn = enabled) }
        }
    }

    override fun setZoom(ratio: Float) {
        dispatch_async(sessionQueue) {
            val dev = device ?: return@dispatch_async
            val state = _cameraState.value
            val clamped = ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
            withDeviceLock(dev) {
                dev.videoZoomFactor = clamped.toDouble()
            }
            _cameraState.update { it.copy(zoomRatio = clamped) }
        }
    }

    override fun focusAt(x: Float, y: Float) {
        // El punto llega normalizado en coords del preview; el preview layer lo
        // traduce a coords del sensor (respetando rotación y crop del AspectFill).
        dispatch_async(dispatch_get_main_queue()) {
            val layerBounds = previewLayer.bounds
            val layerPoint = layerBounds.useContents {
                CGPointMake(size.width * x.toDouble(), size.height * y.toDouble())
            }
            val devicePoint = previewLayer.captureDevicePointOfInterestForPoint(layerPoint)
            dispatch_async(sessionQueue) {
                val dev = device ?: return@dispatch_async
                withDeviceLock(dev) {
                    if (dev.focusPointOfInterestSupported &&
                        dev.isFocusModeSupported(AVCaptureFocusModeAutoFocus)
                    ) {
                        dev.focusPointOfInterest = devicePoint
                        dev.focusMode = AVCaptureFocusModeAutoFocus
                    }
                    if (dev.exposurePointOfInterestSupported &&
                        dev.isExposureModeSupported(AVCaptureExposureModeAutoExpose)
                    ) {
                        dev.exposurePointOfInterest = devicePoint
                        dev.exposureMode = AVCaptureExposureModeAutoExpose
                    }
                }
            }
        }
    }

    override fun switchLens(lens: CameraLens) {
        val config = runtimeConfig ?: return
        runtimeConfig = config.copy(lens = lens)
        dispatch_async(sessionQueue) {
            session.beginConfiguration()
            session.inputs.filterIsInstance<AVCaptureDeviceInput>().forEach { session.removeInput(it) }
            addInputFor(lens)
            session.commitConfiguration()
            publishCameraState(analysisPaused = paused)
        }
    }

    // -- interno (todo en sessionQueue) --

    private fun configureSession(config: CameraRuntimeConfig) {
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPreset1280x720

        addInputFor(config.lens)

        val output = AVCaptureVideoDataOutput().apply {
            // Equivalente directo de STRATEGY_KEEP_ONLY_LATEST: si Vision está
            // ocupado, el frame tardío se descarta, nunca se encola.
            alwaysDiscardsLateVideoFrames = true
            videoSettings = mapOf<Any?, Any?>(
                kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_420YpCbCr8BiPlanarFullRange,
            )
            setSampleBufferDelegate(sampleDelegate, videoQueue)
        }
        if (session.canAddOutput(output)) {
            session.addOutput(output)
            videoOutput = output
            // Buffers en portrait para que las esquinas normalizadas coincidan con el preview.
            output.connectionWithMediaType(AVMediaTypeVideo)?.let { connection ->
                if (connection.supportsVideoOrientation) {
                    connection.videoOrientation = AVCaptureVideoOrientationPortrait
                }
            }
        } else {
            _errors.tryEmit(ScannerError.CameraUnavailable("No se pudo agregar el video output"))
        }

        session.commitConfiguration()
        configured = true
    }

    private fun addInputFor(lens: CameraLens) {
        val position = when (lens) {
            CameraLens.Back -> AVCaptureDevicePositionBack
            CameraLens.Front -> AVCaptureDevicePositionFront
        }
        val dev = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            mediaType = AVMediaTypeVideo,
            position = position,
        ).devices.firstOrNull() as? AVCaptureDevice
        if (dev == null) {
            _errors.tryEmit(ScannerError.CameraUnavailable("Sin cámara para lente $lens"))
            return
        }
        val input = AVCaptureDeviceInput.deviceInputWithDevice(dev, null)
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
            device = dev
        } else {
            _errors.tryEmit(ScannerError.CameraUnavailable("No se pudo crear el input de cámara"))
        }
    }

    private fun applyInitialControls(config: CameraRuntimeConfig) {
        val dev = device ?: return
        withDeviceLock(dev) {
            if (config.torchOnStart && dev.hasTorch) dev.torchMode = AVCaptureTorchModeOn
            if (config.initialZoomRatio != 1f) {
                dev.videoZoomFactor = config.initialZoomRatio.toDouble()
                    .coerceAtMost(dev.activeFormat.videoMaxZoomFactor)
            }
        }
    }

    private fun publishCameraState(analysisPaused: Boolean) {
        val dev = device ?: return
        val maxZoom = min(dev.activeFormat.videoMaxZoomFactor, 10.0)
        _cameraState.update {
            it.copy(
                torchOn = dev.torchMode == AVCaptureTorchModeOn,
                torchAvailable = dev.hasTorch,
                zoomRatio = dev.videoZoomFactor.toFloat(),
                minZoomRatio = 1f,
                maxZoomRatio = maxZoom.toFloat(),
                lens = if (dev.position == AVCaptureDevicePositionFront) CameraLens.Front else CameraLens.Back,
                isAnalysisPaused = analysisPaused,
            )
        }
    }

    private inline fun withDeviceLock(dev: AVCaptureDevice, block: () -> Unit) {
        if (dev.lockForConfiguration(null)) {
            try {
                block()
            } finally {
                dev.unlockForConfiguration()
            }
        }
    }

    private fun handleSampleBuffer(sampleBuffer: CMSampleBufferRef?) {
        // Regla de eficiencia: pausado => retorno inmediato, Vision ni se entera.
        if (paused) return
        val config = runtimeConfig ?: return
        val pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) ?: return

        val request = VNDetectBarcodesRequest(completionHandler = null).apply {
            symbologies = config.formats.toVisionSymbologies()
            val region = config.scanRegion
            if (region is ScanRegion.Normalized) {
                // Vision usa origen abajo-izquierda; nuestra API arriba-izquierda.
                regionOfInterest = CGRectMake(
                    region.left.toDouble(),
                    (1.0 - region.top - region.height).toDouble(),
                    region.width.toDouble(),
                    region.height.toDouble(),
                )
            }
        }

        val handler = VNImageRequestHandler(
            cVPixelBuffer = pixelBuffer,
            orientation = kCGImagePropertyOrientationUp,
            options = emptyMap<Any?, Any>(),
        )

        // Síncrono a propósito: bloquea la cola del delegate y deja que
        // alwaysDiscardsLateVideoFrames descarte lo que llegue mientras tanto.
        val ok = handler.performRequests(listOf(request), null)
        if (!ok) {
            _errors.tryEmit(ScannerError.AnalysisFailure("Vision performRequests falló"))
            return
        }

        val timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong()
        request.results
            ?.filterIsInstance<VNBarcodeObservation>()
            ?.forEach { observation ->
                val value = observation.payloadStringValue ?: return@forEach
                val format = observation.symbology.toBarcodeFormat(value) ?: return@forEach
                _detections.tryEmit(
                    ScanResult(
                        rawValue = value,
                        format = format,
                        cornerPoints = observation.normalizedCorners(),
                        timestampMillis = timestamp,
                    ),
                )
            }
    }

    /** Esquinas Vision (origen abajo-izq) -> coords normalizadas del preview (origen arriba-izq). */
    private fun VNBarcodeObservation.normalizedCorners(): List<NormalizedPoint> = listOf(
        topLeft.toPreviewPoint(),
        topRight.toPreviewPoint(),
        bottomRight.toPreviewPoint(),
        bottomLeft.toPreviewPoint(),
    )

    private fun CValue<platform.CoreGraphics.CGPoint>.toPreviewPoint(): NormalizedPoint =
        useContents { NormalizedPoint(x = x.toFloat(), y = (1.0 - y).toFloat()) }

    private inner class SampleBufferDelegate :
        NSObject(),
        AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputSampleBuffer: CMSampleBufferRef?,
            fromConnection: AVCaptureConnection,
        ) {
            handleSampleBuffer(didOutputSampleBuffer)
        }
    }
}
