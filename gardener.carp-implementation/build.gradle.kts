import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    kotlin("plugin.serialization") version "1.5.10"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "gardener.carp-implementation"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val vertxVersion = "4.1.3"
val junitJupiterVersion = "5.7.2"

val mainVerticleName = "com.example.authenticationmodule.verticles.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
    mainClass.set(launcherClassName)
}

dependencies {
    // Gardener Core
    implementation(project(":gardener.core"))
    // Guava for hashing
    implementation("com.google.guava:guava:20.0")
    // OAUTH Scribe java
    implementation("com.github.scribejava:scribejava-core:6.9.0")
    // Jackson + extension for Java 8 dates
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")
    // Logging
    implementation("org.slf4j:slf4j-api:1.7.+")
    implementation("ch.qos.logback:logback-classic:1.2.+")
    // Vert.x
    implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    implementation("io.vertx:vertx-web-client:$vertxVersion")
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-config:$vertxVersion")
    // Vert.x Mongo
    implementation("io.vertx:vertx-mongo-client:4.1.5")
    // RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.12.0")
    // Only for mac
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.67.Final:osx-x86_64")
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.21")
    // Test
    testImplementation("io.vertx:vertx-junit5:$vertxVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"

tasks.withType<ShadowJar> {
    archiveClassifier.set("fat")
    manifest {
        attributes(mapOf("Main-Verticle" to mainVerticleName))
    }
    mergeServiceFiles()
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(PASSED, SKIPPED, FAILED)
    }
}

tasks.withType<JavaExec> {
    args = listOf(
        "run",
        mainVerticleName,
        "--redeploy=$watchForChange",
        "--launcher-class=$launcherClassName",
        "--on-redeploy=$doOnChange"
    )
}
