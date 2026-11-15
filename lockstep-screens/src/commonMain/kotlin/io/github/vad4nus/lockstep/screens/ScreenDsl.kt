package io.github.vad4nus.lockstep.screens

import kotlin.jvm.JvmName
import kotlin.native.HiddenFromObjC

/** Marks the scopes of the screen builder, so that a node is never added to an outer scope by accident. */
@DslMarker
public annotation class ScreenDsl

/** Scroll context of an axis: no ancestor scrolls along it. */
@HiddenFromObjC
public sealed interface Bounded

/** Scroll context of an axis: an ancestor scrolls along it, so the length on that axis is unbounded. */
@HiddenFromObjC
public sealed interface Scrolled

/**
 * Builds nodes. [V] and [H] record whether an ancestor scrolls vertically or horizontally, so a layout that crashes
 * or collapses on a platform does not compile: the offending call resolves to an overload deprecated with level ERROR.
 */
@ScreenDsl
@HiddenFromObjC
public open class NodeScope<V, H> internal constructor() {
    internal val nodes: MutableList<Node> = mutableListOf()
}

/** The scope inside a column, where a flexible spacer takes a share of the free height. */
@HiddenFromObjC
public class NodeColumnScope<V, H> internal constructor() : NodeScope<V, H>()

/** The scope inside a row, where a flexible spacer takes a share of the free width. */
@HiddenFromObjC
public class NodeRowScope<V, H> internal constructor() : NodeScope<V, H>()

/** Builds a screen with exactly one root node and passes it through [checked]; the default [onViolation] throws. */
@HiddenFromObjC
public fun screen(id: String, onViolation: (LayoutViolation) -> Unit = throwOnViolation, block: NodeScope<Bounded, Bounded>.() -> Unit): ScreenSpec {
    val root = requireNotNull(NodeScope<Bounded, Bounded>().apply(block).nodes.singleOrNull()) { "lockstep: screen $id needs exactly one root node" }
    return ScreenSpec(id, root).checked(onViolation)
}

/** Adds a [Node.Column]: children one under another. With an [event] the whole column is tappable. */
@HiddenFromObjC
public fun <V, H> NodeScope<V, H>.column(
    id: String,
    spacing: Space? = null,
    align: HAlign = HAlign.Start,
    event: NodeEvent? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeColumnScope<V, H>.() -> Unit,
) {
    nodes += Node.Column(id, NodeColumnScope<V, H>().apply(block).nodes, spacing, align, false, event, style, a11y)
}

/** Adds a [Node.Row]: children side by side. With an [event] the whole row is tappable. */
@HiddenFromObjC
public fun <V, H> NodeScope<V, H>.row(
    id: String,
    spacing: Space? = null,
    align: VAlign = VAlign.Center,
    event: NodeEvent? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeRowScope<V, H>.() -> Unit,
) {
    nodes += Node.Row(id, NodeRowScope<V, H>().apply(block).nodes, spacing, align, false, event, style, a11y)
}

/** Adds a [Node.Box]: children drawn on top of each other. With an [event] the whole box is tappable. */
@HiddenFromObjC
public fun <V, H> NodeScope<V, H>.box(
    id: String,
    event: NodeEvent? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeScope<V, H>.() -> Unit,
) {
    nodes += Node.Box(id, NodeScope<V, H>().apply(block).nodes, event, style, a11y)
}

/** Adds a column that scrolls vertically. */
@HiddenFromObjC
public fun <H> NodeScope<Bounded, H>.scrollColumn(
    id: String,
    spacing: Space? = null,
    align: HAlign = HAlign.Start,
    event: NodeEvent? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeColumnScope<Scrolled, H>.() -> Unit,
) {
    nodes += Node.Column(id, NodeColumnScope<Scrolled, H>().apply(block).nodes, spacing, align, true, event, style, a11y)
}

/** Does not compile: the deprecation message names the platform failure and the quick-fix offers the safe form. */
@HiddenFromObjC
@JvmName("scrollColumnInVerticalScroll")
@Deprecated(NESTED_VERTICAL_SCROLL, ReplaceWith("column(id, spacing, align, event, style, a11y, block)"), DeprecationLevel.ERROR)
public fun <H> NodeScope<Scrolled, H>.scrollColumn(
    id: String,
    spacing: Space? = null,
    align: HAlign = HAlign.Start,
    event: NodeEvent? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeColumnScope<Scrolled, H>.() -> Unit,
): Unit = error(NESTED_VERTICAL_SCROLL)

/** Adds a row that scrolls horizontally. */
@HiddenFromObjC
public fun <V> NodeScope<V, Bounded>.scrollRow(
    id: String,
    spacing: Space? = null,
    align: VAlign = VAlign.Center,
    event: NodeEvent? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeRowScope<V, Scrolled>.() -> Unit,
) {
    nodes += Node.Row(id, NodeRowScope<V, Scrolled>().apply(block).nodes, spacing, align, true, event, style, a11y)
}

/** Does not compile: the deprecation message names the platform failure and the quick-fix offers the safe form. */
@HiddenFromObjC
@JvmName("scrollRowInHorizontalScroll")
@Deprecated(NESTED_HORIZONTAL_SCROLL, ReplaceWith("row(id, spacing, align, event, style, a11y, block)"), DeprecationLevel.ERROR)
public fun <V> NodeScope<V, Scrolled>.scrollRow(
    id: String,
    spacing: Space? = null,
    align: VAlign = VAlign.Center,
    event: NodeEvent? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeRowScope<V, Scrolled>.() -> Unit,
): Unit = error(NESTED_HORIZONTAL_SCROLL)

/** Adds a vertical lazy list that takes the height its parent gives it. */
@HiddenFromObjC
public fun <H> NodeScope<Bounded, H>.lazyColumn(
    id: String,
    spacing: Space? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeScope<Scrolled, H>.() -> Unit,
) {
    nodes += Node.LazyList(id, NodeScope<Scrolled, H>().apply(block).nodes, ListAxis.Vertical, spacing, style = style, a11y = a11y)
}

/** Does not compile: the deprecation message names the platform failure and the quick-fix offers the safe form. */
@HiddenFromObjC
@JvmName("lazyColumnInVerticalScroll")
@Deprecated(NESTED_VERTICAL_SCROLL, ReplaceWith("lazyColumn(id, height = 300, spacing = spacing, style = style, a11y = a11y, block = block)"), DeprecationLevel.ERROR)
public fun <H> NodeScope<Scrolled, H>.lazyColumn(
    id: String,
    spacing: Space? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeScope<Scrolled, H>.() -> Unit,
): Unit = error(NESTED_VERTICAL_SCROLL)

/** A vertical lazy list with a fixed [height]; allowed inside a vertical scroll. */
@HiddenFromObjC
public fun <V, H> NodeScope<V, H>.lazyColumn(
    id: String,
    height: Int,
    spacing: Space? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeScope<Scrolled, H>.() -> Unit,
) {
    nodes += Node.LazyList(id, NodeScope<Scrolled, H>().apply(block).nodes, ListAxis.Vertical, spacing, height, style, a11y)
}

/** Adds a horizontal lazy list that takes the width its parent gives it. */
@HiddenFromObjC
public fun <V> NodeScope<V, Bounded>.lazyRow(
    id: String,
    spacing: Space? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeScope<V, Scrolled>.() -> Unit,
) {
    nodes += Node.LazyList(id, NodeScope<V, Scrolled>().apply(block).nodes, ListAxis.Horizontal, spacing, style = style, a11y = a11y)
}

/** Does not compile: the deprecation message names the platform failure and the quick-fix offers the safe form. */
@HiddenFromObjC
@JvmName("lazyRowInHorizontalScroll")
@Deprecated(NESTED_HORIZONTAL_SCROLL, ReplaceWith("lazyRow(id, width = 300, spacing = spacing, style = style, a11y = a11y, block = block)"), DeprecationLevel.ERROR)
public fun <V> NodeScope<V, Scrolled>.lazyRow(
    id: String,
    spacing: Space? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeScope<V, Scrolled>.() -> Unit,
): Unit = error(NESTED_HORIZONTAL_SCROLL)

/** A horizontal lazy list with a fixed [width]; allowed inside a horizontal scroll. */
@HiddenFromObjC
public fun <V, H> NodeScope<V, H>.lazyRow(
    id: String,
    width: Int,
    spacing: Space? = null,
    style: Style? = null,
    a11y: A11y? = null,
    block: NodeScope<V, Scrolled>.() -> Unit,
) {
    nodes += Node.LazyList(id, NodeScope<V, Scrolled>().apply(block).nodes, ListAxis.Horizontal, spacing, width, style, a11y)
}

/** Adds a spacer that takes a share of the free height of the column. */
@HiddenFromObjC
public fun <H> NodeColumnScope<Bounded, H>.flexibleSpacer(id: String) {
    nodes += Node.Spacer(id, flexible = true)
}

/** Adds a spacer that takes a share of the free width of the row. */
@HiddenFromObjC
public fun <V> NodeRowScope<V, Bounded>.flexibleSpacer(id: String) {
    nodes += Node.Spacer(id, flexible = true)
}

/** Does not compile: the deprecation message names the platform failure and the quick-fix offers the safe form. */
@HiddenFromObjC
@Deprecated(FLEXIBLE_SPACER_IN_SCROLL, ReplaceWith("spacer(id, Space.M)"), DeprecationLevel.ERROR)
public fun NodeScope<*, *>.flexibleSpacer(id: String): Unit = error(FLEXIBLE_SPACER_IN_SCROLL)

/** Adds a fixed gap of [size]. */
@HiddenFromObjC
public fun NodeScope<*, *>.spacer(id: String, size: Space) {
    nodes += Node.Spacer(id, size)
}

/** Adds a [Node.Text]. */
@HiddenFromObjC
public fun NodeScope<*, *>.text(
    id: String,
    text: String,
    textStyle: TextStyleToken = TextStyleToken.Body,
    color: ColorToken? = null,
    style: Style? = null,
    a11y: A11y? = null,
) {
    nodes += Node.Text(id, text, textStyle, color, style, a11y)
}

/** Adds a [Node.Image]: the image the platform theme registers under [name]. */
@HiddenFromObjC
public fun NodeScope<*, *>.image(id: String, name: String, size: Int = 24, style: Style? = null, a11y: A11y? = null) {
    nodes += Node.Image(id, name, size, style, a11y)
}

/** Adds a [Node.Button] that sends [event] when tapped. */
@HiddenFromObjC
public fun NodeScope<*, *>.button(
    id: String,
    label: String,
    event: NodeEvent,
    variant: ButtonVariant = ButtonVariant.Primary,
    style: Style? = null,
    a11y: A11y? = null,
) {
    nodes += Node.Button(id, label, event, variant, style, a11y)
}

/** Adds a [Node.Input] bound to [field]; [rules] run on [NodeEvent.Submit]. */
@HiddenFromObjC
public fun NodeScope<*, *>.input(
    id: String,
    field: String,
    label: String,
    kind: InputKind = InputKind.Text,
    rules: List<InputRule> = emptyList(),
    style: Style? = null,
    a11y: A11y? = null,
) {
    nodes += Node.Input(id, field, label, kind, rules, style, a11y)
}

/** Adds a [Node.Toggle] bound to [field]. */
@HiddenFromObjC
public fun NodeScope<*, *>.toggle(id: String, field: String, label: String, style: Style? = null, a11y: A11y? = null) {
    nodes += Node.Toggle(id, field, label, style, a11y)
}

/** Adds a [Node.Custom]: the native piece the platform theme registers under [key]. */
@HiddenFromObjC
public fun NodeScope<*, *>.custom(id: String, key: String, props: Map<String, String> = emptyMap(), style: Style? = null, a11y: A11y? = null) {
    nodes += Node.Custom(id, key, props, style, a11y)
}
