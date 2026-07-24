package com.eddymy1304.scanner.core.detection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import com.eddymy1304.scanner.core.model.ScanMode
import com.eddymy1304.scanner.core.model.ScanResult
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Aplica el [ScanMode] sobre el flujo crudo de detecciones del motor.
 * Lógica pura y testeable: no sabe de cámaras ni de UI.
 *
 * @param timeSource inyectable para tests ([kotlin.time.TestTimeSource]).
 */
public class DetectionFilter(
    private val mode: ScanMode,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) {

    /**
     * Transforma las detecciones crudas según el modo:
     * - [ScanMode.SingleShot]: emite la primera y completa el flujo.
     * - [ScanMode.Continuous]: throttle — emite y suprime durante `emitInterval`.
     * - [ScanMode.ContinuousDistinct]: no re-emite el mismo [ScanResult.dedupeKey]
     *   hasta cumplir `cooldownPerValue`; valores distintos pasan sin espera.
     */
    public fun apply(detections: Flow<ScanResult>): Flow<ScanResult> = when (mode) {
        is ScanMode.SingleShot -> detections.take(1)
        is ScanMode.Continuous -> detections.throttleFirst(mode.emitInterval)
        is ScanMode.ContinuousDistinct -> detections.distinctWithCooldown(mode.cooldownPerValue)
    }

    private fun Flow<ScanResult>.throttleFirst(interval: Duration): Flow<ScanResult> = flow {
        var lastEmit: ComparableTimeMark? = null
        collect { result ->
            val last = lastEmit
            if (last == null || last.elapsedNow() >= interval) {
                lastEmit = timeSource.markNow()
                emit(result)
            }
        }
    }

    private fun Flow<ScanResult>.distinctWithCooldown(cooldown: Duration): Flow<ScanResult> = flow {
        val lastEmitByKey = mutableMapOf<String, ComparableTimeMark>()
        collect { result ->
            val last = lastEmitByKey[result.dedupeKey]
            if (last == null || last.elapsedNow() >= cooldown) {
                lastEmitByKey[result.dedupeKey] = timeSource.markNow()
                emit(result)
            }
        }
    }
}
