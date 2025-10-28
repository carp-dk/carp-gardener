import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.application)
}

group = "caws-implementation"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    // Ktor main application entry
    mainClass.set("dk.carp.gardener.authentication.ktor.MainApplicationKt")
}

dependencies {
    // Gardener Core
    implementation(projects.core)

    // Libraries
    implementation(libs.guava)
    implementation(libs.scribejava)

    // Jackson
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.jsr310)

    // Logging
    implementation(libs.slf4j.api)
    implementation(libs.logback)

    // RabbitMQ
    implementation(libs.rabbitmq)

    // macOS native resolver (optional)
    runtimeOnly(libs.netty.osx)

    // Kotlin
    implementation(libs.kotlin.reflect)

    // HTTP client/server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    // MongoDB Java driver
    implementation(libs.mongodb.driver.sync)

    // Test
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.kotlin)
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("fat")
    manifest { attributes(mapOf("Main-Class" to application.mainClass.get())) }
    mergeServiceFiles()
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging { events = setOf(PASSED, SKIPPED, FAILED) }
}
