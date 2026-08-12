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

// Why a second module and not a source set in :app — a Macrobenchmark drives the app from
// outside its own process, the way the system does. It has to be a `com.android.test` project
// to be built and installed as a separate APK; there is no way to express that inside :app.
// Nothing ships from here: it produces the profile text that :app carries, and measurements.
include(":benchmark")
