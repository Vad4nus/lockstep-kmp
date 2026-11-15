plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

description = "Base ViewModel with state, chrome, actions, requests and a stack navigator"

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "io.github.vad4nus.lockstep.mvvm"
        compileSdk = 36
        minSdk = 21
        withHostTest {}
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    sourceSets {
        commonMain.dependencies {
            api(project(":lockstep"))
            api(project(":lockstep-result"))
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.moko.resources)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
