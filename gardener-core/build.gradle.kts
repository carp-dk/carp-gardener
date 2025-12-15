import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "dk.cachet.carp.gardener" // ← set your real group
version = "1.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Jackson
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.jsr310)

    // Kotlin (stdlib is added by the plugin automatically)
    implementation(libs.kotlin.reflect)

    // Tests
    testImplementation(kotlin("test"))
    testImplementation(libs.mockito.kotlin)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
}
