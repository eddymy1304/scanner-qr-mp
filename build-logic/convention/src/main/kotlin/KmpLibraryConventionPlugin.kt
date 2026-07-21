import com.android.build.gradle.LibraryExtension
import internal.configureIosTargets
import internal.libs
import internal.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin para módulos de librería KMP (scanner-core, scanner-camera, scanner-ui, scanner).
 * Configura targets Android + iOS, JDK 17 y explicit API mode.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.library")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                jvmToolchain(17)
                explicitApi = ExplicitApiMode.Strict

                androidTarget {
                    publishLibraryVariants("release")
                }
                configureIosTargets()
            }

            extensions.configure<LibraryExtension> {
                // :scanner-core -> pe.lacalera.scanner.core, :scanner -> pe.lacalera.scanner
                namespace = "pe.lacalera." + name.replace("-", ".")
                compileSdk = libs.version("android-compileSdk").toInt()
                defaultConfig {
                    minSdk = libs.version("android-minSdk").toInt()
                }
            }
        }
    }
}
