plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.skie)
    id("io.github.vad4nus.lockstep")
}

kotlin {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "PlainShared"
            isStatic = true
            export(project(":lockstep"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":lockstep"))
        }
    }
}
