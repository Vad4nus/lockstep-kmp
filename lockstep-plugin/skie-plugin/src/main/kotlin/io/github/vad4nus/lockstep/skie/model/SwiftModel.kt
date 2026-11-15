package io.github.vad4nus.lockstep.skie.model

internal sealed interface FieldKind {
    data class Passthrough(val swiftType: String) : FieldKind
    data class BoxedPrimitive(val swiftType: String, val accessor: String, val box: String) : FieldKind
    data class Generated(val typeName: String, val nullable: Boolean) : FieldKind
    data class GeneratedList(val typeName: String, val nullable: Boolean) : FieldKind

    /** A `StringDesc` read as its localized text; [raw] is the Swift function that turns a text back into one, if exported. */
    data class Localized(val nullable: Boolean, val raw: String? = null) : FieldKind
}

/**
 * A field of a generated type. [constructorLabel] is the argument label it has in the Kotlin constructor, `_` for
 * none, or null when the constructor does not take it.
 */
internal data class Field(val swiftName: String, val kind: FieldKind, val constructorLabel: String? = swiftName)

internal data class SwiftStruct(
    val name: String,
    val kotlinSwiftName: String,
    val fields: List<Field>,
    val equatable: Boolean,
    val hashable: Boolean = false,
    val writable: Boolean = false,
)

internal data class EnumCase(
    val name: String,
    val kotlinCaseSwiftName: String,
    val fields: List<Field>,
    val isObject: Boolean = false,
)

internal data class SwiftEnum(
    val name: String,
    val kotlinSwiftName: String,
    val cases: List<EnumCase>,
    val equatable: Boolean,
    val hashable: Boolean = false,
    val routeSwiftName: String? = null,
    val indirect: Boolean = false,
    val writable: Boolean = false,
)

internal enum class FlowKind { State, Events }

/** What a flow carries and how its Kotlin value becomes the Swift one. */
internal sealed interface FlowValue {
    data class Generated(val typeName: String) : FlowValue

    /** A list of a generated type; [kotlinSwiftName] is the Kotlin class of an element as Swift sees it. */
    data class GeneratedList(val typeName: String, val kotlinSwiftName: String) : FlowValue

    /** A boxed number or boolean, read through [accessor]. */
    data class Boxed(val swiftType: String, val accessor: String) : FlowValue

    /** A `StringDesc`, read as its localized text. */
    data object Localized : FlowValue

    /** A value Swift bridges by itself: a string, an enum, a collection or an object of a class. */
    data class Bridged(val swiftType: String) : FlowValue
}

internal data class FlowAccessor(
    val ownerSwiftName: String,
    val propertyName: String,
    val originalName: String,
    val flowKind: FlowKind,
    val value: FlowValue,
    val valueNullable: Boolean,
)

/** A parameter of a proxy; one of a generated type is passed as [kotlinBinding], built from the struct when it is [writable]. */
internal data class ProxyParameter(
    val label: String?,
    val name: String,
    val swiftType: String,
    val kotlinBinding: String?,
    val writable: Boolean = false,
)

/**
 * A Kotlin method repeated on the generated struct. A [total] one always runs; another returns nil without a Kotlin
 * object. A total `toString` is the [description] of the struct, so printing the struct prints what Kotlin prints.
 */
internal data class ProxyFunction(
    val identifier: String,
    val parameters: List<ProxyParameter>,
    val result: FieldKind?,
    val call: String,
    val total: Boolean = false,
    val description: Boolean = false,
)

internal data class ProxyExtension(val structName: String, val functions: List<ProxyFunction>, val writable: Boolean = false)

internal data class MvvmModel(
    val androidViewModel: String,
    val viewModelStore: String,
    val navigator: String,
    val route: String,
    val chrome: String,
    val uiError: String,
    val uiMessage: String,
    val navStack: String,
    val viewModels: List<String>,
)
