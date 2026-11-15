package io.github.vad4nus.lockstep.gradle

import co.touchlab.skie.plugin.SkieTarget
import co.touchlab.skie.plugin.subplugin.SkieSubplugin
import co.touchlab.skie.plugin.util.KotlinCompilerPluginOption
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider

class LockstepGradlePlugin : SkieSubplugin {

    override val compilerPluginId: String = "io.github.vad4nus.lockstep"

    override fun apply(target: Project) {
        val appliedSkie = runCatching {
            Class.forName("co.touchlab.skie.gradle_plugin_impl.BuildConfig")
                .getField("SKIE_VERSION")
                .get(null) as String
        }.getOrNull()
        if (appliedSkie != LockstepVersion.SUPPORTED_SKIE) {
            val message = "lockstep ${LockstepVersion.VALUE} is built for SKIE ${LockstepVersion.SUPPORTED_SKIE} and uses its " +
                "internal API, found ${appliedSkie ?: "an unknown version"}. Use that SKIE version, or set " +
                "lockstep.allowUnsupportedSkie=true in gradle.properties to try this one at your own risk."
            if (target.providers.gradleProperty("lockstep.allowUnsupportedSkie").orNull == "true") {
                target.logger.warn(message)
            } else {
                throw GradleException(message)
            }
        }
        checkLibraryVersions(target)
        target.afterEvaluate {
            check(target.plugins.hasPlugin("co.touchlab.skie")) {
                "lockstep: apply the co.touchlab.skie plugin together with io.github.vad4nus.lockstep"
            }
        }
    }

    override fun getOptions(
        project: Project,
        target: SkieTarget,
    ): Provider<List<KotlinCompilerPluginOption>> = project.provider { emptyList() }

    override fun configureDependencies(project: Project, configuration: Configuration) {
        configuration.dependencies.add(
            project.dependencies.create("io.github.vad4nus:lockstep-skie-plugin:${LockstepVersion.VALUE}"),
        )
    }
}

/** What is wrong with a `lockstep` library of a version other than the plugin's, or null when the dependency is fine. */
internal fun versionMismatch(
    group: String?,
    name: String,
    version: String?,
    pluginVersion: String = LockstepVersion.VALUE,
): String? =
    if (group != "io.github.vad4nus" || !name.startsWith("lockstep") || version.isNullOrEmpty() || version == pluginVersion) {
        null
    } else {
        "lockstep: $name is $version, the plugin is $pluginVersion. " +
            "The generated Swift calls the libraries, so use one version for both."
    }

/** Fails the resolution of a `lockstep` library whose version differs from the plugin's, in every configuration of [project]. */
internal fun checkLibraryVersions(project: Project) {
    project.configurations.configureEach { configuration ->
        configuration.resolutionStrategy.eachDependency { details ->
            val requested = details.requested
            versionMismatch(requested.group, requested.name, requested.version)?.let { throw GradleException(it) }
        }
    }
}
