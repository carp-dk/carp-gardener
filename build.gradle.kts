import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false

}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        apply(plugin = "org.jlleitschuh.gradle.ktlint")

        dependencies {
            add("detektPlugins", libs.detekt.formatting)
        }
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
