package internal

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

/**
 * Targets iOS de la librería. Sin iosX64: Compose Multiplatform 1.11+
 * ya no publica para simulador Intel.
 */
internal fun KotlinMultiplatformExtension.configureIosTargets() {
    iosArm64()
    iosSimulatorArm64()
}
