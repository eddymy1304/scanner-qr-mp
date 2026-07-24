import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure

/**
 * Publicación Maven (GitHub Packages) para los módulos de librería.
 * Credenciales: env GITHUB_ACTOR/GITHUB_TOKEN o gradle props gpr.user/gpr.key.
 */
class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")

            // groupId y version los toma vanniktech de GROUP/VERSION_NAME (gradle.properties);
            // el artifactId es el nombre del módulo.
            extensions.configure<MavenPublishBaseExtension> {
                pom {
                    name.set(project.name)
                    description.set("Librería Kotlin Multiplatform de escaneo QR/Barcode (Android + iOS) — La Calera")
                    url.set("https://github.com/eddymy1304/scanner-qr-mp")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("lacalera")
                            name.set("La Calera")
                        }
                    }
                    scm {
                        url.set("https://github.com/eddymy1304/scanner-qr-mp")
                        connection.set("scm:git:git://github.com/eddymy1304/scanner-qr-mp.git")
                    }
                }
            }

            extensions.configure<PublishingExtension> {
                repositories.maven(
                    Action<MavenArtifactRepository> {
                        name = "GitHubPackages"
                        setUrl("https://maven.pkg.github.com/eddymy1304/scanner-qr-mp")
                        credentials(
                            Action<PasswordCredentials> {
                                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                                    ?: providers.gradleProperty("gpr.user").orNull
                                password = providers.environmentVariable("GITHUB_TOKEN").orNull
                                    ?: providers.gradleProperty("gpr.key").orNull
                            },
                        )
                    },
                )
            }
        }
    }
}
