package com.eddymy1304.scanner.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Forma del viewfinder (la "ventana" de escaneo del overlay).
 * Las fracciones son relativas al lado menor del contenedor.
 */
public sealed interface ViewfinderShape {

    /** Cuadrado redondeado, el clásico para QR. */
    public data class RoundedSquare(
        val cornerRadius: Dp = 16.dp,
        val sizeFraction: Float = 0.7f,
    ) : ViewfinderShape

    /** Rectángulo apaisado, ideal para códigos de barras 1D. */
    public data class Rectangle(
        val cornerRadius: Dp = 16.dp,
        val widthFraction: Float = 0.85f,
        val aspectRatio: Float = 1.8f,
    ) : ViewfinderShape

    public data class Circle(
        val sizeFraction: Float = 0.7f,
    ) : ViewfinderShape

    /** Sin ventana: el scrim no se dibuja y se escanea el frame completo visible. */
    public data object None : ViewfinderShape
}

/** Línea "láser" animada dentro del viewfinder. */
public data class LaserConfig(
    val color: Color = Color(0xFFFF3B30),
    val strokeWidth: Dp = 2.dp,
    val sweepDuration: Duration = 2.seconds,
)

/**
 * Configuración visual del overlay por defecto ([DefaultScannerOverlay]).
 * Para control total, pasar un slot `overlay` custom a [CodeScanner].
 */
public data class OverlayConfig(
    val shape: ViewfinderShape = ViewfinderShape.RoundedSquare(),
    val scrimColor: Color = Color.Black.copy(alpha = 0.55f),
    val borderColor: Color = Color.White,
    val borderWidth: Dp = 3.dp,
    val laser: LaserConfig? = LaserConfig(),
    val showTorchButton: Boolean = true,
    val showCloseButton: Boolean = false,
    val showSwitchCameraButton: Boolean = false,
    val successHighlight: Boolean = true,
)
