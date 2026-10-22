plugins {
    kotlin("multiplatform")
}

group = "com.omnigart"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvm()
    // When PR #2 lands: add androidTarget() / iosArm64() / iosSimulatorArm64()
    // and wire expect/actuals. Until then this module is pure common + JVM host tests.
    sourceSets {
        commonMain.dependencies {
            // Pure logic — no platform BLE / Polar / Health SDK deps.
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
