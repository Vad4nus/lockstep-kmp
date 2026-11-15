plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

description = "Flows for Kotlin Multiplatform ViewModels that Swift reads as native types"

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "io.github.vad4nus.lockstep"
        compileSdk = 36
        minSdk = 21
        withHostTest {}
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
