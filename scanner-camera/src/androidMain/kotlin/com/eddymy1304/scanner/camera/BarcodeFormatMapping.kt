package com.eddymy1304.scanner.camera

import com.google.mlkit.vision.barcode.common.Barcode
import com.eddymy1304.scanner.core.model.BarcodeFormat

internal fun Set<BarcodeFormat>.toMlKitFormats(): Int =
    map { it.toMlKitFormat() }.reduce { acc, format -> acc or format }

internal fun BarcodeFormat.toMlKitFormat(): Int = when (this) {
    BarcodeFormat.QrCode -> Barcode.FORMAT_QR_CODE
    BarcodeFormat.Ean13 -> Barcode.FORMAT_EAN_13
    BarcodeFormat.Ean8 -> Barcode.FORMAT_EAN_8
    BarcodeFormat.Code128 -> Barcode.FORMAT_CODE_128
    BarcodeFormat.Code39 -> Barcode.FORMAT_CODE_39
    BarcodeFormat.Code93 -> Barcode.FORMAT_CODE_93
    BarcodeFormat.Codabar -> Barcode.FORMAT_CODABAR
    BarcodeFormat.Itf -> Barcode.FORMAT_ITF
    BarcodeFormat.UpcA -> Barcode.FORMAT_UPC_A
    BarcodeFormat.UpcE -> Barcode.FORMAT_UPC_E
    BarcodeFormat.Pdf417 -> Barcode.FORMAT_PDF417
    BarcodeFormat.Aztec -> Barcode.FORMAT_AZTEC
    BarcodeFormat.DataMatrix -> Barcode.FORMAT_DATA_MATRIX
}

internal fun Int.toBarcodeFormat(): BarcodeFormat? = when (this) {
    Barcode.FORMAT_QR_CODE -> BarcodeFormat.QrCode
    Barcode.FORMAT_EAN_13 -> BarcodeFormat.Ean13
    Barcode.FORMAT_EAN_8 -> BarcodeFormat.Ean8
    Barcode.FORMAT_CODE_128 -> BarcodeFormat.Code128
    Barcode.FORMAT_CODE_39 -> BarcodeFormat.Code39
    Barcode.FORMAT_CODE_93 -> BarcodeFormat.Code93
    Barcode.FORMAT_CODABAR -> BarcodeFormat.Codabar
    Barcode.FORMAT_ITF -> BarcodeFormat.Itf
    Barcode.FORMAT_UPC_A -> BarcodeFormat.UpcA
    Barcode.FORMAT_UPC_E -> BarcodeFormat.UpcE
    Barcode.FORMAT_PDF417 -> BarcodeFormat.Pdf417
    Barcode.FORMAT_AZTEC -> BarcodeFormat.Aztec
    Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DataMatrix
    else -> null
}
