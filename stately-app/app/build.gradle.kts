plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.cazh0.stately"
    compileSdk = 35

    defaultConfig {
        // Why not com.lloydtorres.stately: this is an independent rebuild, not a continuation
        // of the published listing. A distinct id also lets both apps sit on one device.
        applicationId = "dev.cazh0.stately"
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

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
