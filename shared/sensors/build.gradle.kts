plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ktlint)
}

ktlint {
    android.set(false)
    ignoreFailures.set(false)
}

kotlin {
    jvm()
    // Same Apple targets as :shared so treadmill parsers ship inside Shared.framework.
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosArm64()
    watchosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
