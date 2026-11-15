import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.skie) apply false
    id("io.github.vad4nus.lockstep") apply false
}

subprojects {
    if (!name.startsWith("lockstep")) return@subprojects
    version = rootProject.libs.versions.lockstep.get()
    apply(from = rootProject.file("gradle/publishing.gradle.kts"))
    listOf("org.jetbrains.kotlin.multiplatform", "org.jetbrains.kotlin.android").forEach { kotlinPlugin ->
        pluginManager.withPlugin(kotlinPlugin) {
            extensions.configure<KotlinProjectExtension> {
                @OptIn(ExperimentalAbiValidation::class)
                abiValidation()
            }
        }
    }
}
