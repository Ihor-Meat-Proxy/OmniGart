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

include(":shared:evidence")
project(":shared:evidence").projectDir = file("shared/evidence")

include(":shared:planning")
project(":shared:planning").projectDir = file("shared/planning")
