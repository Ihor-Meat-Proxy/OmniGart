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

include(":shared:evidence")
project(":shared:evidence").projectDir = file("shared/evidence")

include(":shared:bodymap")
project(":shared:bodymap").projectDir = file("shared/bodymap")

include(":androidApp")
include(":wearApp")
