plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    jvm()
    // Same Apple targets as :shared so Planner engines ship in Shared.framework.
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosArm64()
    watchosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            // Content validators / evidence tooling stay JVM-only.
            implementation(project(":shared:evidence"))
            implementation(libs.json.schema.validator)
            implementation(libs.jackson.databind)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Test>("jvmTest") {
    systemProperty(
        "omnigart.content.root",
        rootProject.projectDir.resolve("content/exercises").absolutePath
    )
    systemProperty(
        "omnigart.yoga.root",
        rootProject.projectDir.resolve("content/yoga").absolutePath
    )
    systemProperty(
        "omnigart.pilates.root",
        rootProject.projectDir.resolve("content/pilates").absolutePath
    )
}

ktlint {
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
}
