package pe.lacalera.scanner.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Estrategia de emisión de resultados. Controla que NUNCA se lea de más:
 * el análisis se pausa o los duplicados se suprimen según el modo.
 */
public sealed interface ScanMode {

    /**
     * Detecta un único código y pausa el análisis de frames
     * (la preview sigue viva; se rearma con `ScannerController.resume()`).
     * Es el default del composable simple.
     */
    public data object SingleShot : ScanMode

    /**
     * Emite continuamente, con un intervalo mínimo entre emisiones
     * (throttle: emite el primero al instante y suprime los siguientes durante [emitInterval]).
     */
    public data class Continuous(
        val emitInterval: Duration = 500.milliseconds,
    ) : ScanMode

    /**
     * Emite continuamente pero no re-emite el MISMO código (valor+formato)
     * hasta que pase [cooldownPerValue]. Códigos distintos se emiten sin espera.
     * Ideal para escaneo de lotes (picking, inventario).
     */
    public data class ContinuousDistinct(
        val cooldownPerValue: Duration = 2.seconds,
    ) : ScanMode
}
