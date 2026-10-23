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

<<<<<<< HEAD
include(":shared:activity-algorithms")
project(":shared:activity-algorithms").projectDir = file("shared/activity-algorithms")
=======
include(":androidApp")
include(":wearApp")
>>>>>>> origin/main
