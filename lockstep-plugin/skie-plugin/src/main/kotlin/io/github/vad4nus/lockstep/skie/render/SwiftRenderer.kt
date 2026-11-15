package io.github.vad4nus.lockstep.skie.render

import io.github.vad4nus.lockstep.skie.model.EnumCase
import io.github.vad4nus.lockstep.skie.model.Field
import io.github.vad4nus.lockstep.skie.model.FieldKind
import io.github.vad4nus.lockstep.skie.model.FlowAccessor
import io.github.vad4nus.lockstep.skie.model.FlowKind
import io.github.vad4nus.lockstep.skie.model.FlowValue
import io.github.vad4nus.lockstep.skie.model.ProxyExtension
import io.github.vad4nus.lockstep.skie.model.ProxyFunction
import io.github.vad4nus.lockstep.skie.model.SwiftEnum
import io.github.vad4nus.lockstep.skie.model.SwiftStruct

internal fun render(structs: List<SwiftStruct>, enums: List<SwiftEnum>, accessors: List<FlowAccessor>): String {
    val blocks = structs.map(::renderStruct) + enums.map(::renderEnum) + accessors.map(::renderAccessor)
    return "import Foundation\n\n" + blocks.joinToString("\n\n") + "\n"
}

internal fun renderProxies(extensions: List<ProxyExtension>, modules: Set<String> = emptySet()): String {
    val types = extensions.flatMap { it.functions }.flatMap { function -> function.parameters.map { it.swiftType } + listOfNotNull(function.result?.optionalType()) }
    val imports = (listOf("Foundation") + importsFor(types, modules)).joinToString("\n") { "import $it" }
    return "$imports\n\n" + extensions.joinToString("\n\n", transform = ::renderProxyExtension) + "\n"
}

private fun renderProxyExtension(extension: ProxyExtension): String {
    val (descriptions, functions) = extension.functions.partition { it.description }
    val blocks = listOfNotNull(
        functions.takeIf { it.isNotEmpty() }?.let { list ->
            "extension ${extension.structName} {\n" + list.joinToString("\n\n") { renderProxy(it, extension.writable) } + "\n}"
        },
    ) + descriptions.map {
        """
            extension ${extension.structName}: CustomStringConvertible {
                public var description: Swift.String {
                    kotlinValue.${it.call}
                }
            }
        """.trimIndent()
    }
    return blocks.joinToString("\n\n")
}

private class ProxyBinding(val name: String, val source: String, val writable: Boolean)

private fun renderProxy(function: ProxyFunction, ownerWritable: Boolean): String {
    val parameters = function.parameters.joinToString(", ") {
        val name = swiftIdentifier(it.name)
        "${if (it.label == null) "" else "${it.label} "}$name: ${it.swiftType}"
    }
    val bindings = listOf(ProxyBinding("lockstepSelf", "self", ownerWritable)) + function.parameters.mapNotNull { parameter ->
        parameter.kotlinBinding?.let { ProxyBinding(it, swiftIdentifier(parameter.name), parameter.writable) }
    }
    val guards = bindings.filterNot { it.writable }.joinToString(", ") { "let ${it.name} = ${it.source}.kotlin" }
    val result = function.result
    val invocation = "lockstepSelf.${function.call}"
    return buildString {
        appendLine("    public func ${swiftIdentifier(function.identifier)}($parameters)${result?.let { " -> ${if (function.total) it.type() else it.optionalType()}" }.orEmpty()} {")
        bindings.filter { it.writable }.forEach { appendLine("        let ${it.name} = ${it.source}.kotlinValue") }
        if (guards.isNotEmpty()) appendLine("        guard $guards else { return${if (result == null) "" else " nil"} }")
        append("        ${if (result == null) invocation else "return ${result.read(invocation)}"}")
        appendLine()
        append("    }")
    }
}

private fun conformance(equatable: Boolean, hashable: Boolean = false) = when {
    hashable -> ": Hashable"
    equatable -> ": Equatable"
    else -> ""
}

private fun FieldKind.type(): String = when (this) {
    is FieldKind.Passthrough -> swiftType
    is FieldKind.BoxedPrimitive -> "$swiftType?"
    is FieldKind.Generated -> if (nullable) "$typeName?" else typeName
    is FieldKind.GeneratedList -> if (nullable) "[$typeName]?" else "[$typeName]"
    is FieldKind.Localized -> if (nullable) "Swift.String?" else "Swift.String"
}

private fun Field.type(): String = kind.type()

private fun FieldKind.read(expression: String): String = when (this) {
    is FieldKind.Passthrough -> expression
    is FieldKind.BoxedPrimitive -> "$expression?.$accessor"
    is FieldKind.Generated -> if (nullable) "$expression.map($typeName.init)" else "$typeName($expression)"
    is FieldKind.GeneratedList -> if (nullable) "$expression.map { \$0.map($typeName.init) }" else "$expression.map($typeName.init)"
    is FieldKind.Localized -> if (nullable) "$expression?.localized()" else "$expression.localized()"
}

private fun FieldKind.write(expression: String): String = when (this) {
    is FieldKind.Passthrough -> expression
    is FieldKind.BoxedPrimitive -> "$expression.map { $box \$0) }"
    is FieldKind.Generated -> if (nullable) "$expression?.kotlinValue" else "$expression.kotlinValue"
    is FieldKind.GeneratedList -> if (nullable) "$expression?.map { \$0.kotlinValue }" else "$expression.map { \$0.kotlinValue }"
    is FieldKind.Localized -> when {
        raw == null -> error("lockstep: a StringDesc field cannot be written back to Kotlin without RawStringDesc")
        nullable -> "$expression.map($raw)"
        else -> "$raw($expression)"
    }
}

private fun FieldKind.optionalType(): String = when (this) {
    is FieldKind.Passthrough -> swiftType
    is FieldKind.BoxedPrimitive -> "$swiftType?"
    is FieldKind.Generated -> "$typeName?"
    is FieldKind.GeneratedList -> "[$typeName]?"
    is FieldKind.Localized -> "Swift.String?"
}

private fun constructorArguments(fields: List<Field>): String =
    fields.filter { it.constructorLabel != null }.joinToString(", ") {
        val value = it.kind.write(swiftIdentifier(it.swiftName))
        if (it.constructorLabel == "_") value else "${swiftIdentifier(it.constructorLabel!!)}: $value"
    }

private fun Field.read(source: String): String = kind.read("$source.${swiftIdentifier(swiftName)}")

private fun renderStruct(struct: SwiftStruct): String {
    val kotlinInit = buildString {
        appendLine("    public init(_ kotlin: ${struct.kotlinSwiftName}) {")
        struct.fields.forEach { appendLine("        self.${swiftIdentifier(it.swiftName)} = ${it.read("kotlin")}") }
        appendLine("        self.kotlin = kotlin")
        append("    }")
    }
    val equality = buildString {
        appendLine("    public static func == (lhs: ${struct.name}, rhs: ${struct.name}) -> Bool {")
        val comparison = struct.fields.joinToString(" && ") { "lhs.${swiftIdentifier(it.swiftName)} == rhs.${swiftIdentifier(it.swiftName)}" }
        appendLine("        ${comparison.ifEmpty { "true" }}")
        append("    }")
    }
    return buildString {
        appendLine("public struct ${struct.name}${conformance(struct.equatable, struct.hashable)} {")
        struct.fields.forEach { appendLine("    public let ${swiftIdentifier(it.swiftName)}: ${it.type()}") }
        appendLine("    public let kotlin: ${struct.kotlinSwiftName}?")
        appendLine()
        if (struct.fields.isNotEmpty()) {
            val parameters = struct.fields.joinToString(", ") { "${swiftIdentifier(it.swiftName)}: ${it.type()}" }
            appendLine("    public init($parameters) {")
            struct.fields.forEach { appendLine("        self.${swiftIdentifier(it.swiftName)} = ${swiftIdentifier(it.swiftName)}") }
            appendLine("        self.kotlin = nil")
            appendLine("    }")
            appendLine()
        }
        append(kotlinInit)
        if (struct.equatable) {
            appendLine()
            appendLine()
            append(equality)
        }
        if (struct.hashable) {
            appendLine()
            appendLine()
            appendLine("    public func hash(into hasher: inout Hasher) {")
            struct.fields.forEach { appendLine("        hasher.combine(${swiftIdentifier(it.swiftName)})") }
            append("    }")
        }
        if (struct.writable) {
            appendLine()
            appendLine()
            appendLine("    public var kotlinValue: ${struct.kotlinSwiftName} {")
            appendLine("        kotlin ?? ${struct.kotlinSwiftName}(${constructorArguments(struct.fields)})")
            append("    }")
        }
        appendLine()
        append("}")
    }
}

private fun EnumCase.payload(transform: (Field) -> String): String =
    if (fields.isEmpty()) "" else fields.joinToString(", ", "(", ")") { "${swiftIdentifier(it.swiftName)}: ${transform(it)}" }

private fun EnumCase.branch(): String {
    val construction = ".${swiftIdentifier(name)}${payload { it.read("c") }}"
    val test = if (fields.isEmpty()) "kotlin is $kotlinCaseSwiftName" else "let c = kotlin as? $kotlinCaseSwiftName"
    return "        if $test { self = $construction; return }"
}

private fun EnumCase.construction(): String = when {
    isObject -> "$kotlinCaseSwiftName.shared"
    else -> "$kotlinCaseSwiftName(${constructorArguments(fields)})"
}

private fun EnumCase.pattern(bound: (Field) -> Boolean): String =
    ".${swiftIdentifier(name)}" + if (fields.isEmpty()) "" else fields.joinToString(", ", "(", ")") { if (bound(it)) "let ${swiftIdentifier(it.swiftName)}" else "_" }

private fun SwiftEnum.sharedFields(): List<Field> {
    val caseNames = cases.map { it.name }.toSet()
    return cases.firstOrNull()?.fields.orEmpty().filter { field ->
        field.swiftName !in caseNames && cases.all { case -> case.fields.any { it.swiftName == field.swiftName && it.kind == field.kind } }
    }
}

private fun StringBuilder.appendSharedField(enum: SwiftEnum, field: Field) {
    appendLine()
    appendLine()
    appendLine("    public var ${swiftIdentifier(field.swiftName)}: ${field.type()} {")
    appendLine("        switch self {")
    enum.cases.forEach { appendLine("        case ${it.pattern { f -> f.swiftName == field.swiftName }}: return ${swiftIdentifier(field.swiftName)}") }
    appendLine("        }")
    append("    }")
}

private fun renderEnum(enum: SwiftEnum): String = buildString {
    appendLine("public ${if (enum.indirect) "indirect " else ""}enum ${enum.name}${conformance(enum.equatable, enum.hashable)} {")
    enum.cases.forEach { appendLine("    case ${swiftIdentifier(it.name)}${it.payload { field -> field.type() }}") }
    appendLine()
    appendLine("    public init(_ kotlin: ${enum.kotlinSwiftName}) {")
    enum.cases.forEach { appendLine(it.branch()) }
    appendLine("        fatalError(\"Unknown subclass of ${enum.name}\")")
    append("    }")
    enum.sharedFields().forEach { appendSharedField(enum, it) }
    if (enum.hashable && enum.routeSwiftName != null) {
        appendLine()
        appendLine()
        appendLine("    public init?(route: ${enum.routeSwiftName}) {")
        appendLine("        guard let kotlin = route as? ${enum.kotlinSwiftName} else { return nil }")
        appendLine("        self.init(kotlin)")
        append("    }")
    }
    if (enum.writable) {
        appendLine()
        appendLine()
        appendLine("    public var kotlinValue: ${enum.kotlinSwiftName} {")
        appendLine("        switch self {")
        enum.cases.forEach { appendLine("        case ${it.pattern { field -> field.constructorLabel != null }}: return ${it.construction()}") }
        appendLine("        }")
        append("    }")
    }
    appendLine()
    append("}")
}

private fun FlowValue.type(): String = when (this) {
    is FlowValue.Generated -> typeName
    is FlowValue.GeneratedList -> "[$typeName]"
    is FlowValue.Boxed -> swiftType
    FlowValue.Localized -> "Swift.String"
    is FlowValue.Bridged -> swiftType
}

/** The Swift value of the Kotlin object [kotlin]; a bridged one is cast through `AnyObject`, as Swift bridges it at run time. */
private fun FlowValue.read(kotlin: String): String = when (this) {
    is FlowValue.Generated -> "$typeName($kotlin)"
    is FlowValue.GeneratedList -> "($kotlin as! [$kotlinSwiftName]).map { $typeName(\$0) }"
    is FlowValue.Boxed -> "$kotlin.$accessor"
    FlowValue.Localized -> "$kotlin.localized()"
    is FlowValue.Bridged -> "($kotlin as AnyObject) as! $swiftType"
}

private fun FlowAccessor.valueType(): String = if (valueNullable) "${value.type()}?" else value.type()

private fun FlowAccessor.mapper(): String =
    if (valueNullable) "{ kotlin in kotlin.map { kotlin in ${value.read("kotlin")} } }" else "{ kotlin in ${value.read("kotlin!")} }"

private fun FlowAccessor.keyName(): String =
    "LockstepKey_" + (ownerSwiftName + "_" + propertyName).replace(Regex("[^A-Za-z0-9]"), "_")

private fun renderAccessor(accessor: FlowAccessor): String = when (accessor.flowKind) {
    FlowKind.State -> """
        private let ${accessor.keyName()}: StaticString = "${accessor.keyName()}"

        @available(iOS 17.0, *)
        extension ${accessor.ownerSwiftName} {
            public var ${swiftIdentifier(accessor.propertyName)}: UiState<${accessor.valueType()}> {
                if let cached = objc_getAssociatedObject(self, ${accessor.keyName()}.utf8Start) as? UiState<${accessor.valueType()}> {
                    return cached
                }
                let created = UiState(${accessor.originalName}, ${accessor.mapper()})
                objc_setAssociatedObject(self, ${accessor.keyName()}.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
                return created
            }
        }
    """.trimIndent()

    FlowKind.Events -> """
        @available(iOS 17.0, *)
        extension ${accessor.ownerSwiftName} {
            public var ${swiftIdentifier(accessor.propertyName)}: UiEvents<${accessor.valueType()}> {
                UiEvents(${accessor.originalName}, ${accessor.mapper()})
            }
        }
    """.trimIndent()
}

private val swiftKeywords = setOf(
    "Any", "Self", "as", "associatedtype", "break", "case", "catch", "class", "continue", "default", "defer",
    "deinit", "do", "else", "enum", "extension", "fallthrough", "false", "fileprivate", "for", "func", "guard",
    "if", "import", "in", "init", "inout", "internal", "is", "let", "nil", "open", "operator", "private",
    "precedencegroup", "protocol", "public", "repeat", "rethrows", "return", "self", "static", "struct",
    "subscript", "super", "switch", "throw", "throws", "true", "try", "typealias", "var", "where", "while",
)

internal fun swiftIdentifier(name: String): String = if (name in swiftKeywords) "`$name`" else name
