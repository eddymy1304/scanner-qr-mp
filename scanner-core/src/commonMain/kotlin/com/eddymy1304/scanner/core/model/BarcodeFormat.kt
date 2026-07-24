package com.eddymy1304.scanner.core.model

/**
 * Formatos de código soportados, mapeables a ML Kit ([com.google.mlkit.vision.barcode.common.Barcode])
 * en Android y a `VNBarcodeSymbology` (Vision) en iOS.
 */
public enum class BarcodeFormat {
    QrCode,
    Ean13,
    Ean8,
    Code128,
    Code39,
    Code93,
    Codabar,
    Itf,
    UpcA,
    UpcE,
    Pdf417,
    Aztec,
    DataMatrix,
    ;

    public companion object {
        /** Solo QR: el default del composable simple. */
        public val QR_ONLY: Set<BarcodeFormat> = setOf(QrCode)

        /** Todos los formatos 1D (códigos de barras lineales). */
        public val ONE_DIMENSIONAL: Set<BarcodeFormat> =
            setOf(Ean13, Ean8, Code128, Code39, Code93, Codabar, Itf, UpcA, UpcE)

        /** Todos los formatos 2D. */
        public val TWO_DIMENSIONAL: Set<BarcodeFormat> = setOf(QrCode, Pdf417, Aztec, DataMatrix)

        /** Todos los formatos soportados. */
        public val ALL: Set<BarcodeFormat> = entries.toSet()
    }
}
