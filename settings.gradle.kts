pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "carp-gardener"
include("core", "caws-implementation")

 project(":core").projectDir = file("gardener-core")
 project(":caws-implementation").projectDir = file("gardener-caws-implementation")