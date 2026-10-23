plugins {
    alias(libs.plugins.android.application)
    // AGP 9+ ships built-in Kotlin; do not apply org.jetbrains.kotlin.android
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.omnigart.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.omnigart.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.omnigart"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDirs(
                "src/main/res",
                rootProject.file("design/generated/android/values"),
                rootProject.file("design/generated/android/drawable")
            )
            // Offline baseline: exercises, evidence, icons, body-maps, etc.
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
    implementation(libs.appauth)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

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
        exclude { it.file.path.contains("/design/") }
        exclude { it.file.path.contains("/com/omnigart/design/") }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // Synced from design/generated/compose — skip generated ImageVectors / tokens.
    exclude("**/com/omnigart/design/**")
}
