package io.github.vad4nus.lockstep.skie.kir

import co.touchlab.skie.kir.KirProvider
import co.touchlab.skie.kir.element.KirClass
import co.touchlab.skie.kir.element.KirConstructor
import co.touchlab.skie.kir.element.KirProperty
import co.touchlab.skie.kir.element.KirScope
import co.touchlab.skie.kir.type.DeclaredKirType
import co.touchlab.skie.kir.type.KirType
import co.touchlab.skie.kir.type.NullableReferenceKirType
import co.touchlab.skie.kir.type.OirBasedKirType
import co.touchlab.skie.sir.element.SirValueParameter
import co.touchlab.skie.sir.element.isAccessible
import co.touchlab.skie.util.KirReporter
import io.github.vad4nus.lockstep.skie.model.FlowKind

/**
 * What the Kotlin descriptor tells about a class: [constructorOrder] names the parameters of the primary constructor,
 * and [constructor] is that constructor as Swift sees it, or null when it is not exported.
 */
internal class ClassInfo(val isData: Boolean, val constructorOrder: List<String>, val constructor: KirConstructor?)

internal sealed interface PlannedType {
    data class Generated(val target: KirClass, val nullable: Boolean) : PlannedType
    data class GeneratedList(val target: KirClass, val nullable: Boolean) : PlannedType
    data class Localized(val nullable: Boolean) : PlannedType
    data class Boxed(val swiftType: String, val accessor: String, val box: String) : PlannedType
    data class Passthrough(val equatable: Boolean) : PlannedType
}

/** A property of a generated type; [constructorParameter] is the parameter of the exported primary constructor it fills, if any. */
internal class PlannedField(val property: KirProperty, val type: PlannedType, val constructorParameter: SirValueParameter?)

internal class PlannedCase(val kirClass: KirClass, val name: String, val fields: List<PlannedField>)

internal sealed interface PlannedNode {
    val kirClass: KirClass

    class Struct(override val kirClass: KirClass, val fields: List<PlannedField>) : PlannedNode
    class Enum(override val kirClass: KirClass, val cases: List<PlannedCase>) : PlannedNode
}

/** A flow property that gets a Swift accessor; [valueType] is the type argument of the flow and [value] is how Swift reads it. */
internal class PlannedAccessor(
    val owner: KirClass,
    val property: KirProperty,
    val flowKind: FlowKind,
    val valueType: KirType,
    val value: PlannedType,
    val nullable: Boolean,
)

internal class ExtractionPlan(
    val nodes: List<PlannedNode>,
    val accessors: List<PlannedAccessor>,
    val equatable: Map<KirClass, Boolean>,
    val stateFlow: KirClass,
    val flow: KirClass,
    val subscription: KirClass,
    val enumNames: Set<String>,
    val hashable: Set<KirClass>,
    val route: KirClass?,
    val routes: Set<KirClass>,
    val indirect: Set<KirClass>,
    val writable: Set<KirClass>,
    val rawStringDesc: KirConstructor?,
) {
    val generated: Set<KirClass> = nodes.mapTo(mutableSetOf()) { it.kirClass }
}

/** Fails the build when an exported class has one of the Swift [names] a lockstep runtime file declares. */
internal fun ExtractionPlan.requireFree(kirProvider: KirProvider, names: Collection<String>) {
    kirProvider.kotlinClasses.forEach {
        val swiftName = if (it in generated) generatedSwiftName(it.swiftName) else it.swiftName
        if (swiftName in names) {
            error("lockstep: ${it.kotlinFqName} has the Swift name $swiftName, which the lockstep runtime uses; rename the class")
        }
    }
}

internal const val LOCKSTEP_PACKAGE = "io.github.vad4nus.lockstep"

internal val boxedPrimitives: Map<String, PlannedType.Boxed> = mapOf(
    "kotlin.Int" to PlannedType.Boxed("Int32", "int32Value", "KotlinInt(int:"),
    "kotlin.Long" to PlannedType.Boxed("Int64", "int64Value", "KotlinLong(longLong:"),
    "kotlin.Short" to PlannedType.Boxed("Int16", "int16Value", "KotlinShort(short:"),
    "kotlin.Byte" to PlannedType.Boxed("Int8", "int8Value", "KotlinByte(char:"),
    "kotlin.Double" to PlannedType.Boxed("Double", "doubleValue", "KotlinDouble(double:"),
    "kotlin.Float" to PlannedType.Boxed("Float", "floatValue", "KotlinFloat(float:"),
    "kotlin.Boolean" to PlannedType.Boxed("Bool", "boolValue", "KotlinBoolean(bool:"),
    "kotlin.UInt" to PlannedType.Boxed("UInt32", "uint32Value", "KotlinUInt(unsignedInt:"),
    "kotlin.ULong" to PlannedType.Boxed("UInt64", "uint64Value", "KotlinULong(unsignedLongLong:"),
    "kotlin.UShort" to PlannedType.Boxed("UInt16", "uint16Value", "KotlinUShort(unsignedShort:"),
    "kotlin.UByte" to PlannedType.Boxed("UInt8", "uint8Value", "KotlinUByte(unsignedChar:"),
)

internal fun boxed(type: KirType): PlannedType.Boxed? =
    ((type as? NullableReferenceKirType)?.nonNullType as? DeclaredKirType)?.declaration?.kotlinFqName?.let(boxedPrimitives::get)

internal fun KirClass.inherits(parent: KirClass): Boolean =
    resolvedSuperTypes.map { it.declaration }.any { it == parent || it.inherits(parent) }

internal fun declaredTarget(type: KirType): Pair<KirClass, Boolean>? = when (type) {
    is DeclaredKirType -> type.declaration to false
    is NullableReferenceKirType -> (type.nonNullType as? DeclaredKirType)?.declaration?.let { it to true }
    else -> null
}

private const val STRING_DESC = "dev.icerock.moko.resources.desc.StringDesc"

private const val RAW_STRING_DESC = "dev.icerock.moko.resources.desc.RawStringDesc"

private const val ROUTE = "$LOCKSTEP_PACKAGE.mvvm.navigation.Route"

private val collectionClasses = setOf(
    "platform.Foundation.NSArray", "platform.Foundation.NSMutableArray",
    "platform.Foundation.NSSet", "platform.Foundation.NSMutableSet",
    "platform.Foundation.NSDictionary", "platform.Foundation.NSMutableDictionary",
)

internal class ModelExtractor(
    private val kirProvider: KirProvider,
    private val classInfo: Map<String, ClassInfo>,
    private val nullableValues: Set<KirProperty>,
    private val reporter: KirReporter,
) {
    private val inProgress = LinkedHashSet<KirClass>()
    private val indirect = mutableSetOf<KirClass>()
    private val nodes = LinkedHashMap<KirClass, PlannedNode>()
    private val candidates = HashMap<KirClass, Boolean>()

    fun extract(): ExtractionPlan? {
        val stateFlow = kirProvider.findClassByFqName("$LOCKSTEP_PACKAGE.UiStateFlow") ?: return null
        val flow = kirProvider.findClassByFqName("$LOCKSTEP_PACKAGE.UiFlow") ?: return null
        val subscription = kirProvider.findClassByFqName("$LOCKSTEP_PACKAGE.UiSubscription") ?: return null
        val accessors = collectAccessors(stateFlow, flow).ifEmpty { return null }
        val routeClass = kirProvider.findClassByFqName(ROUTE)
        val routeRoots = routeClass?.let(::discoverRoutes).orEmpty()
        routeRoots.forEach(::visit)
        val enumNames = kirProvider.kotlinEnums.mapTo(mutableSetOf()) { it.swiftName }
        val equatable = fixedPoint(::equatableField)
        val hashable = hashableClosure(routeRoots)
        val routes = routeRoots.filterTo(mutableSetOf()) { it in hashable }
        val rawStringDesc = kirProvider.findClassByFqName(RAW_STRING_DESC)
            ?.takeIf { constructorParameters(it)?.size == 1 }?.let { classInfo.getValue(it.kotlinFqName).constructor }
        val writable = fixedPoint({ type, result -> writableField(type, result, rawStringDesc != null) }, ::constructible).filterValues { it }.keys
        val plan = ExtractionPlan(
            nodes = nodes.values.toList(), accessors = accessors, equatable = equatable, stateFlow = stateFlow, flow = flow,
            subscription = subscription, enumNames = enumNames, hashable = hashable, route = routeClass, routes = routes,
            indirect = indirect, writable = writable, rawStringDesc = rawStringDesc,
        )
        plan.nodes.filterIsInstance<PlannedNode.Struct>().filter { node -> node.fields.any { it.property.swiftName == "kotlin" } }.forEach {
            error("lockstep: ${it.kirClass.kotlinFqName} has a property named kotlin, which the generated struct uses for the Kotlin object; rename the property")
        }
        val clashes = duplicates(plan.nodes.map { generatedSwiftName(it.kirClass.swiftName) })
        check(clashes.isEmpty()) { "lockstep: generated Swift names clash: $clashes" }
        return plan
    }

    private fun members(kirClass: KirClass): List<KirProperty> {
        val order = classInfo[kirClass.kotlinFqName]?.constructorOrder.orEmpty()
        return kirClass.callableDeclarations.filterIsInstance<KirProperty>()
            .filter { it.scope == KirScope.Member }
            .sortedBy { order.indexOf(it.kotlinName).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
    }

    /**
     * The flow properties that get a Swift accessor: those of classes and objects that are not generic, with a flow
     * that is not nullable and carries objects of a class. A generic class is left to its subclasses without a word;
     * every other flow that is skipped is reported.
     */
    private fun collectAccessors(stateFlow: KirClass, flow: KirClass): List<PlannedAccessor> =
        kirProvider.kotlinClasses
            .filter { it.typeParameters.isEmpty() }
            .sortedBy { it.kotlinFqName }
            .flatMap { owner ->
                val properties = if (owner.kind == KirClass.Kind.File) owner.callableDeclarations.filterIsInstance<KirProperty>() else members(owner)
                properties.mapNotNull { property ->
                    val type = (property.type as? NullableReferenceKirType)?.nonNullType ?: property.type
                    val flowKind = when ((type as? DeclaredKirType)?.declaration) {
                        stateFlow -> FlowKind.State
                        flow -> FlowKind.Events
                        else -> return@mapNotNull null
                    }
                    val valueType = type.typeArguments.singleOrNull()
                    val valueClass = valueType?.let(::declaredTarget)?.first
                    val reason = when {
                        owner.kind == KirClass.Kind.File -> "it is a top-level property; move it to a class or an object"
                        owner.kind == KirClass.Kind.Interface -> "it belongs to an interface; read it through the class that implements it"
                        owner.kind == KirClass.Kind.Enum -> "it belongs to an enum; move it to a class"
                        type !== property.type -> "the flow is nullable"
                        valueClass == null -> "its values are not objects of a class"
                        else -> null
                    }
                    if (reason != null) {
                        if (!property.isFakeOverride) skipped(property, reason)
                        return@mapNotNull null
                    }
                    if (inheritsAccessor(owner, property, setOf(stateFlow, flow))) return@mapNotNull null
                    val value = boxedPrimitives[valueClass!!.kotlinFqName] ?: classify(valueType, property)
                    PlannedAccessor(owner, property, flowKind, valueType, value, property in nullableValues)
                }
            }

    private fun skipped(property: KirProperty, reason: String) {
        reporter.warning("lockstep: ${property.owner.kotlinFqName}.${property.kotlinName} gets no Swift accessor and stays a Kotlin flow: $reason", property)
    }

    /** Whether the nearest class above [owner] that gets accessors has one for the flow [property] overrides. */
    private fun inheritsAccessor(owner: KirClass, property: KirProperty, flows: Set<KirClass>): Boolean {
        val parent = owner.resolvedSuperTypes.map { it.declaration }
            .firstOrNull { it.kind == KirClass.Kind.Class && it.kotlinFqName != "kotlin.Any" && it.typeParameters.isEmpty() } ?: return false
        return members(parent).any { it.kotlinName == property.kotlinName && (it.type as? DeclaredKirType)?.declaration in flows }
    }

    private fun generatedTarget(type: KirType): Pair<KirClass, Boolean>? = declaredTarget(type)?.takeIf { isCandidate(it.first) }

    private fun isCandidate(kirClass: KirClass): Boolean = candidates.getOrPut(kirClass) {
        when {
            kirClass.typeParameters.isNotEmpty() -> false
            kirClass.kind == KirClass.Kind.Class && classInfo[kirClass.kotlinFqName]?.isData == true -> true
            kirClass.isSealed && (kirClass.kind == KirClass.Kind.Class || kirClass.kind == KirClass.Kind.Interface) -> sealedSupported(kirClass)
            else -> false
        }
    }

    private fun sealedSupported(kirClass: KirClass): Boolean {
        if (kirClass.hasUnexposedSealedSubclasses) {
            reporter.warning("lockstep: ${kirClass.kotlinFqName} has unexposed sealed subclasses and is passed through as a Kotlin type", kirClass)
            return false
        }
        val unsupported = kirClass.sealedSubclasses.firstOrNull {
            it.isSealed || it.typeParameters.isNotEmpty() || it.kind != KirClass.Kind.Class && it.kind != KirClass.Kind.Object
        }
        if (unsupported != null) {
            reporter.warning("lockstep: ${kirClass.kotlinFqName} has the unsupported subclass ${unsupported.kotlinFqName} and is passed through as a Kotlin type", kirClass)
            return false
        }
        val names = kirClass.sealedSubclasses.map { caseName(it.kotlinFqName) }
        val clashes = duplicates(names)
        check(clashes.isEmpty()) { "lockstep: ${kirClass.kotlinFqName} has sealed subclasses with the same case name: $clashes" }
        return true
    }

    private fun visit(kirClass: KirClass) {
        if (kirClass in nodes || kirClass in inProgress) return
        inProgress += kirClass
        nodes[kirClass] = if (kirClass.isSealed) {
            PlannedNode.Enum(kirClass, kirClass.sealedSubclasses.map { sub ->
                PlannedCase(sub, caseName(sub.kotlinFqName), if (sub.kind == KirClass.Kind.Object) emptyList() else fields(sub))
            })
        } else {
            PlannedNode.Struct(kirClass, fields(kirClass))
        }
        inProgress -= kirClass
    }

    private fun fields(kirClass: KirClass): List<PlannedField> {
        val order = classInfo[kirClass.kotlinFqName]?.constructorOrder.orEmpty()
        val parameters = constructorParameters(kirClass)
        return members(kirClass).map { PlannedField(it, classify(it.type, it), parameters?.getOrNull(order.indexOf(it.kotlinName))) }
    }

    /** The parameters of the primary constructor as Swift calls it, or null when Swift cannot call it. */
    private fun constructorParameters(kirClass: KirClass): List<SirValueParameter>? {
        val info = classInfo[kirClass.kotlinFqName] ?: return null
        val constructor = info.constructor?.primarySirConstructor ?: return null
        return constructor.valueParameters.takeIf { constructor.isAccessible && !constructor.isHidden && it.size == info.constructorOrder.size }
    }

    /** How Swift reads a value of [type], which is the type of [property] or of the values of its flow. */
    private fun classify(type: KirType, property: KirProperty): PlannedType {
        boxed(type)?.let { return it }
        declaredTarget(type)?.takeIf { it.first.kotlinFqName == STRING_DESC }?.let { return PlannedType.Localized(it.second) }
        listElement(type)?.let { (element, nullable) ->
            visit(element)
            return PlannedType.GeneratedList(element, nullable)
        }
        val target = generatedTarget(type) ?: return PlannedType.Passthrough(kirEquatable(type))
        if (target.first in inProgress) {
            val sealed = inProgress.dropWhile { it != target.first }.firstOrNull { it.isSealed }
            if (sealed == null) {
                reporter.warning("lockstep: ${property.owner.kotlinFqName}.${property.kotlinName} closes a reference cycle of data classes through ${target.first.kotlinFqName} and is passed through as a Kotlin type; put a list or a sealed type on the cycle", property)
                return PlannedType.Passthrough(equatable = true)
            }
            indirect += sealed
        }
        visit(target.first)
        return PlannedType.Generated(target.first, target.second)
    }

    private fun listElement(type: KirType): Pair<KirClass, Boolean>? {
        val (list, nullable) = when (type) {
            is DeclaredKirType -> type to false
            is NullableReferenceKirType -> (type.nonNullType as? DeclaredKirType)?.let { it to true } ?: return null
            else -> return null
        }
        if (list.declaration.kotlinFqName != "platform.Foundation.NSArray") return null
        val element = list.typeArguments.singleOrNull()?.let(::generatedTarget) ?: return null
        return element.takeIf { !it.second }?.let { it.first to nullable }
    }

    private fun discoverRoutes(route: KirClass): List<KirClass> =
        kirProvider.kotlinClasses
            .filter { it.isSealed && it.typeParameters.isEmpty() && it.inherits(route) && isCandidate(it) }
            .sortedBy { it.kotlinFqName }

    private fun hashableClosure(routeRoots: List<KirClass>): Set<KirClass> {
        if (routeRoots.isEmpty()) return emptySet()
        val result = fixedPoint(::hashableField)
        val closures = routeRoots.associateWith(::closure)
        routeRoots.filterNot { result.getValue(it) }.forEach { root ->
            val culprit = closures.getValue(root).firstNotNullOfOrNull { kirClass ->
                nodes[kirClass]?.let(::nodeFields).orEmpty().firstOrNull { !hashableField(it.type, result) }?.let { kirClass to it }
            }
            val where = culprit?.let { "${it.first.kotlinFqName}.${it.second.property.kotlinName}" } ?: root.kotlinFqName
            reporter.warning("lockstep: route ${root.kotlinFqName} is not Hashable in Swift because $where has a type that cannot be hashed (an interface, Any, a list of generated types or a StringDesc); use a plain value in the route", root)
        }
        return closures.values.flatten().filterTo(mutableSetOf()) { result.getValue(it) }
    }

    private fun closure(root: KirClass): Set<KirClass> {
        val closure = LinkedHashSet<KirClass>()
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            val kirClass = queue.removeFirst()
            if (!closure.add(kirClass)) continue
            nodes[kirClass]?.let(::nodeFields).orEmpty().forEach { field ->
                (field.type as? PlannedType.Generated)?.let { queue.add(it.target) }
            }
        }
        return closure
    }

    private fun nodeFields(node: PlannedNode): List<PlannedField> = when (node) {
        is PlannedNode.Struct -> node.fields
        is PlannedNode.Enum -> node.cases.flatMap { it.fields }
    }

    private fun hashableField(type: PlannedType, result: Map<KirClass, Boolean>): Boolean = when (type) {
        is PlannedType.Boxed -> true
        is PlannedType.Generated -> result.getValue(type.target)
        is PlannedType.Passthrough -> type.equatable
        is PlannedType.GeneratedList, is PlannedType.Localized -> false
    }

    private fun writableField(type: PlannedType, result: Map<KirClass, Boolean>, rawStrings: Boolean): Boolean = when (type) {
        is PlannedType.Boxed, is PlannedType.Passthrough -> true
        is PlannedType.Generated -> result.getValue(type.target)
        is PlannedType.GeneratedList -> result.getValue(type.target)
        is PlannedType.Localized -> rawStrings
    }

    private fun constructible(node: PlannedNode): Boolean = when (node) {
        is PlannedNode.Struct -> constructible(node.kirClass, node.fields)
        is PlannedNode.Enum -> node.cases.all { it.kirClass.kind == KirClass.Kind.Object || constructible(it.kirClass, it.fields) }
    }

    private fun constructible(kirClass: KirClass, fields: List<PlannedField>): Boolean =
        constructorParameters(kirClass) != null &&
            classInfo.getValue(kirClass.kotlinFqName).constructorOrder.all { name -> fields.any { it.property.kotlinName == name } }

    private fun kirEquatable(type: KirType): Boolean = when (type) {
        is OirBasedKirType -> true
        is NullableReferenceKirType -> kirEquatable(type.nonNullType)
        is DeclaredKirType -> when {
            type.declaration.kotlinFqName == "kotlin.Any" -> false
            type.declaration.kotlinFqName in collectionClasses ->
                type.typeArguments.isNotEmpty() && type.typeArguments.all(::kirEquatable)
            else -> type.declaration.kind != KirClass.Kind.Interface
        }
        else -> false
    }

    private fun equatableField(type: PlannedType, result: Map<KirClass, Boolean>): Boolean = when (type) {
        is PlannedType.Boxed, is PlannedType.Localized -> true
        is PlannedType.Generated -> result.getValue(type.target)
        is PlannedType.GeneratedList -> result.getValue(type.target)
        is PlannedType.Passthrough -> type.equatable
    }

    private fun fixedPoint(
        fieldOk: (PlannedType, Map<KirClass, Boolean>) -> Boolean,
        nodeOk: (PlannedNode) -> Boolean = { true },
    ): Map<KirClass, Boolean> {
        val result = nodes.values.associateTo(mutableMapOf()) { it.kirClass to nodeOk(it) }
        var changed = true
        while (changed) {
            changed = false
            nodes.values.forEach { node ->
                if (result.getValue(node.kirClass) && !nodeFields(node).all { fieldOk(it.type, result) }) {
                    result[node.kirClass] = false
                    changed = true
                }
            }
        }
        return result
    }
}
