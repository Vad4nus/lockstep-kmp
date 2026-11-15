package io.github.vad4nus.lockstep.skie.kir

import co.touchlab.skie.kir.element.KirCallableDeclaration
import co.touchlab.skie.kir.element.KirClass
import co.touchlab.skie.kir.element.KirScope
import co.touchlab.skie.kir.element.KirSimpleFunction
import co.touchlab.skie.kir.element.KirValueParameter
import co.touchlab.skie.kir.type.KirType
import co.touchlab.skie.oir.element.OirFunction
import co.touchlab.skie.oir.type.VoidOirType
import co.touchlab.skie.sir.element.SirValueParameter
import co.touchlab.skie.sir.element.isAccessible
import co.touchlab.skie.util.KirReporter

internal class FunctionInfo(val synthesized: Boolean, val generic: Boolean)

internal class PlannedProxyParameter(val sir: SirValueParameter, val type: PlannedType)

internal class PlannedProxy(val function: KirSimpleFunction, val parameters: List<PlannedProxyParameter>, val result: PlannedType?)

private const val RESERVED_LOCAL_PREFIX = "lockstep"

internal class ProxyPlanner(
    private val plan: ExtractionPlan,
    private val functionInfo: Map<KirSimpleFunction, FunctionInfo>,
    private val reporter: KirReporter,
) {
    private val nodes = plan.nodes.associateBy { it.kirClass }

    fun plan(): Map<KirClass, List<PlannedProxy>> =
        plan.nodes.filterIsInstance<PlannedNode.Struct>().associate { it.kirClass to planStruct(it) }

    private fun planStruct(struct: PlannedNode.Struct): List<PlannedProxy> {
        val reserved = struct.fields.mapTo(mutableSetOf("kotlin", "init")) { it.property.swiftName }
        val seen = mutableSetOf<String>()
        return struct.kirClass.callableDeclarations
            .filterIsInstance<KirSimpleFunction>()
            .filter(::isCandidate)
            .sortedBy { it.kotlinName }
            .mapNotNull { planFunction(struct.kirClass, it, reserved, seen) }
    }

    private fun isCandidate(function: KirSimpleFunction): Boolean =
        function.kind == KirSimpleFunction.Kind.Function &&
            function.scope == KirScope.Member &&
            !function.isFakeOverride &&
            function.origin != KirCallableDeclaration.Origin.Global &&
            functionInfo[function]?.synthesized != true

    private fun planFunction(owner: KirClass, function: KirSimpleFunction, reserved: Set<String>, seen: MutableSet<String>): PlannedProxy? {
        val sir = function.primarySirFunction
        val kirParameters = function.valueParameters.filter { it.kind == KirValueParameter.Kind.ValueParameter }
        val types = kirParameters.map { classify(it.type) }
        val signature = sir.identifier + sir.valueParameters.joinToString("") { "${it.labelOrName}:" }
        val reason = when {
            function.isSuspend || sir.isAsync -> "suspend functions are not supported"
            functionInfo[function]?.generic != false || sir.typeParameters.isNotEmpty() -> "generic functions are not supported"
            sir.throws || function.errorHandlingStrategy != OirFunction.ErrorHandlingStrategy.Crashes -> "throwing functions are not supported"
            !sir.visibility.isAccessible || sir.isHidden || sir.isReplaced -> "SKIE hides or replaces it"
            kirParameters.size != sir.valueParameters.size -> "its parameters do not match the exported ones"
            sir.valueParameters.any { it.name.startsWith(RESERVED_LOCAL_PREFIX) } -> "a parameter name starts with $RESERVED_LOCAL_PREFIX"
            sir.identifier in reserved -> "its name clashes with a field or with kotlin"
            else -> types.firstNotNullOfOrNull(::parameterProblem) ?: "another proxy has the same Swift signature".takeUnless { seen.add(signature) }
        }
        if (reason != null) {
            reporter.warning("lockstep: ${owner.kotlinFqName}.${function.kotlinName} has no Swift proxy: $reason; call it through kotlin", function)
            return null
        }
        val result = if (function.oirSimpleFunction.returnType == VoidOirType) null else classify(function.returnType)
        return PlannedProxy(function, sir.valueParameters.zip(types, ::PlannedProxyParameter), result)
    }

    private fun parameterProblem(type: PlannedType): String? = when {
        type is PlannedType.Boxed -> "a nullable primitive parameter is not supported"
        type is PlannedType.Generated && type.nullable -> "a nullable parameter of a generated type is not supported"
        type is PlannedType.Generated && nodes.getValue(type.target) is PlannedNode.Enum -> "a parameter of a generated enum type is not supported"
        else -> null
    }

    private fun classify(type: KirType): PlannedType =
        boxed(type)
            ?: declaredTarget(type)?.takeIf { it.first in nodes }?.let { PlannedType.Generated(it.first, it.second) }
            ?: PlannedType.Passthrough(equatable = true)
}
