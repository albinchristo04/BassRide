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
        maven("https://jitpack.io") // Tarsos DSP
    }
}

rootProject.name = "BassRide"

include(":app")
include(":audio-engine")
include(":bluetooth")
include(":ui")
include(":data")
include(":billing")
