plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
}

description = "Gradle plugin that adds the Lockstep Swift generator to SKIE"

version = libs.versions.lockstep.get()
apply(from = "../../gradle/publishing.gradle.kts")

val lockstepVersion = libs.versions.lockstep.get()
val skieVersion = libs.versions.skie.get()
val generatedVersionDir = layout.buildDirectory.dir("generated/version")

val generateLockstepVersion by tasks.registering {
    inputs.property("lockstepVersion", lockstepVersion)
    inputs.property("skieVersion", skieVersion)
    outputs.dir(generatedVersionDir)
    doLast {
        val file = generatedVersionDir.get().asFile.resolve("io/github/vad4nus/lockstep/gradle/LockstepVersion.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package io.github.vad4nus.lockstep.gradle

            internal object LockstepVersion {
                const val VALUE = "$lockstepVersion"
                const val SUPPORTED_SKIE = "$skieVersion"
            }
            """.trimIndent() + "\n",
        )
    }
}

kotlin {
    sourceSets.main {
        kotlin.srcDir(generateLockstepVersion)
    }
}

dependencies {
    compileOnly(libs.skie.gradle.plugin)
    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        create("lockstep") {
            id = "io.github.vad4nus.lockstep"
            implementationClass = "io.github.vad4nus.lockstep.gradle.LockstepGradlePlugin"
        }
    }
}
