import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import internal.configureIosTargets
import internal.libs
import internal.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin para módulos de librería KMP (scanner-core, scanner-camera, scanner-ui, scanner).
 * AGP 9: usa el plugin `com.android.kotlin.multiplatform.library` (el clásico
 * `com.android.library` ya no es compatible con KMP).
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                jvmToolchain(17)
                explicitApi = ExplicitApiMode.Strict

                // Target Android del plugin AGP-KMP.
                (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>(
                    "androidLibrary",
                ) {
                    // :scanner-core -> pe.lacalera.scanner.core, :scanner -> pe.lacalera.scanner
                    namespace = "pe.lacalera." + project.name.replace("-", ".")
                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()
                    // Habilita los unit tests JVM (commonTest) del target Android.
                    withHostTest {}
                }

                configureIosTargets()
            }
        }
    }
}
