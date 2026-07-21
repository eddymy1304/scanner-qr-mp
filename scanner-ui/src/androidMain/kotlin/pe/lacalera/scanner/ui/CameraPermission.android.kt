package pe.lacalera.scanner.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Composable
internal actual fun rememberCameraPermissionController(
    onResult: (granted: Boolean, canRequestAgain: Boolean) -> Unit,
): CameraPermissionController {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val currentOnResult by rememberUpdatedState(onResult)

    var permissionStatus by remember {
        mutableStateOf(
            if (isGranted(context)) CameraPermissionStatus.Granted else CameraPermissionStatus.NotDetermined,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // Tras una denegación, si ya no corresponde mostrar rationale => denegado permanente.
        val canRequestAgain = granted || activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false
        permissionStatus = when {
            granted -> CameraPermissionStatus.Granted
            canRequestAgain -> CameraPermissionStatus.Denied
            else -> CameraPermissionStatus.DeniedPermanently
        }
        currentOnResult(granted, canRequestAgain)
    }

    return remember {
        object : CameraPermissionController {
            override val status: CameraPermissionStatus get() = permissionStatus

            override fun request() {
                if (isGranted(context)) {
                    permissionStatus = CameraPermissionStatus.Granted
                    currentOnResult(true, true)
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            }

            override fun openSettings() {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
        }
    }
}

private fun isGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
