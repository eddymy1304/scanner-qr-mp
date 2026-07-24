package com.eddymy1304.scanner.core.model

/**
 * Punto normalizado en coordenadas del preview: (0f,0f) = esquina superior izquierda,
 * (1f,1f) = esquina inferior derecha. Independiente de la densidad y del tamaño real.
 */
public data class NormalizedPoint(
    val x: Float,
    val y: Float,
)

/**
 * Resultado de una detección.
 *
 * @property rawValue contenido decodificado del código.
 * @property format formato detectado.
 * @property cornerPoints esquinas del código en coordenadas normalizadas del preview
 * (vacío si el motor no las reporta). Útil para el success highlight.
 * @property timestampMillis epoch millis del frame en que se detectó (lo estampa el motor).
 */
public data class ScanResult(
    val rawValue: String,
    val format: BarcodeFormat,
    val cornerPoints: List<NormalizedPoint> = emptyList(),
    val timestampMillis: Long = 0L,
) {
    /** Clave de de-duplicación: mismo contenido + mismo formato = mismo código. */
    public val dedupeKey: String get() = "$format:$rawValue"
}
