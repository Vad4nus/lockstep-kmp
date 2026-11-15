package io.github.vad4nus.lockstep.screens

import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal const val LOGIN_JSON = """
{
  "id": "login",
  "root": {
    "type": "column", "id": "root", "spacing": "m", "style": {"padding": "m"},
    "children": [
      {"type": "text", "id": "title", "text": "Sign in", "textStyle": "title", "a11y": {"heading": true}},
      {"type": "input", "id": "email", "field": "email", "label": "Email", "kind": "email",
       "rules": [{"type": "required", "message": "Enter your email"}, {"type": "pattern", "regex": ".+@.+", "message": "Not an email"}]},
      {"type": "toggle", "id": "remember", "field": "remember", "label": "Remember me"},
      {"type": "button", "id": "submit", "label": "Sign in", "event": {"type": "submit"}}
    ]
  }
}
"""

internal val loginTree: Node = Node.Column(
    "root",
    listOf(
        Node.Text("title", "Sign in", TextStyleToken.Title, a11y = A11y(heading = true)),
        Node.Input(
            "email", "email", "Email", InputKind.Email,
            listOf(InputRule.Required("Enter your email"), InputRule.Pattern(".+@.+", "Not an email")),
        ),
        Node.Toggle("remember", "remember", "Remember me"),
        Node.Button("submit", "Sign in", NodeEvent.Submit),
    ),
    spacing = Space.M,
    style = Style(padding = Space.M),
)

class NodesTest {

    private fun tree(root: String): Node = ScreenJson.decodeFromString(ScreenSpec.serializer(), """{"id": "s", "root": $root}""").root

    private fun strict(root: String): ScreenSpec = ScreenSpec.decode("""{"id": "s", "root": $root}""")

    private fun lenient(root: String): Pair<ScreenSpec, List<LayoutViolation>> {
        val reported = mutableListOf<LayoutViolation>()
        return ScreenSpec.decode("""{"id": "s", "root": $root}""") { reported += it } to reported
    }

    private fun column(child: String) =
        """{"type": "column", "id": "root", "children": [{"type": "text", "id": "ok", "text": "fine"}, $child]}"""

    private fun boxes(depth: Int) = buildString {
        repeat(depth) { append("""{"type": "box", "id": "b$it", "children": [""") }
        append("""{"type": "text", "id": "t", "text": "x"}""")
        repeat(depth) { append("]}") }
    }

    @Test
    fun decodesATreeWithTokensEventsAndRules() {
        assertEquals(ScreenSpec("login", loginTree), ScreenSpec.decode(LOGIN_JSON))
    }

    @Test
    fun encodedTreeDecodesBackToTheSameTree() {
        val login = ScreenSpec("login", loginTree)
        val card = ScreenSpec("card", Node.Box("card", listOf(Node.Unknown("u", Node.Text("t", "x"))), NodeEvent.Navigate("details")))
        for (spec in listOf(login, card)) {
            assertEquals(spec, ScreenSpec.decode(ScreenJson.encodeToString(ScreenSpec.serializer(), spec)))
        }
    }

    @Test
    fun containerDecodesItsEvent() {
        val card = tree("""{"type": "column", "id": "card", "event": {"type": "navigate", "destination": "orders/1"}, "children": []}""")
        assertEquals(Node.Column("card", emptyList(), event = NodeEvent.Navigate("orders/1")), card)
    }

    @Test
    fun unknownTokenAndExplicitNullFallBackToTheDefault() {
        assertEquals(Node.Text("t", "x"), tree("""{"type": "text", "id": "t", "text": "x", "textStyle": "display"}"""))
        assertEquals(Node.Text("t", "x"), tree("""{"type": "text", "id": "t", "text": "x", "textStyle": null, "color": "neon"}"""))
        assertEquals(
            Node.Column("c", emptyList()),
            tree("""{"type": "column", "id": "c", "children": [], "spacing": "huge", "align": "justify", "scroll": null}"""),
        )
    }

    @Test
    fun unreadableNodeBecomesUnknownAndCostsOnlyItself() {
        val broken = """{"type": "text", "id": "t", "fallback": {"type": "text", "id": "f", "text": "Update the app"}}"""
        val (spec, reported) = lenient(column(broken))
        val children = assertIs<Node.Column>(spec.root).children

        assertEquals(Node.Text("ok", "fine"), children[0])
        val unknown = assertIs<Node.Unknown>(children[1])
        assertEquals("t", unknown.id)
        assertEquals(Node.Text("f", "Update the app"), unknown.fallback)
        assertEquals(listOf("t"), reported.map { it.nodeId })
        assertTrue(reported.single().message.startsWith(UNREADABLE_NODE))
        assertFailsWith<IllegalArgumentException> { strict(column(broken)) }
    }

    @Test
    fun unknownEventAndRuleTypesAreKeptAndReported() {
        val (spec, reported) = lenient(
            column(
                """{"type": "row", "id": "r", "event": {"type": "swipe"}, "children": [
                  {"type": "button", "id": "b", "label": "Share", "event": {"type": "share", "url": "x"}},
                  {"type": "input", "id": "i", "field": "f", "label": "L", "rules": [{"type": "maxLength", "length": 5, "message": "m"}]}
                ]}""",
            ),
        )
        val row = assertIs<Node.Row>(assertIs<Node.Column>(spec.root).children[1])

        assertEquals(NodeEvent.Unknown, row.event)
        assertEquals(NodeEvent.Unknown, assertIs<Node.Button>(row.children[0]).event)
        assertEquals(listOf<InputRule>(InputRule.Unknown("m")), assertIs<Node.Input>(row.children[1]).rules)
        assertEquals(
            listOf(LayoutViolation("r", UNKNOWN_EVENT), LayoutViolation("b", UNKNOWN_EVENT), LayoutViolation("i", UNKNOWN_RULE)),
            reported,
        )
        assertEquals(FormState(), FormState().reduce(NodeEvent.Submit, spec.root))
        assertEquals(FormState(), FormState().reduce(NodeEvent.Unknown, spec.root))
    }

    @Test
    fun newerSchemaIsReported() {
        val json = """{"id": "s", "schema": 99, "root": {"type": "text", "id": "t", "text": "x"}}"""
        val reported = mutableListOf<LayoutViolation>()

        assertFailsWith<IllegalArgumentException> { ScreenSpec.decode(json) }
        assertEquals(99, ScreenSpec.decode(json) { reported += it }.schema)
        assertEquals(listOf(LayoutViolation("t", NEWER_SCHEMA)), reported)
    }

    @Test
    fun invalidPatternIsReportedAndAcceptsEveryValue() {
        val input = """{"type": "input", "id": "i", "field": "f", "label": "L", "rules": [{"type": "pattern", "regex": "([a-z", "message": "m"}]}"""
        val (spec, reported) = lenient(column(input))

        assertEquals(listOf(LayoutViolation("i", INVALID_PATTERN)), reported)
        assertEquals(emptyMap(), FormState().reduce(NodeEvent.Submit, spec.root).errors)
    }

    @Test
    fun violatingTreeIsRepairedForBothPlatforms() {
        val (spec, reported) = lenient(
            """{"type": "list", "id": "root", "items": [
              {"type": "list", "id": "inner", "spacing": "s", "items": [{"type": "text", "id": "a", "text": "1"}, {"type": "text", "id": "a", "text": "2"}]},
              {"type": "column", "id": "scroll", "scroll": true, "children": [{"type": "spacer", "id": "gap", "flexible": true}]},
              {"type": "list", "id": "tags", "axis": "horizontal", "items": [{"type": "row", "id": "more", "scroll": true, "children": []}]}
            ]}""",
        )

        assertEquals(
            Node.LazyList(
                "root",
                listOf(
                    Node.Column("inner", listOf(Node.Text("a", "1"), Node.Text("a#2", "2")), Space.S),
                    Node.Column("scroll", listOf(Node.Spacer("gap"))),
                    Node.LazyList("tags", listOf(Node.Row("more", emptyList())), ListAxis.Horizontal),
                ),
            ),
            spec.root,
        )
        assertEquals(listOf("inner", "a", "scroll", "gap", "more"), reported.map { it.nodeId })
        assertEquals(emptyList(), spec.root.violations())
    }

    @Test
    fun unknownNodesWithoutIdsAreNotDuplicates() {
        val spec = strict("""{"type": "column", "id": "root", "children": [{"type": "carousel"}, {"type": "banner"}]}""")

        assertEquals(listOf("", "#2"), assertIs<Node.Column>(spec.root).children.map { it.id })
    }

    @Test
    fun sameFieldInTwoControlsIsReportedAndIdsMayRepeatAcrossBranches() {
        val (_, reported) = lenient(
            """{"type": "column", "id": "root", "children": [
              {"type": "row", "id": "a", "children": [{"type": "input", "id": "x", "field": "email", "label": "L"}]},
              {"type": "row", "id": "b", "children": [{"type": "toggle", "id": "x", "field": "email", "label": "L"}]}
            ]}""",
        )

        assertEquals(listOf(LayoutViolation("x", DUPLICATE_FIELD)), reported)
    }

    @Test
    fun tooDeepJsonIsRejectedWithoutOverflowingTheStack() {
        assertFailsWith<SerializationException> { strict(boxes(5000)) }
        assertFailsWith<SerializationException> { strict(boxes(64)) }
        assertEquals(emptyList(), strict(boxes(63)).root.violations())
    }

    @Test
    fun bracketsInsideTextDoNotCountAsNesting() {
        val brackets = "[{".repeat(200)

        assertEquals(Node.Text("t", "\"$brackets"), strict("""{"type": "text", "id": "t", "text": "\"$brackets"}""").root)
    }

    @Test
    fun formStateDoesNotPrintValues() {
        val printed = FormState(mapOf("password" to "hunter2"), mapOf("password" to "Too short")).toString()

        assertFalse("hunter2" in printed)
        assertTrue("password" in printed)
    }

    @Test
    fun unknownNodeTypeDecodesToUnknownWithItsFallback() {
        assertEquals(Node.Unknown("c"), tree("""{"type": "carousel", "id": "c", "items": [{"type": "text", "id": "t", "text": "x"}]}"""))
        assertEquals(
            Node.Unknown("c", Node.Text("t", "Update the app")),
            tree("""{"type": "carousel", "id": "c", "fallback": {"type": "text", "id": "t", "text": "Update the app"}}"""),
        )
    }

    @Test
    fun verticalScrollInsideVerticalScrollIsReportedAtAnyDepth() {
        val nested = tree("""{"type": "column", "id": "root", "scroll": true, "children": [{"type": "list", "id": "inner", "items": []}]}""")
        val deep = tree(
            """{"type": "list", "id": "feed", "items": [{"type": "box", "id": "card", "children": [{"type": "column", "id": "inner", "scroll": true, "children": []}]}]}""",
        )

        assertEquals(listOf(LayoutViolation("inner", NESTED_VERTICAL_SCROLL)), nested.violations())
        assertEquals(listOf(LayoutViolation("inner", NESTED_VERTICAL_SCROLL)), deep.violations())
    }

    @Test
    fun horizontalScrollInsideHorizontalScrollIsReported() {
        val nested = tree("""{"type": "row", "id": "root", "scroll": true, "children": [{"type": "list", "id": "inner", "axis": "horizontal", "items": []}]}""")

        assertEquals(listOf(LayoutViolation("inner", NESTED_HORIZONTAL_SCROLL)), nested.violations())
    }

    @Test
    fun flexibleSpacerInsideAScrollOrAListIsReported() {
        val inScroll = tree("""{"type": "column", "id": "root", "scroll": true, "children": [{"type": "spacer", "id": "gap", "flexible": true}]}""")
        val inList = tree("""{"type": "list", "id": "root", "items": [{"type": "spacer", "id": "gap", "flexible": true}]}""")
        val inCardOfCarouselInList = tree(
            """{"type": "list", "id": "root", "items": [{"type": "list", "id": "carousel", "axis": "horizontal", "items": [
              {"type": "column", "id": "card", "children": [{"type": "spacer", "id": "gap", "flexible": true}]}]}]}""",
        )

        assertEquals(listOf(LayoutViolation("gap", FLEXIBLE_SPACER_IN_SCROLL)), inScroll.violations())
        assertEquals(listOf(LayoutViolation("gap", FLEXIBLE_SPACER_IN_SCROLL)), inList.violations())
        assertEquals(listOf(LayoutViolation("gap", FLEXIBLE_SPACER_IN_SCROLL)), inCardOfCarouselInList.violations())
    }

    @Test
    fun duplicateSiblingIdsAreReported() {
        val list = tree("""{"type": "list", "id": "root", "items": [{"type": "text", "id": "a", "text": "1"}, {"type": "text", "id": "a", "text": "2"}]}""")

        assertEquals(listOf(LayoutViolation("a", DUPLICATE_ID)), list.violations())
    }

    @Test
    fun crossAxisNestingFixedSizeListsAndFlexibleSpacersInBoundedAxesAreAllowed() {
        val allowed = tree(
            """
            {"type": "column", "id": "root", "children": [
              {"type": "list", "id": "carousel", "axis": "horizontal", "items": [
                {"type": "column", "id": "card", "children": [{"type": "spacer", "id": "gap", "flexible": true}]}
              ]},
              {"type": "column", "id": "feed", "scroll": true, "children": [
                {"type": "list", "id": "fixed", "size": 200, "items": []},
                {"type": "list", "id": "tags", "axis": "horizontal", "items": []},
                {"type": "row", "id": "actions", "children": [{"type": "spacer", "id": "push", "flexible": true}]}
              ]}
            ]}
            """,
        )

        assertEquals(emptyList(), allowed.violations())
        assertEquals(emptyList(), loginTree.violations())
    }

    @Test
    fun decodeThrowsOnAViolationByDefaultAndHandsItToTheHandlerOtherwise() {
        val json = """{"id": "s", "root": {"type": "list", "id": "root", "items": [{"type": "list", "id": "inner", "items": []}]}}"""
        val reported = mutableListOf<LayoutViolation>()

        assertFailsWith<IllegalArgumentException> { ScreenSpec.decode(json) }
        ScreenSpec.decode(json) { reported += it }
        assertEquals(listOf(LayoutViolation("inner", NESTED_VERTICAL_SCROLL)), reported)
    }

    @Test
    fun formStoresValuesAndValidatesOnSubmit() {
        val typed = FormState().reduce(NodeEvent.ValueChanged("email", "nope"), loginTree)
        val submitted = typed.reduce(NodeEvent.Submit, loginTree)
        val fixed = submitted.reduce(NodeEvent.ValueChanged("email", "a@b.c"), loginTree)

        assertEquals(mapOf("email" to "nope"), typed.values)
        assertEquals(mapOf("email" to "Not an email"), submitted.errors)
        assertTrue(fixed.errors.isEmpty())
        assertEquals(mapOf("email" to "Enter your email"), FormState().reduce(NodeEvent.Submit, loginTree).errors)
        assertEquals(typed, typed.reduce(NodeEvent.Click("submit"), loginTree))
    }
}
