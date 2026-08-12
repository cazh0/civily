plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "dev.cazh0.civily"
    compileSdk = 35

    defaultConfig {
        // Why not com.lloydtorres.stately: this is an independent rebuild, not a continuation
        // of the published listing. A distinct id also lets both apps sit on one device.
        applicationId = "dev.cazh0.civily"
        // Why: minSdk stays at 21 to honour the compatibility tenet in README.md.
        // Compose, OkHttp 4 and Coil 2 all support 21.
        minSdk = 21
        targetSdk = 35
        versionCode = 52
        versionName = "2.0.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Why: spec §4 requires the release APK never grow. R8 + resource shrinking
            // is the only lever that offsets the Compose runtime's baseline cost.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // Why: the Baseline Profile plugin derives two more build types from `release` — one
    // unminified, to record a profile against readable names, and one to benchmark against — and
    // an APK cannot be installed on a device unless it is signed. `release` itself keeps no
    // signing config, because what signs the shipped app is not this machine's debug keystore.
    buildTypes.configureEach {
        if (name.startsWith("nonMinified") || name.startsWith("benchmark")) {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // Why: kotlinx-coroutines ships a binary descriptor for its debug agent, which nothing
        // in a shipped app can attach. Spec §4 says the APK may not grow; the first place to
        // look is what is in it that never runs.
        resources.excludes += "DebugProbesKt.bin"
    }

    testOptions {
        // Why: android.util.Log is a stub on the JVM and throws by default. The parse boundary
        // logs before it returns a failure, so without this every parse-failure test blows up
        // on the logging rather than the assertion.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.okhttp)
    implementation(libs.xmlutil.serialization)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // Why a runtime library for a build-time artifact: the platform only installs a shipped
    // profile itself from Android 9 through Play, and not at all for a sideloaded APK. This
    // writes it into ART's store on first launch, which is what makes the profile take effect
    // on every device the app supports rather than only the ones that got it from the store.
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // The module that records the profile. Its output is `src/release/generated/`, checked in.
    baselineProfile(project(":benchmark"))
}

baselineProfile {
    // Why generation is not part of assembling: a build that needs a phone plugged into it is
    // not a build. `./gradlew :app:generateReleaseBaselineProfile` regenerates the file, and it
    // is a deliberate act performed when the journey changes.
    automaticGenerationDuringBuild = false
}
