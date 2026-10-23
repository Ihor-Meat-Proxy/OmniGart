plugins {
    // AGP 9+ ships built-in Kotlin — do not apply org.jetbrains.kotlin.android
    id("com.android.library")
}

android {
    namespace = "com.omnigart.sensors.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(project(":shared:sensors"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
    // 1.17.0 is the newest stable that still metadata-checks against compileSdk 36
    // (1.19+ requires compileSdk 37; platform android-37 not yet on all CI images).
    implementation("androidx.core:core-ktx:1.19.1")

    // Official Polar BLE SDK (JitPack). Licence: Polar SDK Limited License — see docs/third_party/POLAR_SDK.md
    // Verified release: 8.4.0 (2026-10-01). https://github.com/polarofficial/polar-ble-sdk/releases/tag/8.4.0
    implementation("com.github.polarofficial:polar-ble-sdk:8.4.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("org.robolectric:robolectric:4.17")
}

tasks.withType<Test>().configureEach {
    // Robolectric 4.17 + newer JDKs need these opens for ApplicationSharedMemory.
    jvmArgs(
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
    )
}
