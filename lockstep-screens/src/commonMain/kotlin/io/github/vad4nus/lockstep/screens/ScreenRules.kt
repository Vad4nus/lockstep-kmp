package io.github.vad4nus.lockstep.screens

import kotlin.native.HiddenFromObjC

internal const val NESTED_VERTICAL_SCROLL =
    "A vertical lazy list or scroll inside another vertical scroll crashes Compose (infinite height constraints) and gets no height of its own in SwiftUI. Give the inner list a fixed height or make it horizontal."

internal const val NESTED_HORIZONTAL_SCROLL =
    "A horizontal lazy list or scroll inside another horizontal scroll crashes Compose (infinite width constraints) and gets no width of its own in SwiftUI. Give the inner list a fixed width or make it vertical."

internal const val FLEXIBLE_SPACER_IN_SCROLL =
    "A flexible spacer only works directly inside a column or row that does not scroll along its axis; inside a scroll or a lazy list it collapses to zero on both platforms. Use a fixed spacer."

internal const val DUPLICATE_ID =
    "Sibling nodes share an id: Compose lazy lists crash on duplicate keys and SwiftUI ForEach gives undefined results."

internal const val DUPLICATE_FIELD =
    "Another input or toggle is bound to the same field, so they overwrite each other's value."

internal const val UNREADABLE_NODE = "The node could not be read and is not drawn: "

internal const val UNKNOWN_EVENT = "The event type is not known to this version, so the node does nothing when tapped."

internal const val UNKNOWN_RULE = "A rule type is not known to this version, so it accepts every value."

internal const val INVALID_PATTERN = "A pattern rule does not compile, so it accepts every value."

internal const val NEWER_SCHEMA = "The screen is in a newer schema than this version reads, so parts of it may be missing."

/** A problem in a screen: a platform layout rule a node breaks, or a part this version cannot use. */
public data class LayoutViolation(val nodeId: String, val message: String)

internal val throwOnViolation: (LayoutViolation) -> Unit = { throw IllegalArgumentException("lockstep: node ${it.nodeId}: ${it.message}") }

/**
 * Reports every problem of the screen to [onViolation] and returns the screen with the layout problems repaired,
 * so that it draws on both platforms: a scroll nested along its own axis stops scrolling, a lazy list nested that
 * way becomes a plain column or row, a flexible spacer inside a scroll becomes a fixed one, and repeated sibling
 * ids are made unique.
 *
 * [ScreenSpec.decode] and the [screen] builder call it. The default [onViolation] throws.
 */
public fun ScreenSpec.checked(onViolation: (LayoutViolation) -> Unit = throwOnViolation): ScreenSpec {
    if (schema > ScreenSpec.SCHEMA) onViolation(LayoutViolation(root.id, NEWER_SCHEMA))
    return copy(root = TreeCheck(onViolation).repair(root, parent = null, vertical = false, horizontal = false))
}

/** The problems [checked] reports for this tree, apart from the schema version. */
public fun Node.violations(): List<LayoutViolation> =
    buildList { TreeCheck { add(it) }.repair(this@violations, parent = null, vertical = false, horizontal = false) }

private class TreeCheck(private val report: (LayoutViolation) -> Unit) {
    private val fields = mutableSetOf<String>()

    fun repair(node: Node, parent: Node?, vertical: Boolean, horizontal: Boolean): Node {
        val scrollsVertically = node is Node.Column && node.scroll || node is Node.LazyList && node.axis == ListAxis.Vertical
        val scrollsHorizontally = node is Node.Row && node.scroll || node is Node.LazyList && node.axis == ListAxis.Horizontal
        val sized = node is Node.LazyList && node.size != null
        val nested = !sized && (scrollsVertically && vertical || scrollsHorizontally && horizontal)
        if (nested) report(LayoutViolation(node.id, if (scrollsVertically) NESTED_VERTICAL_SCROLL else NESTED_HORIZONTAL_SCROLL))
        val collapsed = node is Node.Spacer && node.flexible && when (parent) {
            is Node.Column -> vertical
            is Node.Row -> horizontal
            else -> true
        }
        if (collapsed) report(LayoutViolation(node.id, FLEXIBLE_SPACER_IN_SCROLL))
        checkContent(node)
        val children = node.nested()
        children.groupBy { it.id }
            .filter { (id, group) -> group.size > 1 && !(id.isEmpty() && group.all { it is Node.Unknown }) }
            .keys.forEach { report(LayoutViolation(it, DUPLICATE_ID)) }
        val repaired = children.withUniqueIds().map {
            repair(it, if (node is Node.Unknown) parent else node, vertical || scrollsVertically, horizontal || scrollsHorizontally)
        }
        return when (node) {
            is Node.Column -> node.copy(children = repaired, scroll = node.scroll && !nested)
            is Node.Row -> node.copy(children = repaired, scroll = node.scroll && !nested)
            is Node.Box -> node.copy(children = repaired)
            is Node.LazyList -> when {
                !nested -> node.copy(items = repaired)
                scrollsVertically -> Node.Column(node.id, repaired, node.spacing, style = node.style, a11y = node.a11y)
                else -> Node.Row(node.id, repaired, node.spacing, style = node.style, a11y = node.a11y)
            }
            is Node.Unknown -> node.copy(fallback = repaired.firstOrNull())
            is Node.Spacer -> node.copy(flexible = node.flexible && !collapsed)
            is Node.Text, is Node.Image, is Node.Button, is Node.Input, is Node.Toggle, is Node.Custom -> node
        }
    }

    private fun checkContent(node: Node) {
        if (node is Node.Unknown && node.error != null) report(LayoutViolation(node.id, UNREADABLE_NODE + node.error))
        if (node.event() == NodeEvent.Unknown) report(LayoutViolation(node.id, UNKNOWN_EVENT))
        if (node is Node.Input) {
            if (node.rules.any { it is InputRule.Unknown }) report(LayoutViolation(node.id, UNKNOWN_RULE))
            if (node.rules.any { it is InputRule.Pattern && it.compiled() == null }) report(LayoutViolation(node.id, INVALID_PATTERN))
        }
        val field = (node as? Node.Input)?.field ?: (node as? Node.Toggle)?.field
        if (field != null && !fields.add(field)) report(LayoutViolation(node.id, DUPLICATE_FIELD))
    }
}

private fun Node.event(): NodeEvent? = when (this) {
    is Node.Button -> event
    is Node.Column -> event
    is Node.Row -> event
    is Node.Box -> event
    is Node.LazyList, is Node.Spacer, is Node.Text, is Node.Image, is Node.Input, is Node.Toggle, is Node.Custom, is Node.Unknown -> null
}

private fun List<Node>.withUniqueIds(): List<Node> {
    val taken = mapTo(mutableSetOf()) { it.id }
    val seen = mutableSetOf<String>()
    return map { node ->
        if (seen.add(node.id)) node else node.withId(generateSequence(2) { it + 1 }.map { "${node.id}#$it" }.first(taken::add))
    }
}

private fun Node.withId(id: String): Node = when (this) {
    is Node.Column -> copy(id = id)
    is Node.Row -> copy(id = id)
    is Node.Box -> copy(id = id)
    is Node.LazyList -> copy(id = id)
    is Node.Spacer -> copy(id = id)
    is Node.Text -> copy(id = id)
    is Node.Image -> copy(id = id)
    is Node.Button -> copy(id = id)
    is Node.Input -> copy(id = id)
    is Node.Toggle -> copy(id = id)
    is Node.Custom -> copy(id = id)
    is Node.Unknown -> copy(id = id)
}

/** Applies [NodeEvent.ValueChanged] and validates on [NodeEvent.Submit]; other events leave the form as it is. */
@HiddenFromObjC
public fun FormState.reduce(event: NodeEvent, root: Node): FormState = when (event) {
    is NodeEvent.ValueChanged -> copy(values = values + (event.field to event.value), errors = errors - event.field)
    NodeEvent.Submit -> validate(root)
    is NodeEvent.Click, is NodeEvent.Navigate, is NodeEvent.Custom, NodeEvent.Unknown -> this
}

private fun FormState.validate(root: Node): FormState =
    copy(errors = inputs(root).mapNotNull { input -> input.rules.firstOrNull { !it.accepts(values[input.field].orEmpty()) }?.let { input.field to it.message } }.toMap())

private fun InputRule.accepts(value: String): Boolean = when (this) {
    is InputRule.Required -> value.isNotBlank()
    is InputRule.MinLength -> value.length >= length
    is InputRule.Pattern -> compiled()?.matches(value) ?: true
    is InputRule.Unknown -> true
}

private fun InputRule.Pattern.compiled(): Regex? = try {
    Regex(regex)
} catch (invalid: IllegalArgumentException) {
    null
}

private fun inputs(node: Node): List<Node.Input> = if (node is Node.Input) listOf(node) else node.nested().flatMap(::inputs)

private fun Node.nested(): List<Node> = when (this) {
    is Node.Column -> children
    is Node.Row -> children
    is Node.Box -> children
    is Node.LazyList -> items
    is Node.Unknown -> listOfNotNull(fallback)
    is Node.Spacer, is Node.Text, is Node.Image, is Node.Button, is Node.Input, is Node.Toggle, is Node.Custom -> emptyList()
}
