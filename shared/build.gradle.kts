import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    android {
        namespace = "com.omnigart.shared"
        compileSdk = 37
        minSdk = 26
        withHostTest {
            isIncludeAndroidResources = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Re-export so Swift can see NutritionFacade / NutritionCardSnapshot.
            export(project(":shared:nutrition"))
            export(project(":shared:treadmill"))
            export(project(":shared:planning"))
        }
    }

    // watchosX64 omitted (deprecated in Kotlin/Native).
    listOf(
        watchosArm64(),
        watchosSimulatorArm64()
    ).forEach { watchTarget ->
        watchTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":shared:nutrition"))
            export(project(":shared:treadmill"))
            export(project(":shared:planning"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            // Zone 2 HR bands for WorkoutKit schedule plans (pure algorithms).
            implementation(project(":shared:activity-algorithms"))
            // Adaptive calorie / macro targets (exported for phone Food tab).
            api(project(":shared:nutrition"))
            // Treadmill FTMS / RSC / iConsole parsers (exported into Shared.framework).
            api(project(":shared:treadmill"))
            // Rule-based planner / adaptive suggestions (exported for phone Planner tab).
            api(project(":shared:planning"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.health.connect.client)
            implementation(libs.security.crypto)
        }
        // Default hierarchy provides appleMain for iOS + watchOS.
        appleMain.dependencies {
            implementation(libs.sqldelight.native)
            implementation(libs.ktor.client.darwin)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.junit)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.junit)
            implementation(libs.health.connect.testing)
        }
    }
}

sqldelight {
    databases {
        create("OmniGartDatabase") {
            packageName.set("com.omnigart.shared.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        exclude { it.file.path.contains("/build/generated/") }
        // Large locale tables sourced from i18n JSON (line length / trailing commas).
        exclude("**/com/omnigart/shared/i18n/StringCatalogTables.kt")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/build/generated/**")
    exclude("**/com/omnigart/shared/strength/BundledStrengthLibrary.kt")
    exclude("**/com/omnigart/shared/strength/ReadyMadeWorkoutCatalog.kt")
    exclude("**/com/omnigart/shared/i18n/StringCatalogTables.kt")
}

/**
 * Copy already-linked Shared.frameworks into the XcodeGen search path:
 * shared/build/xcode-frameworks/<Configuration>/<PLATFORM_NAME>/Shared.framework
 *
 * Use Xcode `$(PLATFORM_NAME)` (iphonesimulator, iphoneos, watchsimulator, watchos),
 * not versioned `$(SDK_NAME)` (e.g. watchsimulator27.2), so new SDKs do not require
 * Gradle list updates. Xcode pre-build: `scripts/xcode_sync_shared_framework.sh`.
 */
tasks.register("syncXcodeFrameworks") {
    group = "build"
    description =
        "Sync linked Shared.framework into xcode-frameworks/\$(CONFIGURATION)/\$(PLATFORM_NAME)"
    doLast {
        val outRoot = layout.buildDirectory.get().asFile.resolve("xcode-frameworks")
        var synced = 0

        fun sync(targetName: String, buildType: NativeBuildType, platformName: String) {
            val typeDir =
                if (buildType == NativeBuildType.DEBUG) "debugFramework" else "releaseFramework"
            val configDir = if (buildType == NativeBuildType.DEBUG) "Debug" else "Release"
            val framework =
                layout.buildDirectory
                    .get()
                    .asFile
                    .resolve("bin/$targetName/$typeDir/Shared.framework")
            if (!framework.isDirectory) return
            val destParent = outRoot.resolve("$configDir/$platformName")
            destParent.mkdirs()
            val dest = destParent.resolve("Shared.framework")
            if (dest.exists()) dest.deleteRecursively()
            framework.copyRecursively(dest, overwrite = true)
            synced += 1
            logger.lifecycle("Synced Shared.framework → xcode-frameworks/$configDir/$platformName")
        }

        listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE).forEach { type ->
            // Prefer arm64 simulator output when both arm64 and x64 exist.
            sync("iosX64", type, "iphonesimulator")
            sync("iosSimulatorArm64", type, "iphonesimulator")
            sync("iosArm64", type, "iphoneos")
            sync("watchosSimulatorArm64", type, "watchsimulator")
            sync("watchosArm64", type, "watchos")
        }
        if (synced == 0) {
            throw GradleException(
                "syncXcodeFrameworks found no linked Shared.framework under shared/build/bin " +
                    "(run link*Framework* before sync, or fix task ordering)"
            )
        }
    }
}

// Link tasks are registered by the Kotlin Multiplatform plugin after this file is
// evaluated — wire ordering in afterEvaluate so sync never runs before links in the
// same Gradle invocation (CI used to hit a silent empty sync + test -d failure).
afterEvaluate {
    tasks.named("syncXcodeFrameworks").configure {
        mustRunAfter(
            tasks.matching { it.name.startsWith("link") && it.name.contains("Framework") }
        )
    }
}
