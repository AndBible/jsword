rootProject.name = "jsword"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        // Keep in step with and-bible's gradle/libs.versions.toml (kotlin = "2.4.21"); inside and-bible
        // this file is not evaluated and the root build supplies the plugin.
        id("org.jetbrains.kotlin.multiplatform") version "2.4.21"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
