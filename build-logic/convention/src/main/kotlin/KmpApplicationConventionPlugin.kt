import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import internal.configureIosTargets
import internal.libs
import internal.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin para apps KMP de ejemplo (sample/composeApp).
 */
class KmpApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.application")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                jvmToolchain(17)
                androidTarget()
                configureIosTargets()
            }

            extensions.configure<BaseAppModuleExtension> {
                compileSdk = libs.version("android-compileSdk").toInt()
                defaultConfig {
                    minSdk = libs.version("android-minSdk").toInt()
                    targetSdk = libs.version("android-targetSdk").toInt()
                    versionCode = 1
                    versionName = "0.1.0"
                }
            }
        }
    }
}
