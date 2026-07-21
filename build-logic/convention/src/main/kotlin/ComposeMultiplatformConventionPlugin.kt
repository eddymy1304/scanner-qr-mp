import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin para módulos que usan Compose Multiplatform.
 * Aplica el plugin de Compose y el compilador de Compose de Kotlin.
 */
class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("org.jetbrains.compose")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
    }
}
