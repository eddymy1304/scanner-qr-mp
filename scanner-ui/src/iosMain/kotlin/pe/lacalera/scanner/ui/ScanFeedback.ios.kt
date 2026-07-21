package pe.lacalera.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AudioToolbox.AudioServicesPlaySystemSound

@Composable
internal actual fun rememberBeepPlayer(): () -> Unit =
    remember { { AudioServicesPlaySystemSound(SCAN_BEEP_SOUND_ID) } }

/** SystemSoundID 1057 ("Tink"): beep corto estándar de confirmación. */
private const val SCAN_BEEP_SOUND_ID: UInt = 1057u
