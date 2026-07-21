package pe.lacalera.scanner.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Overlay por defecto: scrim con ventana (viewfinder), borde, láser animado,
 * highlight de detección y botones de torch/cerrar.
 *
 * Para un diseño 100% propio, pasar un slot `overlay` custom a [CodeScanner].
 */
@Composable
public fun BoxScope.DefaultScannerOverlay(
    state: ScannerUiState.Scanning,
    config: OverlayConfig,
    onToggleTorch: () -> Unit,
    onClose: () -> Unit,
) {
    val laserProgress = config.laser?.let { laser ->
        rememberInfiniteTransition(label = "laser").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = laser.sweepDuration.inWholeMilliseconds.toInt(),
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "laserProgress",
        )
    }

    Canvas(
        modifier = Modifier
            .matchParentSize()
            // Necesario para que BlendMode.Clear recorte el scrim y no pinte negro.
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val shape = config.shape
        if (shape is ViewfinderShape.None) return@Canvas
        val rect = viewfinderRect(shape) ?: return@Canvas

        drawRect(color = config.scrimColor)

        val cornerRadius = when (shape) {
            is ViewfinderShape.RoundedSquare -> CornerRadius(shape.cornerRadius.toPx())
            is ViewfinderShape.Rectangle -> CornerRadius(shape.cornerRadius.toPx())
            else -> CornerRadius.Zero
        }

        // Ventana transparente.
        when (shape) {
            is ViewfinderShape.Circle -> drawCircle(
                color = Color.Transparent,
                radius = rect.width / 2f,
                center = rect.center,
                blendMode = BlendMode.Clear,
            )

            else -> drawRoundRect(
                color = Color.Transparent,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = cornerRadius,
                blendMode = BlendMode.Clear,
            )
        }

        // Borde.
        if (config.borderWidth > 0.dp) {
            val stroke = Stroke(width = config.borderWidth.toPx())
            when (shape) {
                is ViewfinderShape.Circle -> drawCircle(
                    color = config.borderColor,
                    radius = rect.width / 2f,
                    center = rect.center,
                    style = stroke,
                )

                else -> drawRoundRect(
                    color = config.borderColor,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = cornerRadius,
                    style = stroke,
                )
            }
        }

        // Láser animado (se apaga con el análisis pausado).
        val laser = config.laser
        if (laser != null && laserProgress != null && !state.isAnalysisPaused) {
            val y = rect.top + rect.height * laserProgress.value
            val inset = rect.width * 0.06f
            drawLine(
                color = laser.color,
                start = Offset(rect.left + inset, y),
                end = Offset(rect.right - inset, y),
                strokeWidth = laser.strokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }

    // Highlight del último código detectado (esquinas normalizadas -> px).
    if (config.successHighlight) {
        val corners = state.lastDetection?.cornerPoints ?: emptyList()
        if (corners.size >= 4) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val path = Path().apply {
                    corners.forEachIndexed { index, point ->
                        val x = point.x * size.width
                        val y = point.y * size.height
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0xFF34C759),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }

    if (config.showCloseButton) {
        OverlayCircleButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val stroke = 2.dp.toPx()
                drawLine(Color.White, Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
            }
        }
    }

    if (config.showTorchButton && state.torchAvailable) {
        OverlayCircleButton(
            onClick = onToggleTorch,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawLightningBolt(if (state.torchOn) Color(0xFFFFD60A) else Color.White)
            }
        }
    }
}

@Composable
private fun OverlayCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Rect del viewfinder según la forma, centrado en el canvas. */
private fun DrawScope.viewfinderRect(shape: ViewfinderShape): Rect? {
    val minSide = min(size.width, size.height)
    return when (shape) {
        is ViewfinderShape.RoundedSquare -> {
            val side = minSide * shape.sizeFraction
            Rect(offset = Offset((size.width - side) / 2f, (size.height - side) / 2f), size = Size(side, side))
        }

        is ViewfinderShape.Rectangle -> {
            val width = size.width * shape.widthFraction
            val height = (width / shape.aspectRatio).coerceAtMost(size.height * 0.9f)
            Rect(
                offset = Offset((size.width - width) / 2f, (size.height - height) / 2f),
                size = Size(width, height),
            )
        }

        is ViewfinderShape.Circle -> {
            val diameter = minSide * shape.sizeFraction
            Rect(
                offset = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f),
                size = Size(diameter, diameter),
            )
        }

        ViewfinderShape.None -> null
    }
}

private fun DrawScope.drawLightningBolt(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.62f, 0f)
        lineTo(0f, h * 0.58f)
        lineTo(w * 0.40f, h * 0.58f)
        lineTo(w * 0.34f, h)
        lineTo(w, h * 0.40f)
        lineTo(w * 0.56f, h * 0.40f)
        close()
    }
    drawPath(path, color)
}
