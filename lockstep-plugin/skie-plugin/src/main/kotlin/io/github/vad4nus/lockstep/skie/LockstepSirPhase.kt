package io.github.vad4nus.lockstep.skie

import co.touchlab.skie.kir.KirProvider
import co.touchlab.skie.kir.element.KirClass
import co.touchlab.skie.kir.element.KirProperty
import co.touchlab.skie.phases.SirPhase
import co.touchlab.skie.sir.element.SirClass
import io.github.vad4nus.lockstep.skie.kir.ExtractionPlan
import io.github.vad4nus.lockstep.skie.kir.LOCKSTEP_PACKAGE
import io.github.vad4nus.lockstep.skie.kir.ModelExtractor
import io.github.vad4nus.lockstep.skie.kir.MvvmPlanner
import io.github.vad4nus.lockstep.skie.kir.ProxyPlanner
import io.github.vad4nus.lockstep.skie.kir.generatedSwiftName
import io.github.vad4nus.lockstep.skie.kir.requireFree
import io.github.vad4nus.lockstep.skie.kir.toMvvmModel
import io.github.vad4nus.lockstep.skie.kir.toProxyExtensions
import io.github.vad4nus.lockstep.skie.kir.toSwiftModel
import io.github.vad4nus.lockstep.skie.render.mvvmRuntimeSwift
import io.github.vad4nus.lockstep.skie.render.mvvmRuntimeSwiftTypes
import io.github.vad4nus.lockstep.skie.render.render
import io.github.vad4nus.lockstep.skie.render.renderProxies
import io.github.vad4nus.lockstep.skie.render.runtimeSwift
import io.github.vad4nus.lockstep.skie.render.runtimeSwiftTypes
import io.github.vad4nus.lockstep.skie.render.screensRuntimeSwift
import io.github.vad4nus.lockstep.skie.render.screensRuntimeSwiftTypes
import io.github.vad4nus.lockstep.skie.render.screensSwiftEnums
import io.github.vad4nus.lockstep.skie.render.screensSwiftTypes

private const val SWIFT_NAMESPACE = "Lockstep"

private const val SCREENS_PACKAGE = "$LOCKSTEP_PACKAGE.screens"

internal class LockstepRenamePhase(private val run: LockstepRun) : SirPhase {

    context(context: SirPhase.Context)
    override suspend fun execute() {
        val plan = ModelExtractor(context.kirProvider, run.classInfo, run.nullableValues, context.kirReporter).extract() ?: return
        run.plan = plan
        plan.generated.forEach { hide(it.primarySirClass) }
        plan.accessors.forEach { hideDeclarations(it.property) }
    }

    private fun hide(sirClass: SirClass) {
        sirClass.isHidden = true
        sirClass.isReplaced = true
    }

    private fun hideDeclarations(property: KirProperty) {
        if (!property.isFakeOverride) {
            property.primarySirProperty.isHidden = true
            property.primarySirProperty.isReplaced = true
        }
        property.overriddenDeclarations.forEach(::hideDeclarations)
    }
}

internal class LockstepSirPhase(private val run: LockstepRun) : SirPhase {

    context(context: SirPhase.Context)
    override suspend fun execute() {
        val plan = run.plan ?: return
        val model = plan.toSwiftModel {
            context.sirTypeTranslator.mapType(context.oirTypeTranslator.mapType(it)).evaluate().swiftPoetTypeName.toString()
        }
        val files = context.sirFileProvider
        val classes = context.kirProvider
        plan.requireFree(classes, runtimeSwiftTypes)
        files.getWrittenSourceFile(SWIFT_NAMESPACE, "LockstepRuntime").content =
            runtimeSwift(plan.stateFlow.swiftName, plan.flow.swiftName, plan.subscription.swiftName)
        files.getWrittenSourceFile(SWIFT_NAMESPACE, "LockstepGenerated").content = render(model.structs, model.enums, model.accessors)
        MvvmPlanner(plan, classes, context.kirReporter).plan()?.let {
            plan.requireFree(classes, mvvmRuntimeSwiftTypes)
            files.getWrittenSourceFile(SWIFT_NAMESPACE, "LockstepMvvmRuntime").content =
                mvvmRuntimeSwift(plan.toMvvmModel(it), model.enums.filter { enum -> enum.routeSwiftName != null })
        }
        val node = classes.findClassByFqName("$SCREENS_PACKAGE.Node")?.takeIf { it in plan.generated } ?: return
        val problems = screensProblems(plan, classes)
        if (problems.isEmpty()) {
            plan.requireFree(classes, screensRuntimeSwiftTypes)
            files.getWrittenSourceFile(SWIFT_NAMESPACE, "LockstepScreensRuntime").content = screensRuntimeSwift()
        } else {
            context.kirReporter.warning("lockstep: NodeView and NodeTheme are not generated: ${problems.joinToString("; ")}", node)
        }
    }

    /** Why the screens runtime cannot refer to the types it draws by their own names; empty when it can. */
    private fun screensProblems(plan: ExtractionPlan, classes: KirProvider): List<String> {
        fun problem(name: String, swiftName: String?): String? = when (swiftName) {
            name -> null
            null -> "$name is not part of a state that a flow exposes"
            else -> "$name has the Swift name $swiftName"
        }
        fun find(name: String): KirClass? = classes.findClassByFqName("$SCREENS_PACKAGE.$name")
        return screensSwiftTypes.mapNotNull { name -> problem(name, find(name)?.takeIf { it in plan.generated }?.let { generatedSwiftName(it.swiftName) }) } +
            screensSwiftEnums.mapNotNull { name -> problem(name, find(name)?.swiftName ?: name) }
    }
}

internal class LockstepProxyPhase(private val run: LockstepRun) : SirPhase {

    context(context: SirPhase.Context)
    override suspend fun execute() {
        val plan = run.plan ?: return
        val proxies = ProxyPlanner(plan, run.functionInfo, context.kirReporter).plan()
        val extensions = plan.toProxyExtensions(proxies)
        if (extensions.isEmpty()) return
        context.sirFileProvider.getWrittenSourceFile(SWIFT_NAMESPACE, "LockstepProxies").content = renderProxies(extensions, context.sirProvider.allUsedExternalModules.mapTo(mutableSetOf()) { it.name })
    }
}
