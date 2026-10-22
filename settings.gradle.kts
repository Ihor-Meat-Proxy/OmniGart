pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "omnigart-content-planning"

include(":shared:planning")
project(":shared:planning").projectDir = file("shared/planning")

include(":shared:sensors")
project(":shared:sensors").projectDir = file("shared/sensors")

// Android library needs an SDK. Include when ANDROID_HOME/ANDROID_SDK_ROOT is set
// (CI Sensors job installs the SDK). Sources still live under android/sensors for PR #2.
val androidSdk = sequenceOf(
    System.getenv("ANDROID_HOME"),
    System.getenv("ANDROID_SDK_ROOT"),
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
