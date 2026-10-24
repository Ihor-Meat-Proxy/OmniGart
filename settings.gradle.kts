pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS so :android:sensors / Polar can resolve JitPack without project repos.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "OmniGart"

include(":shared")
include(":shared:planning")
project(":shared:planning").projectDir = file("shared/planning")

include(":shared:evidence")
project(":shared:evidence").projectDir = file("shared/evidence")

include(":shared:insights")
project(":shared:insights").projectDir = file("shared/insights")

include(":shared:achievements")
project(":shared:achievements").projectDir = file("shared/achievements")

include(":shared:bodymap")
project(":shared:bodymap").projectDir = file("shared/bodymap")

include(":shared:sensors")
project(":shared:sensors").projectDir = file("shared/sensors")

include(":shared:treadmill")
project(":shared:treadmill").projectDir = file("shared/treadmill")

include(":shared:hydration")
project(":shared:hydration").projectDir = file("shared/hydration")

include(":shared:body")
project(":shared:body").projectDir = file("shared/body")

include(":shared:activity-algorithms")
project(":shared:activity-algorithms").projectDir = file("shared/activity-algorithms")

include(":shared:endurance-content")
project(":shared:endurance-content").projectDir = file("shared/endurance-content")

include(":shared:nutrition-data")
project(":shared:nutrition-data").projectDir = file("shared/nutrition-data")

include(":androidApp")
include(":wearApp")

// Android sensors library needs an SDK. Include when ANDROID_HOME/ANDROID_SDK_ROOT is set.
val androidSdk =
    sequenceOf(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT")
    ).firstOrNull { !it.isNullOrBlank() }

if (!androidSdk.isNullOrBlank() && file(androidSdk).resolve("platforms").exists()) {
    include(":android:sensors")
    project(":android:sensors").projectDir = file("android/sensors")
    include(":android:treadmill")
    project(":android:treadmill").projectDir = file("android/treadmill")
} else {
    logger.lifecycle(
        "Skipping :android:sensors / :android:treadmill (no Android SDK platforms under ANDROID_HOME). " +
            "KMP :shared:sensors and :shared:treadmill still build; set ANDROID_HOME to enable Android modules."
    )
}
