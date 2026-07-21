plugins {
    alias(libs.plugins.scanner.kmp.library)
    alias(libs.plugins.scanner.compose)
}

kotlin {
    // Umbrella: expone core + camera + ui como un solo artefacto.
    // En la Fase 5 aquí se configura el XCFramework (LaCaleraScanner) con export() de los 3.
    sourceSets {
        commonMain.dependencies {
            api(projects.scannerCore)
            api(projects.scannerCamera)
            api(projects.scannerUi)
            // El plugin de Compose exige el runtime en el compile classpath del módulo.
            implementation(compose.runtime)
        }
    }
}
