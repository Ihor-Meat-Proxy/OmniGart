plugins {
    kotlin("multiplatform")
}

group = "com.omnigart"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvm()
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
