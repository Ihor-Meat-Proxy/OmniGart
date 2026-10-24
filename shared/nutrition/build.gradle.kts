plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

group = "com.omnigart"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvm()
    // Same Apple targets as :shared so phone UI can consume targets via Shared.
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosArm64()
    watchosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
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
