package pe.lacalera.scanner

import pe.lacalera.scanner.core.ScannerLibrary

/**
 * Módulo umbrella: re-exporta core + camera + ui.
 * En la Fase 5 aquí vivirá `ScannerViewController` (iosMain) para apps Swift.
 */
public val scannerVersion: String get() = ScannerLibrary.VERSION
