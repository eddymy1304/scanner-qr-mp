plugins {
    alias(libs.plugins.scanner.kmp.library)
    alias(libs.plugins.scanner.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.scannerCore)
            api(projects.scannerCamera)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}
