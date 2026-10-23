plugins {
    alias(libs.plugins.android.application)
    // AGP 9+ ships built-in Kotlin; do not apply org.jetbrains.kotlin.android
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.omnigart.wear"
    compileSdk = 37

    defaultConfig {
        // Standalone Wear applicationId for Phase 0 / Robolectric isolation.
        // Play multi-APK packaging can realign later (see docs/OPEN_QUESTIONS.md).
        applicationId = "com.omnigart.wear"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs(rootProject.file("generated/content-bundle"))
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(rootProject.tasks.named("packBundledContent"))
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.wear.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    testImplementation(platform(libs.compose.bom))
}

ktlint {
    android.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        exclude { it.file.path.contains("/com/omnigart/design/") }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/com/omnigart/design/**")
}
