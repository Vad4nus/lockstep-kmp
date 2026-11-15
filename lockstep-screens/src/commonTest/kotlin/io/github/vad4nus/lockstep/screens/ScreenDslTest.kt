package io.github.vad4nus.lockstep.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScreenDslTest {

    @Test
    fun dslBuildsTheSameTreeAsTheJsonFixture() {
        val spec = screen("login") {
            column("root", spacing = Space.M, style = Style(padding = Space.M)) {
                text("title", "Sign in", TextStyleToken.Title, a11y = A11y(heading = true))
                input(
                    "email", "email", "Email", InputKind.Email,
                    listOf(InputRule.Required("Enter your email"), InputRule.Pattern(".+@.+", "Not an email")),
                )
                toggle("remember", "remember", "Remember me")
                button("submit", "Sign in", NodeEvent.Submit)
            }
        }

        assertEquals(ScreenSpec.decode(LOGIN_JSON), spec)
    }

    @Test
    fun crossAxisNestingFixedSizeListsAndFlexibleSpacersInBoundedAxesBuild() {
        val spec = screen("feed") {
            column("root") {
                lazyRow("carousel") {
                    column("card") {
                        text("name", "Card")
                        flexibleSpacer("gap")
                    }
                }
                scrollColumn("feed") {
                    lazyColumn("fixed", height = 200) { text("item", "Item") }
                    lazyRow("tags") { text("tag", "Tag") }
                    row("actions") {
                        text("label", "More")
                        flexibleSpacer("push")
                    }
                }
            }
        }

        assertEquals(emptyList(), spec.root.violations())
    }

    @Test
    fun containersTakeAnEvent() {
        val spec = screen("orders") {
            lazyColumn("root") {
                row("order", event = NodeEvent.Navigate("orders/1")) { text("name", "Order 1") }
                box("banner", NodeEvent.Click("banner")) { text("title", "Sale") }
                column("card", event = NodeEvent.Click("card")) { text("body", "Card") }
            }
        }

        assertEquals(
            Node.LazyList(
                "root",
                listOf(
                    Node.Row("order", listOf(Node.Text("name", "Order 1")), event = NodeEvent.Navigate("orders/1")),
                    Node.Box("banner", listOf(Node.Text("title", "Sale")), NodeEvent.Click("banner")),
                    Node.Column("card", listOf(Node.Text("body", "Card")), event = NodeEvent.Click("card")),
                ),
            ),
            spec.root,
        )
    }

    @Test
    fun duplicateSiblingIdsThrow() {
        assertFailsWith<IllegalArgumentException> {
            screen("s") {
                lazyColumn("root") {
                    text("a", "1")
                    text("a", "2")
                }
            }
        }
    }

    @Test
    fun aScreenNeedsExactlyOneRoot() {
        assertFailsWith<IllegalArgumentException> {
            screen("s") {
                text("a", "1")
                text("b", "2")
            }
        }
    }
}
