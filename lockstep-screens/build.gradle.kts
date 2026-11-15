plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

description = "Screens described in Kotlin Multiplatform as a tree of nodes, built with a DSL or decoded from JSON"

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "io.github.vad4nus.lockstep.screens"
        compileSdk = 36
        minSdk = 21
        withHostTest {}
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    sourceSets {
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCRefinement")
        }

        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
