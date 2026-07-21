package pe.lacalera.scanner.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import pe.lacalera.scanner.core.model.BarcodeFormat
import pe.lacalera.scanner.core.model.ScanMode
import pe.lacalera.scanner.core.model.ScanResult
import pe.lacalera.scanner.ui.QrScanner

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var scans by remember { mutableStateOf(listOf<ScanResult>()) }

            Box(modifier = Modifier.fillMaxSize()) {
                QrScanner(
                    onScanned = { result -> scans = (listOf(result) + scans).take(5) },
                    modifier = Modifier.fillMaxSize(),
                    formats = BarcodeFormat.ALL,
                    // Modo lote: no re-emite el mismo código durante 2s.
                    scanMode = ScanMode.ContinuousDistinct(cooldownPerValue = 2.seconds),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                ) {
                    Text("Escaneados (últimos 5):", color = Color.White)
                    scans.forEach { scan ->
                        Text(
                            text = "${scan.format}: ${scan.rawValue}",
                            color = Color.Green,
                        )
                    }
                }
            }
        }
    }
}
