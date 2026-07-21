package pe.lacalera.scanner.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberBeepPlayer(): () -> Unit {
    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME) }.getOrNull()
    }
    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator?.release() }
    }
    return remember(toneGenerator) {
        { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MS) }
    }
}

private const val TONE_VOLUME = 85
private const val BEEP_DURATION_MS = 150
