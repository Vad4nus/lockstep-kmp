plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.skie)
    id("io.github.vad4nus.lockstep")
}

kotlin {
    androidLibrary {
        namespace = "io.github.vad4nus.sample.shared"
        compileSdk = 36
        minSdk = 24
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "SampleShared"
            isStatic = true
            export(project(":lockstep"))
            export(project(":lockstep-result"))
            export(project(":lockstep-mvvm"))
            export(project(":lockstep-screens"))
            export(libs.androidx.lifecycle.viewmodel)
            export(libs.moko.resources)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":lockstep"))
            api(project(":lockstep-result"))
            api(project(":lockstep-mvvm"))
            api(project(":lockstep-screens"))
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.moko.resources)
        }
    }
}
