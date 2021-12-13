import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
}

group = "gardener.core"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Jackson + extension for Java 8 dates
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.21")
    // Test
    testImplementation(kotlin("test"))
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"
