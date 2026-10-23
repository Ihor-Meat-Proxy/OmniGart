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

include(":shared:bodymap")
project(":shared:bodymap").projectDir = file("shared/bodymap")

include(":shared:sensors")
project(":shared:sensors").projectDir = file("shared/sensors")

include(":shared:hydration")
project(":shared:hydration").projectDir = file("shared/hydration")

include(":shared:body")
project(":shared:body").projectDir = file("shared/body")

include(":shared:activity-algorithms")
project(":shared:activity-algorithms").projectDir = file("shared/activity-algorithms")

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
} else {
    logger.lifecycle(
        "Skipping :android:sensors (no Android SDK platforms under ANDROID_HOME). " +
            "KMP :shared:sensors still builds; set ANDROID_HOME to enable the Android module."
    )
}
