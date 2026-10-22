pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "omnigart-content-planning"

include(":shared:planning")
project(":shared:planning").projectDir = file("shared/planning")

include(":shared:bodymap")
project(":shared:bodymap").projectDir = file("shared/bodymap")

include(":shared:sensors")
project(":shared:sensors").projectDir = file("shared/sensors")

// Android sensors library is standalone until PR #2's Android shell lands.
// Include when an Android Gradle Plugin is available on the classpath:
//   include(":android:sensors")
//   project(":android:sensors").projectDir = file("android/sensors")
