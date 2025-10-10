import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

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
    mainClass.set("io.vertx.core.Launcher")
}

val mainVerticleName = "com.example.authenticationmodule.verticles.MainVerticle"
val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

dependencies {
    // Gardener Core
    implementation(projects.core)

    // BOM to align Vert.x modules
    implementation(platform(libs.vertx.bom))

    // Libraries
    implementation(libs.guava)
    implementation(libs.scribejava)

    // Jackson
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.jsr310)

    // Logging
    implementation(libs.slf4j.api)
    implementation(libs.logback)

    // Vert.x (versions via BOM)
    implementation(libs.bundles.vertx.core)
    implementation(libs.vertx.mongo)

    // RabbitMQ
    implementation(libs.rabbitmq)

    // macOS native resolver (optional)
    runtimeOnly(libs.netty.osx)

    // Kotlin
    implementation(libs.kotlin.reflect)

    // Test
    testImplementation(libs.vertx.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("fat")
    manifest { attributes(mapOf("Main-Verticle" to mainVerticleName)) }
    mergeServiceFiles()
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging { events = setOf(PASSED, SKIPPED, FAILED) }
}

tasks.withType<JavaExec> {
    args = listOf(
        "run",
        mainVerticleName,
        "--redeploy=$watchForChange",
        "--launcher-class=${application.mainClass.get()}",
        "--on-redeploy=$doOnChange"
    )
}
