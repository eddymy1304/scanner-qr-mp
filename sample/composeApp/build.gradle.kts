import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.scanner.kmp.library)
    alias(libs.plugins.scanner.compose)
}

kotlin {
    // El sample no necesita explicit API (es una app de demo, no una librería publicada).
    explicitApi = ExplicitApiMode.Disabled

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
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
    }
}
