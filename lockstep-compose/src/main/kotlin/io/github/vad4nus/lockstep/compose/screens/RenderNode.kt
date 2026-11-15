package io.github.vad4nus.lockstep.compose.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vad4nus.lockstep.screens.A11y
import io.github.vad4nus.lockstep.screens.ButtonVariant
import io.github.vad4nus.lockstep.screens.ColorToken
import io.github.vad4nus.lockstep.screens.FormState
import io.github.vad4nus.lockstep.screens.HAlign
import io.github.vad4nus.lockstep.screens.InputKind
import io.github.vad4nus.lockstep.screens.ListAxis
import io.github.vad4nus.lockstep.screens.Node
import io.github.vad4nus.lockstep.screens.NodeEvent
import io.github.vad4nus.lockstep.screens.Space
import io.github.vad4nus.lockstep.screens.TextStyleToken
import io.github.vad4nus.lockstep.screens.VAlign

/** Platform values for the tokens of a KMP-described screen, plus the images and native pieces it names. */
public class NodeTheme(
    public val space: (Space) -> Dp = ::standardSpace,
    public val color: @Composable (ColorToken) -> Color = { standardColor(it) },
    public val textStyle: @Composable (TextStyleToken) -> TextStyle = { standardTextStyle(it) },
    public val images: Map<String, Painter> = emptyMap(),
    public val custom: Map<String, @Composable (Map<String, String>) -> Unit> = emptyMap(),
)

/** The theme [RenderNode] reads. Provide your own to map the tokens to the design system of the app. */
public val LocalNodeTheme: ProvidableCompositionLocal<NodeTheme> = staticCompositionLocalOf { NodeTheme() }

/**
 * Draws [node] with the tokens of [LocalNodeTheme]; inputs and toggles read [form] and report through [onEvent].
 *
 * Draw a checked tree only: one from `ScreenSpec.decode`, the `screen` builder or `checked`. Pass a [form] that is
 * collected without a delay, as `MvvmScreen` does, or fast typing can lose characters. An image or a custom piece
 * the theme does not have keeps its place, draws nothing and is logged.
 */
@Composable
public fun RenderNode(node: Node, form: FormState, onEvent: (NodeEvent) -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalNodeTheme.current
    val fill = node.style?.background?.let { theme.color(it) }
    fun styled(interaction: Modifier? = null): Modifier = modifier.nodeStyle(node, fill, theme, interaction)
    fun tappable(event: NodeEvent?): Modifier? =
        event?.takeIf { it != NodeEvent.Unknown }?.let { Modifier.clickable(role = Role.Button) { onEvent(it) } }
    when (node) {
        is Node.Column -> Column(
            styled(tappable(node.event)).let { if (node.scroll) it.verticalScroll(rememberScrollState()) else it },
            Arrangement.spacedBy(theme.gap(node.spacing)),
            node.align.alignment,
        ) { Children(node.children, form, onEvent, Modifier.weight(1f)) }
        is Node.Row -> Row(
            styled(tappable(node.event)).let { if (node.scroll) it.horizontalScroll(rememberScrollState()) else it },
            Arrangement.spacedBy(theme.gap(node.spacing)),
            node.align.alignment,
        ) { Children(node.children, form, onEvent, Modifier.weight(1f)) }
        is Node.Box -> Box(styled(tappable(node.event))) { Children(node.children, form, onEvent, Modifier) }
        is Node.LazyList -> when (node.axis) {
            ListAxis.Vertical -> LazyColumn(
                node.size?.let { styled().height(it.dp) } ?: styled(),
                verticalArrangement = Arrangement.spacedBy(theme.gap(node.spacing)),
            ) { items(node.items, key = { it.id }) { RenderNode(it, form, onEvent) } }
            ListAxis.Horizontal -> LazyRow(
                node.size?.let { styled().width(it.dp) } ?: styled(),
                horizontalArrangement = Arrangement.spacedBy(theme.gap(node.spacing)),
            ) { items(node.items, key = { it.id }) { RenderNode(it, form, onEvent) } }
        }
        is Node.Spacer -> Spacer(if (node.flexible) styled() else styled().size(theme.gap(node.size)))
        is Node.Text -> Text(
            node.text,
            styled(),
            color = node.color?.let { theme.color(it) } ?: Color.Unspecified,
            style = theme.textStyle(node.textStyle),
        )
        is Node.Image -> {
            val painter = theme.images[node.name]
            if (painter != null) {
                Image(painter, contentDescription = null, modifier = styled().size(node.size.dp))
            } else {
                Missing("image ${node.name}", styled().size(node.size.dp))
            }
        }
        is Node.Button -> {
            val enabled = node.event != NodeEvent.Unknown
            val label: @Composable () -> Unit = { Text(node.label) }
            when (node.variant) {
                ButtonVariant.Primary -> Button({ onEvent(node.event) }, styled(), enabled) { label() }
                ButtonVariant.Secondary -> FilledTonalButton({ onEvent(node.event) }, styled(), enabled) { label() }
                ButtonVariant.Text -> TextButton({ onEvent(node.event) }, styled(), enabled) { label() }
            }
        }
        is Node.Input -> OutlinedTextField(
            value = form.values[node.field].orEmpty(),
            onValueChange = { onEvent(NodeEvent.ValueChanged(node.field, it)) },
            modifier = styled().fillMaxWidth().autofill(node.kind),
            label = { Text(node.label) },
            supportingText = form.errors[node.field]?.let { { Text(it) } },
            isError = node.field in form.errors,
            visualTransformation = if (node.kind == InputKind.Password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = node.kind.keyboardType),
            singleLine = true,
        )
        is Node.Toggle -> {
            val checked = form.values[node.field] == "true"
            val toggle = Modifier.toggleable(checked, role = Role.Switch) { onEvent(NodeEvent.ValueChanged(node.field, it.toString())) }
            Row(styled(toggle).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(node.label)
                Switch(checked, onCheckedChange = null)
            }
        }
        is Node.Custom -> {
            val content = theme.custom[node.key]
            if (content != null) Box(styled()) { content(node.props) } else Missing("custom piece ${node.key}", styled())
        }
        is Node.Unknown -> node.fallback?.let { RenderNode(it, form, onEvent, styled()) }
    }
}

@Composable
private fun Children(children: List<Node>, form: FormState, onEvent: (NodeEvent) -> Unit, flexible: Modifier) {
    children.forEach { child ->
        key(child.id) { RenderNode(child, form, onEvent, if (child is Node.Spacer && child.flexible) flexible else Modifier) }
    }
}

@Composable
private fun Missing(what: String, modifier: Modifier) {
    LaunchedEffect(what) { Log.w("lockstep", "NodeTheme has no $what") }
    Box(modifier)
}

/** [interaction] is a click or a toggle of the whole node: it covers the padding and is clipped to the corners. */
private fun Modifier.nodeStyle(node: Node, fill: Color?, theme: NodeTheme, interaction: Modifier?): Modifier {
    val shape = RoundedCornerShape(theme.gap(node.style?.corner))
    return (interaction?.let { clip(shape).then(it) } ?: this)
        .let { if (fill != null) it.background(fill, shape) else it }
        .padding(theme.gap(node.style?.padding))
        .accessibility(node.a11y)
}

private fun Modifier.accessibility(a11y: A11y?): Modifier = when {
    a11y == null -> this
    a11y.hidden -> clearAndSetSemantics {}
    else -> semantics {
        a11y.label?.let { contentDescription = it }
        if (a11y.heading) heading()
    }
}

private fun NodeTheme.gap(token: Space?): Dp = token?.let(space) ?: 0.dp

private val HAlign.alignment: Alignment.Horizontal
    get() = when (this) {
        HAlign.Start -> Alignment.Start
        HAlign.Center -> Alignment.CenterHorizontally
        HAlign.End -> Alignment.End
    }

private val VAlign.alignment: Alignment.Vertical
    get() = when (this) {
        VAlign.Top -> Alignment.Top
        VAlign.Center -> Alignment.CenterVertically
        VAlign.Bottom -> Alignment.Bottom
    }

private fun Modifier.autofill(kind: InputKind): Modifier = when (kind) {
    InputKind.Email -> semantics { contentType = ContentType.EmailAddress }
    InputKind.Password -> semantics { contentType = ContentType.Password }
    InputKind.Text, InputKind.Number -> this
}

private val InputKind.keyboardType: KeyboardType
    get() = when (this) {
        InputKind.Text -> KeyboardType.Text
        InputKind.Email -> KeyboardType.Email
        InputKind.Password -> KeyboardType.Password
        InputKind.Number -> KeyboardType.Number
    }

private fun standardSpace(token: Space): Dp = when (token) {
    Space.Xs -> 4.dp
    Space.S -> 8.dp
    Space.M -> 16.dp
    Space.L -> 24.dp
    Space.Xl -> 32.dp
}

@Composable
private fun standardColor(token: ColorToken): Color = when (token) {
    ColorToken.Background -> MaterialTheme.colorScheme.background
    ColorToken.Surface -> MaterialTheme.colorScheme.surfaceVariant
    ColorToken.Primary -> MaterialTheme.colorScheme.primary
    ColorToken.Secondary -> MaterialTheme.colorScheme.onSurfaceVariant
    ColorToken.Error -> MaterialTheme.colorScheme.error
}

@Composable
private fun standardTextStyle(token: TextStyleToken): TextStyle = when (token) {
    TextStyleToken.Title -> MaterialTheme.typography.headlineMedium
    TextStyleToken.Headline -> MaterialTheme.typography.titleMedium
    TextStyleToken.Body -> MaterialTheme.typography.bodyLarge
    TextStyleToken.Caption -> MaterialTheme.typography.bodySmall
}
