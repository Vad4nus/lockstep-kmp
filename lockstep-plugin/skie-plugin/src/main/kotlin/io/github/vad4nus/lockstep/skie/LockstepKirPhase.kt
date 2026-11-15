package io.github.vad4nus.lockstep.skie

import co.touchlab.skie.context.KirPhaseContext
import co.touchlab.skie.phases.KirPhase
import io.github.vad4nus.lockstep.skie.kir.ClassInfo
import co.touchlab.skie.kir.element.KirProperty
import co.touchlab.skie.kir.element.KirSimpleFunction
import io.github.vad4nus.lockstep.skie.kir.ExtractionPlan
import io.github.vad4nus.lockstep.skie.kir.FunctionInfo
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.types.isNullable

internal class LockstepRun {
    var classInfo: Map<String, ClassInfo> = emptyMap()
    var functionInfo: Map<KirSimpleFunction, FunctionInfo> = emptyMap()

    /** The properties whose type has one type argument and it is nullable; the exported types do not keep that. */
    var nullableValues: Set<KirProperty> = emptySet()
    var plan: ExtractionPlan? = null
}

internal class LockstepKirPhase(private val run: LockstepRun) : KirPhase {

    context(context: KirPhase.Context)
    override suspend fun execute() {
        val descriptors = (context as KirPhaseContext).descriptorKirProvider
        run.classInfo = context.kirProvider.kotlinClasses.mapNotNull { kirClass ->
            val descriptor = descriptors.findClassDescriptor(kirClass) ?: return@mapNotNull null
            val primary = descriptor.unsubstitutedPrimaryConstructor
            val order = primary?.valueParameters?.map { it.name.asString() }.orEmpty()
            kirClass.kotlinFqName to ClassInfo(descriptor.isData, order, primary?.let(descriptors::findConstructor))
        }.toMap()
        run.nullableValues = context.kirProvider.kotlinClasses
            .flatMap { it.callableDeclarations.filterIsInstance<KirProperty>() }
            .filterTo(mutableSetOf()) {
                descriptors.getCallableDeclarationDescriptor(it).returnType?.arguments?.singleOrNull()?.type?.isNullable() == true
            }
        run.functionInfo = context.kirProvider.kotlinClasses
            .flatMap { it.callableDeclarations.filterIsInstance<KirSimpleFunction>() }
            .associateWith {
                val descriptor = descriptors.getCallableDeclarationDescriptor(it)
                FunctionInfo(descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED, descriptor.typeParameters.isNotEmpty())
            }
    }
}
