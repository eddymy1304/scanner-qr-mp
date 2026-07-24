package com.eddymy1304.scanner.core.detection

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import com.eddymy1304.scanner.core.model.BarcodeFormat
import com.eddymy1304.scanner.core.model.ScanMode
import com.eddymy1304.scanner.core.model.ScanResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class DetectionFilterTest {

    private fun qr(value: String) = ScanResult(rawValue = value, format = BarcodeFormat.QrCode)
    private fun ean(value: String) = ScanResult(rawValue = value, format = BarcodeFormat.Ean13)

    // ---- SingleShot ----

    @Test
    fun singleShot_emitsOnlyFirstDetectionAndCompletes() = runTest {
        val filter = DetectionFilter(ScanMode.SingleShot)

        val results = filter.apply(flowOf(qr("A"), qr("B"), qr("C"))).toList()

        assertEquals(listOf(qr("A")), results)
    }

    // ---- Continuous (throttle) ----

    @Test
    fun continuous_suppressesDetectionsWithinEmitInterval() = runTest {
        val time = TestTimeSource()
        val filter = DetectionFilter(ScanMode.Continuous(emitInterval = 500.milliseconds), time)
        val upstream = MutableSharedFlow<ScanResult>()

        filter.apply(upstream).test {
            upstream.emit(qr("A"))
            assertEquals(qr("A"), awaitItem())

            // Mismos frames dentro del intervalo: suprimidos (aunque el valor cambie).
            upstream.emit(qr("A"))
            upstream.emit(qr("B"))
            expectNoEvents()

            // Pasado el intervalo vuelve a emitir.
            time += 501.milliseconds
            upstream.emit(qr("C"))
            assertEquals(qr("C"), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun continuous_emitsFirstDetectionImmediately() = runTest {
        val time = TestTimeSource()
        val filter = DetectionFilter(ScanMode.Continuous(emitInterval = 5.seconds), time)
        val upstream = MutableSharedFlow<ScanResult>()

        filter.apply(upstream).test {
            upstream.emit(qr("A"))
            assertEquals(qr("A"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- ContinuousDistinct (dedupe con cooldown) ----

    @Test
    fun continuousDistinct_doesNotReEmitSameValueDuringCooldown() = runTest {
        val time = TestTimeSource()
        val filter = DetectionFilter(ScanMode.ContinuousDistinct(cooldownPerValue = 2.seconds), time)
        val upstream = MutableSharedFlow<ScanResult>()

        filter.apply(upstream).test {
            upstream.emit(qr("A"))
            assertEquals(qr("A"), awaitItem())

            // El mismo código en frames consecutivos NO se re-emite.
            upstream.emit(qr("A"))
            upstream.emit(qr("A"))
            expectNoEvents()

            // Un código distinto pasa sin espera.
            upstream.emit(qr("B"))
            assertEquals(qr("B"), awaitItem())

            // Cumplido el cooldown, el primero puede re-emitirse.
            time += 2001.milliseconds
            upstream.emit(qr("A"))
            assertEquals(qr("A"), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun continuousDistinct_sameValueDifferentFormatIsDistinct() = runTest {
        val time = TestTimeSource()
        val filter = DetectionFilter(ScanMode.ContinuousDistinct(cooldownPerValue = 2.seconds), time)
        val upstream = MutableSharedFlow<ScanResult>()

        filter.apply(upstream).test {
            upstream.emit(qr("123"))
            assertEquals(qr("123"), awaitItem())

            // Mismo contenido pero otro formato: clave distinta, se emite.
            upstream.emit(ean("123"))
            assertEquals(ean("123"), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
