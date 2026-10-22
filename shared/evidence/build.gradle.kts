plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Test>("jvmTest") {
    systemProperty(
        "omnigart.evidence.root",
        rootProject.projectDir.resolve("content/evidence").absolutePath,
    )
}
