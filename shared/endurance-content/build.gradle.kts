plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation("com.networknt:json-schema-validator:2.0.8")
            implementation("com.fasterxml.jackson.core:jackson-databind:2.22.4")
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
        "omnigart.running.root",
        rootProject.projectDir.resolve("content/running").absolutePath,
    )
    systemProperty(
        "omnigart.swimming.root",
        rootProject.projectDir.resolve("content/swimming").absolutePath,
    )
}
