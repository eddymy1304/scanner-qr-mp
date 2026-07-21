package pe.lacalera.scanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.lacalera.scanner.core.config.ScannerStrings
import pe.lacalera.scanner.core.model.ScannerError

/** UI de permisos por defecto; los textos vienen de [ScannerStrings]. */
@Composable
internal fun DefaultPermissionContent(
    canRequestAgain: Boolean,
    strings: ScannerStrings,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = if (canRequestAgain) strings.permissionRationale else strings.permissionDeniedMessage,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        if (canRequestAgain) {
            Button(onClick = onRequest) { Text(strings.permissionRequestButton) }
        } else {
            Button(onClick = onOpenSettings) { Text(strings.openSettingsButton) }
        }
    }
}

@Composable
internal fun DefaultErrorContent(
    error: ScannerError,
    strings: ScannerStrings,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val message = when (error) {
            is ScannerError.CameraUnavailable -> strings.cameraUnavailableMessage
            else -> strings.cameraUnavailableMessage
        }
        Text(text = message, color = Color.White, textAlign = TextAlign.Center)
    }
}
