plugins {
    alias(libs.plugins.scanner.kmp.application)
    alias(libs.plugins.scanner.compose)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.scanner)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "pe.lacalera.scanner.sample"
    defaultConfig {
        applicationId = "pe.lacalera.scanner.sample"
    }
}
