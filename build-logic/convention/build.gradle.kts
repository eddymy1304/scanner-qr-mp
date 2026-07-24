plugins {
    `kotlin-dsl`
}

group = "com.eddymy1304.scanner.buildlogic"

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
    implementation(libs.vanniktech.mavenPublish.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "scanner.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "scanner.compose"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("publishing") {
            id = "scanner.publishing"
            implementationClass = "PublishingConventionPlugin"
        }
    }
}
