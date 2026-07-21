package pe.lacalera.scanner.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.lacalera.scanner.ui.QrScanner

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            QrScanner(modifier = Modifier.fillMaxSize())
        }
    }
}
