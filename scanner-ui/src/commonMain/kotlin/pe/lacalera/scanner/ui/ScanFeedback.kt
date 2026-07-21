package pe.lacalera.scanner.ui

import androidx.compose.runtime.Composable

/**
 * Reproductor del beep de confirmación por plataforma.
 * (El feedback háptico usa LocalHapticFeedback de Compose, que ya es común.)
 */
@Composable
internal expect fun rememberBeepPlayer(): () -> Unit
