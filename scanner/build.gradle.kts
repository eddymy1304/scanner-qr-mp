import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

// NOTA: SKIE (sealed->enums Swift, Flow->AsyncSequence) queda pendiente:
// SKIE 0.10.13 aún no soporta Kotlin 2.4.10. Mientras tanto la API iOS es
// Swift-friendly a mano (ScannerConfigBuilder + callbacks explícitos).
plugins {
    alias(libs.plugins.scanner.kmp.library)
    alias(libs.plugins.scanner.publishing)
    alias(libs.plugins.scanner.compose)
}

kotlin {
    // XCFramework para apps iOS nativas (SPM / integración manual).
    val xcf = XCFramework("LaCaleraScanner")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LaCaleraScanner"
            isStatic = true
            // Exporta la API de los 3 módulos en un solo framework.
            export(projects.scannerCore)
            export(projects.scannerCamera)
            export(projects.scannerUi)
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.scannerCore)
            api(projects.scannerCamera)
            api(projects.scannerUi)
            // El plugin de Compose exige el runtime en el compile classpath del módulo.
            implementation(compose.runtime)
        }
        iosMain.dependencies {
            implementation(compose.foundation)
            implementation(compose.ui)
        }
    }
}
