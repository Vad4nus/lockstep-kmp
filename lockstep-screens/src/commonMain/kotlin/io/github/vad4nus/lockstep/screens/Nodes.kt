package io.github.vad4nus.lockstep.screens

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Spacing token; each platform theme turns it into dp or points. */
public enum class Space { Xs, S, M, L, Xl }

/** Color token; each platform theme resolves it, including dark mode. */
public enum class ColorToken { Background, Surface, Primary, Secondary, Error }

/** Text style token; each platform theme resolves it, including Dynamic Type on iOS. */
public enum class TextStyleToken { Title, Headline, Body, Caption }

/** The emphasis of a button: the main action of a screen, a secondary one, or a text link. */
public enum class ButtonVariant { Primary, Secondary, Text }

/** What an input takes: it picks the keyboard, hides a password and tells autofill what the field is. */
public enum class InputKind { Text, Email, Password, Number }

/** Where the children of a column sit horizontally. */
public enum class HAlign { Start, Center, End }

/** Where the children of a row sit vertically. */
public enum class VAlign { Top, Center, Bottom }

/** The direction a lazy list scrolls in. */
public enum class ListAxis { Vertical, Horizontal }

/** Decoration shared by every node. Padding is drawn inside the background on both platforms. */
@Serializable
public data class Style(
    val padding: Space? = null,
    val background: ColorToken? = null,
    val corner: Space? = null,
)

/**
 * What assistive technologies get: a [label] read in place of the content, a [heading] mark, or [hidden] to skip
 * the node.
 */
@Serializable
public data class A11y(
    val label: String? = null,
    val heading: Boolean = false,
    val hidden: Boolean = false,
)

/**
 * A screen described in KMP: a tree of [Node]s in the format version [schema].
 *
 * A screen from [decode] or from the [screen] builder is checked and safe to draw. One built with the constructors
 * is not, until it goes through [checked].
 */
@Serializable
public data class ScreenSpec(
    val id: String,
    val root: @Serializable(with = LenientNodeSerializer::class) Node,
    val schema: Int = SCHEMA,
) {
    public companion object {
        /** The newest format version this library reads. */
        public const val SCHEMA: Int = 1

        /**
         * Decodes a screen and checks it like [checked] does.
         *
         * A part this version cannot read costs only that part: an unknown node type or a node with broken content
         * becomes [Node.Unknown], an unknown event or rule type becomes [NodeEvent.Unknown] or [InputRule.Unknown], and
         * an unknown token falls back to the default. Everything but unknown node types and tokens is reported to
         * [onViolation]. The default handler throws, which suits development; pass a logging one in release builds
         * to draw what can be drawn.
         *
         * Throws [SerializationException] when [json] is not a screen at all or is nested deeper than
         * 128 JSON levels, about 60 containers inside each other.
         */
        public fun decode(json: String, onViolation: (LayoutViolation) -> Unit = throwOnViolation): ScreenSpec {
            if (json.isNestedDeeperThan(MAX_NESTING)) {
                throw SerializationException("lockstep: the screen is nested deeper than $MAX_NESTING JSON levels")
            }
            return ScreenJson.decodeFromString(serializer(), json).checked(onViolation)
        }
    }
}

internal const val MAX_NESTING = 128

/** Counts brackets outside string literals: the JSON parser recurses on every level, so the depth is checked before it runs. */
private fun String.isNestedDeeperThan(limit: Int): Boolean {
    var depth = 0
    var quoted = false
    var index = 0
    while (index < length) {
        val char = this[index]
        when {
            quoted && char == '\\' -> index++
            char == '"' -> quoted = !quoted
            quoted -> Unit
            char == '{' || char == '[' -> if (++depth > limit) return true
            char == '}' || char == ']' -> depth--
        }
        index++
    }
    return false
}

/**
 * A node of a KMP-described screen. Sealed, so a renderer without a case for a new node does not compile;
 * a node type this version does not know decodes to [Unknown].
 */
@Serializable
public sealed interface Node {
    /** Unique among the siblings: both platforms key list items by it. Nodes in different branches may share an id. */
    public val id: String
    /** Padding, background and corners. */
    public val style: Style?

    /** What assistive technologies read. */
    public val a11y: A11y?

    /** Children one under another. With an [event] the whole column is tappable. */
    @Serializable
    @SerialName("column")
    public data class Column(
        override val id: String,
        val children: List<@Serializable(with = LenientNodeSerializer::class) Node>,
        val spacing: Space? = null,
        val align: HAlign = HAlign.Start,
        val scroll: Boolean = false,
        val event: NodeEvent? = null,
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** Children side by side. With an [event] the whole row is tappable. */
    @Serializable
    @SerialName("row")
    public data class Row(
        override val id: String,
        val children: List<@Serializable(with = LenientNodeSerializer::class) Node>,
        val spacing: Space? = null,
        val align: VAlign = VAlign.Center,
        val scroll: Boolean = false,
        val event: NodeEvent? = null,
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** Children drawn on top of each other, aligned to the top start corner. With an [event] the whole box is tappable. */
    @Serializable
    @SerialName("box")
    public data class Box(
        override val id: String,
        val children: List<@Serializable(with = LenientNodeSerializer::class) Node>,
        val event: NodeEvent? = null,
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** A lazy list keyed by item id. [size] fixes its length along [axis], which lets it sit inside a scroll on that axis. */
    @Serializable
    @SerialName("list")
    public data class LazyList(
        override val id: String,
        val items: List<@Serializable(with = LenientNodeSerializer::class) Node>,
        val axis: ListAxis = ListAxis.Vertical,
        val spacing: Space? = null,
        val size: Int? = null,
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** A fixed gap of [size], or, when [flexible], the remaining space of a column or row shared with other flexible spacers. */
    @Serializable
    @SerialName("spacer")
    public data class Spacer(
        override val id: String,
        val size: Space? = null,
        val flexible: Boolean = false,
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** A text in [textStyle], in [color] or in the default text color. */
    @Serializable
    @SerialName("text")
    public data class Text(
        override val id: String,
        val text: String,
        val textStyle: TextStyleToken = TextStyleToken.Body,
        val color: ColorToken? = null,
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** An image the platform theme registers under [name], [size] dp or points square. */
    @Serializable
    @SerialName("image")
    public data class Image(
        override val id: String,
        val name: String,
        val size: Int = 24,
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** A button that sends [event] when tapped. */
    @Serializable
    @SerialName("button")
    public data class Button(
        override val id: String,
        val label: String,
        val event: NodeEvent,
        val variant: ButtonVariant = ButtonVariant.Primary,
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** A text field bound to [field] in [FormState]; [rules] run on [NodeEvent.Submit]. */
    @Serializable
    @SerialName("input")
    public data class Input(
        override val id: String,
        val field: String,
        val label: String,
        val kind: InputKind = InputKind.Text,
        val rules: List<InputRule> = emptyList(),
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** A switch bound to [field] in [FormState], stored as "true" or "false". */
    @Serializable
    @SerialName("toggle")
    public data class Toggle(
        override val id: String,
        val field: String,
        val label: String,
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /** A native piece the platform theme registers under [key]. */
    @Serializable
    @SerialName("custom")
    public data class Custom(
        override val id: String,
        val key: String,
        val props: Map<String, String> = emptyMap(),
        override val style: Style? = null,
        override val a11y: A11y? = null,
    ) : Node

    /**
     * A node this version cannot draw: its type is not known, or its content could not be read and [error] says why.
     * Renders [fallback] or nothing.
     */
    @Serializable
    @SerialName("unknown")
    public data class Unknown(
        override val id: String = "",
        val fallback: @Serializable(with = LenientNodeSerializer::class) Node? = null,
        override val style: Style? = null,
        override val a11y: A11y? = null,
        val error: String? = null,
    ) : Node
}

/** Decodes a node; one that cannot be read becomes [Node.Unknown] with the reason, so it does not cost the whole screen. */
internal object LenientNodeSerializer : KSerializer<Node> {
    override val descriptor: SerialDescriptor get() = Node.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Node): Unit = encoder.encodeSerializableValue(Node.serializer(), value)

    override fun deserialize(decoder: Decoder): Node {
        if (decoder !is JsonDecoder) return decoder.decodeSerializableValue(Node.serializer())
        val element = decoder.decodeJsonElement()
        return try {
            decoder.json.decodeFromJsonElement(Node.serializer(), element)
        } catch (failure: IllegalArgumentException) {
            val fields = element as? JsonObject
            Node.Unknown(
                id = (fields?.get("id") as? JsonPrimitive)?.contentOrNull.orEmpty(),
                fallback = fields?.get("fallback")?.let { decoder.json.decodeFromJsonElement(this, it) },
                error = failure.message?.lineSequence()?.firstOrNull() ?: "unreadable content",
            )
        }
    }
}

/** What a node sends to the screen's single event sink. */
@Serializable
public sealed interface NodeEvent {
    /** A tap the screen tells apart by [id]. */
    @Serializable
    @SerialName("click")
    public data class Click(val id: String) : NodeEvent

    /** Validates the form; see [FormState.reduce]. */
    @Serializable
    @SerialName("submit")
    public data object Submit : NodeEvent

    /** A request to open [destination], which the screen turns into a route. */
    @Serializable
    @SerialName("navigate")
    public data class Navigate(val destination: String) : NodeEvent

    /** An event of the app's own, told apart by [key]. */
    @Serializable
    @SerialName("custom")
    public data class Custom(val key: String, val payload: Map<String, String> = emptyMap()) : NodeEvent

    /** Sent by inputs and toggles, never declared in a tree. */
    @Serializable
    @SerialName("changed")
    public data class ValueChanged(val field: String, val value: String) : NodeEvent

    /** An event type this version does not know. The node that carries it does nothing when tapped. */
    @Serializable
    @SerialName("unknown")
    public data object Unknown : NodeEvent
}

/** A check an [Node.Input] runs on [NodeEvent.Submit]; [message] is shown under the field when it fails. */
@Serializable
public sealed interface InputRule {
    /** The text shown under the field when the rule fails. */
    public val message: String

    /** The value must not be blank. */
    @Serializable
    @SerialName("required")
    public data class Required(override val message: String) : InputRule

    /** The value must be at least [length] characters long. */
    @Serializable
    @SerialName("minLength")
    public data class MinLength(val length: Int, override val message: String) : InputRule

    /**
     * The whole value must match [regex]. Each platform compiles it with its own engine, `java.util.regex` on Android
     * and the Kotlin/Native one on iOS, so keep to what both read the same way: character classes, groups,
     * alternation and quantifiers. A pattern that does not compile is reported by the screen check and accepts
     * every value.
     */
    @Serializable
    @SerialName("pattern")
    public data class Pattern(val regex: String, override val message: String) : InputRule

    /** A rule type this version does not know; it accepts every value. */
    @Serializable
    @SerialName("unknown")
    public data class Unknown(override val message: String = "") : InputRule
}

/** Values and errors of every input and toggle on a screen, keyed by field. The tree itself stays unchanged while the user types. */
public data class FormState(
    val values: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
) {
    /** Names the fields without their values, so that a password does not end up in a log. */
    override fun toString(): String = "FormState(fields=${values.keys}, errors=$errors)"
}

/**
 * JSON for [ScreenSpec]: the `type` discriminator, unknown keys ignored, unknown tokens and explicit nulls replaced
 * by the defaults, unknown node, event and rule types decoded to their `Unknown` cases.
 */
@OptIn(ExperimentalSerializationApi::class)
public val ScreenJson: Json = Json {
    ignoreUnknownKeys = true
    decodeEnumsCaseInsensitive = true
    coerceInputValues = true
    serializersModule = SerializersModule {
        polymorphic(Node::class) { defaultDeserializer { Node.Unknown.serializer() } }
        polymorphic(NodeEvent::class) { defaultDeserializer { NodeEvent.Unknown.serializer() } }
        polymorphic(InputRule::class) { defaultDeserializer { InputRule.Unknown.serializer() } }
    }
}
