package com.eddymy1304.scanner.camera

import platform.Vision.VNBarcodeSymbology
import platform.Vision.VNBarcodeSymbologyAztec
import platform.Vision.VNBarcodeSymbologyCodabar
import platform.Vision.VNBarcodeSymbologyCode128
import platform.Vision.VNBarcodeSymbologyCode39
import platform.Vision.VNBarcodeSymbologyCode93
import platform.Vision.VNBarcodeSymbologyDataMatrix
import platform.Vision.VNBarcodeSymbologyEAN13
import platform.Vision.VNBarcodeSymbologyEAN8
import platform.Vision.VNBarcodeSymbologyITF14
import platform.Vision.VNBarcodeSymbologyI2of5
import platform.Vision.VNBarcodeSymbologyPDF417
import platform.Vision.VNBarcodeSymbologyQR
import platform.Vision.VNBarcodeSymbologyUPCE
import com.eddymy1304.scanner.core.model.BarcodeFormat

/**
 * Mapeo [BarcodeFormat] -> simbologías de Vision.
 * Nota: Vision NO tiene UPC-A propio; los reporta como EAN-13 con "0" inicial.
 */
internal fun Set<BarcodeFormat>.toVisionSymbologies(): List<VNBarcodeSymbology> =
    flatMap { format ->
        when (format) {
            BarcodeFormat.QrCode -> listOf(VNBarcodeSymbologyQR)
            BarcodeFormat.Ean13 -> listOf(VNBarcodeSymbologyEAN13)
            BarcodeFormat.Ean8 -> listOf(VNBarcodeSymbologyEAN8)
            BarcodeFormat.Code128 -> listOf(VNBarcodeSymbologyCode128)
            BarcodeFormat.Code39 -> listOf(VNBarcodeSymbologyCode39)
            BarcodeFormat.Code93 -> listOf(VNBarcodeSymbologyCode93)
            BarcodeFormat.Codabar -> listOf(VNBarcodeSymbologyCodabar)
            BarcodeFormat.Itf -> listOf(VNBarcodeSymbologyITF14, VNBarcodeSymbologyI2of5)
            // UPC-A llega como EAN-13 en Vision.
            BarcodeFormat.UpcA -> listOf(VNBarcodeSymbologyEAN13)
            BarcodeFormat.UpcE -> listOf(VNBarcodeSymbologyUPCE)
            BarcodeFormat.Pdf417 -> listOf(VNBarcodeSymbologyPDF417)
            BarcodeFormat.Aztec -> listOf(VNBarcodeSymbologyAztec)
            BarcodeFormat.DataMatrix -> listOf(VNBarcodeSymbologyDataMatrix)
        }
    }.distinct()

internal fun VNBarcodeSymbology.toBarcodeFormat(rawValue: String?): BarcodeFormat? = when (this) {
    VNBarcodeSymbologyQR -> BarcodeFormat.QrCode
    // Vision reporta UPC-A como EAN13 prefijado con 0.
    VNBarcodeSymbologyEAN13 ->
        if (rawValue?.startsWith("0") == true && rawValue.length == 13) BarcodeFormat.UpcA else BarcodeFormat.Ean13
    VNBarcodeSymbologyEAN8 -> BarcodeFormat.Ean8
    VNBarcodeSymbologyCode128 -> BarcodeFormat.Code128
    VNBarcodeSymbologyCode39 -> BarcodeFormat.Code39
    VNBarcodeSymbologyCode93 -> BarcodeFormat.Code93
    VNBarcodeSymbologyCodabar -> BarcodeFormat.Codabar
    VNBarcodeSymbologyITF14, VNBarcodeSymbologyI2of5 -> BarcodeFormat.Itf
    VNBarcodeSymbologyUPCE -> BarcodeFormat.UpcE
    VNBarcodeSymbologyPDF417 -> BarcodeFormat.Pdf417
    VNBarcodeSymbologyAztec -> BarcodeFormat.Aztec
    VNBarcodeSymbologyDataMatrix -> BarcodeFormat.DataMatrix
    else -> null
}
