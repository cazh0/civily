plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "dev.cazh0.civily.benchmark"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        // Why 28 and not the app's 21: profile collection reads ART's own record of what it
        // compiled, and `pm dump-profiles` only exists from Android 9. The profile this module
        // produces is still installed by `profileinstaller` down to the app's own minSdk — the
        // floor is on generating one, not on using one.
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The app this drives. AGP builds and installs it as part of every task here.
    targetProjectPath = ":app"

    // Why: a Macrobenchmark instruments itself and talks to the app over the system, rather than
    // being loaded into the app's process. Sharing a process would put the test framework inside
    // the thing being measured.
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    // The device on the end of the cable. There is no emulator in this loop: a profile generated
    // on an emulator records an emulator's class-loading order.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
