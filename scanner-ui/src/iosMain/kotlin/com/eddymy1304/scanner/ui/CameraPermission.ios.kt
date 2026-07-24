package com.eddymy1304.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
internal actual fun rememberCameraPermissionController(
    onResult: (granted: Boolean, canRequestAgain: Boolean) -> Unit,
): CameraPermissionController {
    val currentOnResult by rememberUpdatedState(onResult)

    var permissionStatus by remember { mutableStateOf(currentStatus()) }

    return remember {
        object : CameraPermissionController {
            override val status: CameraPermissionStatus get() = permissionStatus

            override fun request() {
                when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                    AVAuthorizationStatusAuthorized -> {
                        permissionStatus = CameraPermissionStatus.Granted
                        currentOnResult(true, true)
                    }

                    AVAuthorizationStatusNotDetermined -> {
                        if (!hasCameraUsageDescription()) {
                            // Sin NSCameraUsageDescription el request crashearía la app.
                            println(
                                "[scanner-qr-mp] Falta NSCameraUsageDescription en el Info.plist " +
                                    "de la app consumidora.",
                            )
                            permissionStatus = CameraPermissionStatus.DeniedPermanently
                            currentOnResult(false, false)
                            return
                        }
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                            dispatch_async(dispatch_get_main_queue()) {
                                permissionStatus = if (granted) {
                                    CameraPermissionStatus.Granted
                                } else {
                                    // iOS nunca re-pregunta: denegado => solo Ajustes.
                                    CameraPermissionStatus.DeniedPermanently
                                }
                                currentOnResult(granted, false)
                            }
                        }
                    }

                    else -> {
                        permissionStatus = CameraPermissionStatus.DeniedPermanently
                        currentOnResult(false, false)
                    }
                }
            }

            override fun openSettings() {
                val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
                UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
            }
        }
    }
}

private fun currentStatus(): CameraPermissionStatus =
    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> CameraPermissionStatus.Granted
        AVAuthorizationStatusNotDetermined -> CameraPermissionStatus.NotDetermined
        else -> CameraPermissionStatus.DeniedPermanently
    }

private fun hasCameraUsageDescription(): Boolean =
    NSBundle.mainBundle.objectForInfoDictionaryKey("NSCameraUsageDescription") != null
