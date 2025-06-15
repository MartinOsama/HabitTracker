pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.0"
    }
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "HabitTracker"
include(":app", ":backend")