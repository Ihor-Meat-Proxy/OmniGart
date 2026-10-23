plugins {
    kotlin("multiplatform")
}

group = "com.omnigart"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvm()
    // Same Apple targets as :shared so Zone 2 math can feed WorkoutKit plan factories.
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosArm64()
    watchosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            // Pure logic — no platform SDKs.
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
