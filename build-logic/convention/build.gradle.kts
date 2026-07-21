plugins {
    `kotlin-dsl`
}

group = "pe.lacalera.scanner.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "scanner.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpApplication") {
            id = "scanner.kmp.application"
            implementationClass = "KmpApplicationConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "scanner.compose"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
    }
}
