pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OmniGart"

include(":shared")
include(":shared:planning")
project(":shared:planning").projectDir = file("shared/planning")

include(":shared:bodymap")
project(":shared:bodymap").projectDir = file("shared/bodymap")

include(":shared:achievements")
project(":shared:achievements").projectDir = file("shared/achievements")

include(":androidApp")
include(":wearApp")
