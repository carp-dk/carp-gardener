import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false

}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        // Detekt runs by default; override with -PenableDetekt=false to skip locally if needed.
        val enableDetekt = rootProject.findProperty("enableDetekt") != "false"

        if (enableDetekt) {
            apply(plugin = "io.gitlab.arturbosch.detekt")
        }
        apply(plugin = "org.jlleitschuh.gradle.ktlint")

        if (enableDetekt) {
            dependencies {
                add("detektPlugins", libs.detekt.formatting)
            }
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }

        // Force Kotlin runtime artifacts to a single version to avoid mixed-Kotlin classpath issues
        configurations.configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") {
                    useVersion(libs.versions.kotlin.get())
                    because("Align Kotlin runtime versions to project Kotlin version to avoid mixed-classpath issues")
                }
            }
        }
        if (enableDetekt) {
            extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
                buildUponDefaultConfig = true
                allRules = false
                config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
                baseline = file("$rootDir/config/detekt/baseline.xml")
            }
            tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
                ignoreFailures = true
                autoCorrect = true

                reports {
                    html.required.set(true)
                    xml.required.set(false)
                    sarif.required.set(true)
                    md.required.set(false)
                }
            }
        }

        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension>("ktlint") {
            ignoreFailures = true
            outputToConsole.set(true)

            reporters {
                reporter(ReporterType.SARIF)
                reporter(ReporterType.CHECKSTYLE)
                reporter(ReporterType.PLAIN_GROUP_BY_FILE)
            }

            filter {
                exclude("**/build/**")
                exclude("**/generated/**")
                include("**/src/**")
            }
        }
    }
}
