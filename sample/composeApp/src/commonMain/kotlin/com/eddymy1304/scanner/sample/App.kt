package com.eddymy1304.scanner.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import com.eddymy1304.scanner.core.config.ScannerConfig
import com.eddymy1304.scanner.core.model.BarcodeFormat
import com.eddymy1304.scanner.core.model.ScanMode
import com.eddymy1304.scanner.core.model.ScanResult
import com.eddymy1304.scanner.ui.CodeScanner
import com.eddymy1304.scanner.ui.OverlayConfig
import com.eddymy1304.scanner.ui.QrScanner
import com.eddymy1304.scanner.ui.ScannerEvent
import com.eddymy1304.scanner.ui.ScannerUiState
import com.eddymy1304.scanner.ui.ViewfinderShape
import com.eddymy1304.scanner.ui.rememberScannerController

private enum class Demo { FullscreenSingleShot, EmbeddedContinuous }

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var demo by remember { mutableStateOf<Demo?>(null) }
            when (demo) {
                null -> Menu(onSelect = { demo = it })
                Demo.FullscreenSingleShot -> FullscreenSingleShotDemo(onBack = { demo = null })
                Demo.EmbeddedContinuous -> EmbeddedContinuousDemo(onBack = { demo = null })
            }
        }
    }
}

@Composable
private fun Menu(onSelect: (Demo) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("scanner-qr-mp — demos", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { onSelect(Demo.FullscreenSingleShot) }, modifier = Modifier.fillMaxWidth()) {
            Text("Fullscreen · SingleShot (API simple + controller)")
        }
        Button(onClick = { onSelect(Demo.EmbeddedContinuous) }, modifier = Modifier.fillMaxWidth()) {
            Text("Embebido 300dp · ContinuousDistinct (API avanzada)")
        }
    }
}

/** API avanzada en fullscreen: SingleShot + botón para rearmar con el controller. */
@Composable
private fun FullscreenSingleShotDemo(onBack: () -> Unit) {
    var lastResult by remember { mutableStateOf<ScanResult?>(null) }
    val controller = rememberScannerController()
    val state by controller.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        CodeScanner(
            onEvent = { event ->
                when (event) {
                    is ScannerEvent.Scanned -> lastResult = event.result
                    ScannerEvent.Dismissed -> onBack()
                    else -> Unit
                }
            },
            modifier = Modifier.fillMaxSize(),
            config = ScannerConfig(scanMode = ScanMode.SingleShot),
            overlayConfig = OverlayConfig(showCloseButton = true),
            controller = controller,
        )

        val paused = (state as? ScannerUiState.Scanning)?.isAnalysisPaused == true
        if (paused || lastResult != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .safeDrawingPadding(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Resultado: ${lastResult?.rawValue ?: "—"}")
                    Text("Formato: ${lastResult?.format ?: "—"}")
                    Button(onClick = { lastResult = null; controller.resume() }) {
                        Text("Escanear otro")
                    }
                }
            }
        }
    }
}

/** API simple embebida: el Modifier decide el tamaño; modo lote con dedupe. */
@Composable
private fun EmbeddedContinuousDemo(onBack: () -> Unit) {
    var scans by remember { mutableStateOf(listOf<ScanResult>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Scanner embebido (300dp)", style = MaterialTheme.typography.titleMedium)

        QrScanner(
            onScanned = { result -> scans = (listOf(result) + scans).take(8) },
            modifier = Modifier.size(300.dp).clip(RoundedCornerShape(24.dp)),
            formats = BarcodeFormat.ALL,
            scanMode = ScanMode.ContinuousDistinct(cooldownPerValue = 2.seconds),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Escaneados:", color = Color.White)
            if (scans.isEmpty()) Text("—", color = Color.Gray)
            scans.forEach { Text("${it.format}: ${it.rawValue}", color = Color(0xFF34C759)) }
        }

        OutlinedButton(onClick = onBack) { Text("Volver") }
    }
}
