pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Why: lets Gradle download the JDK named in gradle/gradle-daemon-jvm.properties instead
    // of failing on a machine that does not already have it.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    // Why: FAIL_ON_PROJECT_REPOS keeps every dependency source declared in exactly one
    // place. Module-level `repositories {}` blocks are how the legacy build ended up
    // pulling from a dead jcenter.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Civily"
include(":app")
