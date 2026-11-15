pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "lockstep-plugin"

include(":lockstep-gradle-plugin", ":lockstep-skie-plugin")
project(":lockstep-gradle-plugin").projectDir = file("gradle-plugin")
project(":lockstep-skie-plugin").projectDir = file("skie-plugin")
