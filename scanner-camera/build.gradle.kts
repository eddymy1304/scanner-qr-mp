plugins {
    alias(libs.plugins.scanner.kmp.library)
    alias(libs.plugins.scanner.publishing)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.scannerCore)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.runtime.android)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.mlkit.barcode.scanning)
        }
    }
}
