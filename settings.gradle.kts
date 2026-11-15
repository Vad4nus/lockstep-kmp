pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("lockstep-plugin")
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "lockstep-kmp"

includeBuild("lockstep-plugin")

include(":lockstep")
include(":lockstep-result")
include(":lockstep-mvvm")
include(":lockstep-ktor")
include(":lockstep-compose")
include(":lockstep-screens")
include(":sample:plain")
include(":sample:shared")
include(":sample:androidApp")
