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

<<<<<<< HEAD
include(":shared:body")
project(":shared:body").projectDir = file("shared/body")
=======
include(":shared:bodymap")
project(":shared:bodymap").projectDir = file("shared/bodymap")
>>>>>>> origin/main
