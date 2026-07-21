package pe.lacalera.scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import pe.lacalera.scanner.camera.cameraEngineName

/**
 * Placeholder de Fase 0 del composable simple.
 * La firma final (`onScanned: (ScanResult) -> Unit`) llega en la Fase 3.
 */
@Composable
public fun QrScanner(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Scanner placeholder · ${cameraEngineName()}",
            color = Color.White,
        )
    }
}
