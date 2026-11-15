package io.github.vad4nus.lockstep.skie.kir

import co.touchlab.skie.kir.element.KirClass
import co.touchlab.skie.kir.element.KirProperty
import co.touchlab.skie.kir.type.KirType
import co.touchlab.skie.sir.element.call
import io.github.vad4nus.lockstep.skie.model.EnumCase
import io.github.vad4nus.lockstep.skie.model.Field
import io.github.vad4nus.lockstep.skie.model.FieldKind
import io.github.vad4nus.lockstep.skie.model.FlowAccessor
import io.github.vad4nus.lockstep.skie.model.FlowValue
import io.github.vad4nus.lockstep.skie.model.MvvmModel
import io.github.vad4nus.lockstep.skie.model.ProxyExtension
import io.github.vad4nus.lockstep.skie.model.ProxyFunction
import io.github.vad4nus.lockstep.skie.model.ProxyParameter
import io.github.vad4nus.lockstep.skie.model.SwiftEnum
import io.github.vad4nus.lockstep.skie.model.SwiftStruct
import io.github.vad4nus.lockstep.skie.render.referenceEnums
import io.github.vad4nus.lockstep.skie.render.swiftIdentifier

internal class SwiftModel(
    val structs: List<SwiftStruct>,
    val enums: List<SwiftEnum>,
    val accessors: List<FlowAccessor>,
)

private fun ExtractionPlan.kotlinName(kirClass: KirClass): String {
    val chain = generateSequence(kirClass) { it.parent as? KirClass }.toList().reversed()
    return kotlinSwiftName(kirClass.swiftName, chain.map { it in generated })
}

/** Builds the Swift model; [bridgedType] spells the Swift type a Kotlin type is bridged to outside a generic argument. */
internal fun ExtractionPlan.toSwiftModel(bridgedType: (KirType) -> String): SwiftModel {
    val raw = rawStringDesc?.let { "${kotlinName(it.owner)}.${it.primarySirConstructor.reference}" }

    fun field(planned: PlannedField): Field = Field(
        planned.property.swiftName,
        fieldKind(planned.type, raw) { swiftType(planned.property, enumNames) },
        planned.constructorParameter?.labelOrName,
    )

    return SwiftModel(
        structs = nodes.filterIsInstance<PlannedNode.Struct>().map {
            SwiftStruct(
                generatedSwiftName(it.kirClass.swiftName), kotlinName(it.kirClass), it.fields.map(::field),
                equatable = equatable.getValue(it.kirClass), hashable = it.kirClass in hashable, writable = it.kirClass in writable,
            )
        },
        enums = nodes.filterIsInstance<PlannedNode.Enum>().map { node ->
            val cases = node.cases.map {
                EnumCase(it.name, kotlinName(it.kirClass), it.fields.map(::field), it.kirClass.kind == KirClass.Kind.Object)
            }
            SwiftEnum(
                generatedSwiftName(node.kirClass.swiftName), kotlinName(node.kirClass), cases,
                equatable = equatable.getValue(node.kirClass),
                hashable = node.kirClass in hashable,
                routeSwiftName = route?.takeIf { node.kirClass in routes }?.let(::kotlinName),
                indirect = node.kirClass in indirect,
                writable = node.kirClass in writable,
            )
        },
        accessors = accessors.map {
            FlowAccessor(
                ownerSwiftName = kotlinName(it.owner),
                propertyName = it.property.swiftName,
                originalName = "__${it.property.swiftName}",
                flowKind = it.flowKind,
                value = flowValue(it, bridgedType),
                valueNullable = it.nullable,
            )
        },
    )
}

private fun ExtractionPlan.flowValue(accessor: PlannedAccessor, bridgedType: (KirType) -> String): FlowValue = when (val value = accessor.value) {
    is PlannedType.Generated -> FlowValue.Generated(generatedSwiftName(value.target.swiftName))
    is PlannedType.GeneratedList -> FlowValue.GeneratedList(generatedSwiftName(value.target.swiftName), kotlinName(value.target))
    is PlannedType.Boxed -> FlowValue.Boxed(value.swiftType, value.accessor)
    is PlannedType.Localized -> FlowValue.Localized
    is PlannedType.Passthrough -> FlowValue.Bridged(referenceEnums(bridgedType(accessor.valueType), enumNames))
}

internal fun ExtractionPlan.toMvvmModel(mvvm: MvvmPlan): MvvmModel = MvvmModel(
    androidViewModel = kotlinName(mvvm.androidViewModel),
    viewModelStore = kotlinName(mvvm.store),
    navigator = kotlinName(mvvm.navigator),
    route = kotlinName(mvvm.route),
    chrome = generatedSwiftName(mvvm.chrome.swiftName),
    uiError = generatedSwiftName(mvvm.uiError.swiftName),
    uiMessage = generatedSwiftName(mvvm.uiMessage.swiftName),
    navStack = generatedSwiftName(mvvm.navStack.swiftName),
    viewModels = mvvm.viewModels.map(::kotlinName),
)

private fun swiftType(property: KirProperty, enumNames: Set<String>): String =
    referenceEnums(property.primarySirProperty.type.evaluate().swiftPoetTypeName.toString(), enumNames)

private fun fieldKind(type: PlannedType, raw: String?, passthrough: () -> String): FieldKind = when (type) {
    is PlannedType.Boxed -> FieldKind.BoxedPrimitive(type.swiftType, type.accessor, type.box)
    is PlannedType.Generated -> FieldKind.Generated(generatedSwiftName(type.target.swiftName), type.nullable)
    is PlannedType.GeneratedList -> FieldKind.GeneratedList(generatedSwiftName(type.target.swiftName), type.nullable)
    is PlannedType.Localized -> FieldKind.Localized(type.nullable, raw)
    is PlannedType.Passthrough -> FieldKind.Passthrough(passthrough())
}

internal fun ExtractionPlan.toProxyExtensions(proxies: Map<KirClass, List<PlannedProxy>>): List<ProxyExtension> =
    proxies.filterValues { it.isNotEmpty() }.map { (kirClass, list) ->
        ProxyExtension(generatedSwiftName(kirClass.swiftName), list.map { proxyFunction(it, kirClass in writable) }, kirClass in writable)
    }

private fun ExtractionPlan.proxyFunction(proxy: PlannedProxy, ownerWritable: Boolean): ProxyFunction {
    val sir = proxy.function.primarySirFunction
    val parameters = proxy.parameters.mapIndexed { index, parameter ->
        val generated = parameter.type as? PlannedType.Generated
        val swiftType = generated?.let { generatedSwiftName(it.target.swiftName) }
            ?: referenceEnums(parameter.sir.type.evaluate().swiftPoetTypeName.toString(), enumNames)
        ProxyParameter(
            parameter.sir.label, parameter.sir.name, swiftType,
            kotlinBinding = generated?.let { "lockstepArg$index" },
            writable = generated != null && generated.target in writable,
        )
    }
    val total = ownerWritable && parameters.all { it.kotlinBinding == null || it.writable }
    val arguments = parameters.map { it.kotlinBinding ?: swiftIdentifier(it.name) }
    val result = proxy.result?.let { type ->
        fieldKind(type, raw = null) {
            val returned = sir.returnType.evaluate().swiftPoetTypeName
            referenceEnums((if (total) returned else returned.makeOptional()).toString(), enumNames)
        }
    }
    return ProxyFunction(sir.identifier, parameters, result, sir.call(arguments), total, total && proxy.function.kotlinName == "toString")
}
