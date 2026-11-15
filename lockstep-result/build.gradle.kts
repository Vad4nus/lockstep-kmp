plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

description = "AppResult and AppError, the result type shared by the data layer and ViewModels"

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "io.github.vad4nus.lockstep.result"
        compileSdk = 36
        minSdk = 21
        withHostTest {}
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    sourceSets {
        commonMain.dependencies {
            api(libs.moko.resources)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
