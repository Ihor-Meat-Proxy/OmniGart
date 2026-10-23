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
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
}

/**
 * Copy already-linked Shared.frameworks into the XcodeGen search path:
 * shared/build/xcode-frameworks/<Configuration>/<sdkName>/Shared.framework
 *
 * Root cause of CI "No such module 'Shared'": Gradle writes frameworks under
 * build/bin/<target>/<debug|release>Framework/, while project.yml looks under
 * build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)/.
 */
tasks.register("syncXcodeFrameworks") {
    group = "build"
    description =
        "Sync linked Shared.framework into xcode-frameworks/\$(CONFIGURATION)/\$(SDK_NAME)"
    doLast {
        val outRoot = layout.buildDirectory.get().asFile.resolve("xcode-frameworks")

        fun sync(targetName: String, buildType: NativeBuildType, sdkPrefixes: List<String>) {
            val typeDir =
                if (buildType == NativeBuildType.DEBUG) "debugFramework" else "releaseFramework"
            val configDir = if (buildType == NativeBuildType.DEBUG) "Debug" else "Release"
            val framework =
                layout.buildDirectory
                    .get()
                    .asFile
                    .resolve("bin/$targetName/$typeDir/Shared.framework")
            if (!framework.isDirectory) return
            sdkPrefixes.forEach { prefix ->
                val destParent = outRoot.resolve("$configDir/$prefix")
                destParent.mkdirs()
                val dest = destParent.resolve("Shared.framework")
                if (dest.exists()) dest.deleteRecursively()
                framework.copyRecursively(dest, overwrite = true)
            }
        }

        val iosSimSdks =
            listOf(
                "iphonesimulator",
                "iphonesimulator18.0",
                "iphonesimulator18.1",
                "iphonesimulator18.2",
                "iphonesimulator18.4",
                "iphonesimulator18.5",
                "iphonesimulator26.0",
                "iphonesimulator26.1",
                "iphonesimulator26.2"
            )
        val iosDeviceSdks =
            listOf("iphoneos", "iphoneos18.0", "iphoneos18.5", "iphoneos26.0", "iphoneos26.2")
        val watchSimSdks =
            listOf(
                "watchsimulator",
                "watchsimulator11.0",
                "watchsimulator11.2",
                "watchsimulator11.5",
                "watchsimulator26.0",
                "watchsimulator26.1",
                "watchsimulator26.2"
            )
        val watchDeviceSdks =
            listOf("watchos", "watchos11.0", "watchos11.5", "watchos26.0", "watchos26.2")

        listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE).forEach { type ->
            sync("iosSimulatorArm64", type, iosSimSdks)
            sync("iosArm64", type, iosDeviceSdks)
            sync("iosX64", type, iosSimSdks)
            sync("watchosSimulatorArm64", type, watchSimSdks)
            sync("watchosArm64", type, watchDeviceSdks)
        }
    }
}
