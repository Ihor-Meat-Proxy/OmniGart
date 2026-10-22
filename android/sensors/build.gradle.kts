plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

/**
 * Standalone Android sensors library.
 *
 * Wiring when PR #2 lands (see docs/sensors/WIRING.md):
 * - Root settings.gradle.kts: include(":android:sensors")
 * - androidApp dependencies: implementation(project(":android:sensors"))
 * - Ensure JitPack + Google/Maven Central repos; Polar BLE SDK 8.4.0 via JitPack.
 *
 * Until the Android Gradle Plugin is on the root build, this module is not
 * included from settings.gradle.kts — open it in Android Studio or include it
 * after AGP is available.
 */
android {
    namespace = "com.omnigart.sensors"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Shared pure-logic module (parsers, HRV, merge). Requires composite build
    // or published artifact once KMP android target is enabled.
    // implementation(project(":shared:sensors"))

    // Polar BLE SDK — latest stable verified 2026-10-01: 8.4.0 (JitPack).
    implementation("com.github.polarofficial:polar-ble-sdk:8.4.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.core:core-ktx:1.17.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
