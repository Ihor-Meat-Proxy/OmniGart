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

include(":shared:achievements")
project(":shared:achievements").projectDir = file("shared/achievements")

include(":shared:bodymap")
project(":shared:bodymap").projectDir = file("shared/bodymap")
