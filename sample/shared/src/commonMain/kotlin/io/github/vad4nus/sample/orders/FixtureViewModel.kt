package io.github.vad4nus.sample.orders

import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import io.github.vad4nus.lockstep.mvvm.MessageKind
import io.github.vad4nus.lockstep.mvvm.navigation.StackNavigator
import io.github.vad4nus.lockstep.screens.A11y
import io.github.vad4nus.lockstep.screens.ButtonVariant
import io.github.vad4nus.lockstep.screens.ColorToken
import io.github.vad4nus.lockstep.screens.FormState
import io.github.vad4nus.lockstep.screens.InputKind
import io.github.vad4nus.lockstep.screens.InputRule
import io.github.vad4nus.lockstep.screens.NodeEvent
import io.github.vad4nus.lockstep.screens.ScreenSpec
import io.github.vad4nus.lockstep.screens.Space
import io.github.vad4nus.lockstep.screens.Style
import io.github.vad4nus.lockstep.screens.TextStyleToken
import io.github.vad4nus.lockstep.screens.box
import io.github.vad4nus.lockstep.screens.button
import io.github.vad4nus.lockstep.screens.column
import io.github.vad4nus.lockstep.screens.flexibleSpacer
import io.github.vad4nus.lockstep.screens.image
import io.github.vad4nus.lockstep.screens.input
import io.github.vad4nus.lockstep.screens.lazyColumn
import io.github.vad4nus.lockstep.screens.lazyRow
import io.github.vad4nus.lockstep.screens.reduce
import io.github.vad4nus.lockstep.screens.row
import io.github.vad4nus.lockstep.screens.screen
import io.github.vad4nus.lockstep.screens.scrollColumn
import io.github.vad4nus.lockstep.screens.text
import io.github.vad4nus.lockstep.screens.toggle

data class FixtureState(val screen: ScreenSpec, val form: FormState = FormState())

/** Shows one KMP-described screen from [fixtures] and keeps its form; a navigate event opens the fixture it names. */
class FixtureViewModel(name: String, private val navigator: StackNavigator) :
    SampleViewModel<FixtureState, OrdersAction>(FixtureState(fixtures[name] ?: missing(name))) {

    fun onEvent(event: NodeEvent) {
        updateState { copy(form = form.reduce(event, screen.root)) }
        when (event) {
            NodeEvent.Submit -> if (currentState.form.errors.isEmpty()) showMessage(StringDesc.Raw("Signed in"), MessageKind.Success)
            is NodeEvent.Navigate -> navigator.push(OrdersRoute.Fixture(event.destination))
            is NodeEvent.Click, is NodeEvent.Custom, is NodeEvent.ValueChanged, NodeEvent.Unknown -> Unit
        }
    }
}

private fun missing(name: String) = screen("missing") {
    column("root", style = Style(padding = Space.M)) { text("title", "There is no screen named $name") }
}

private val login = screen("login") {
    column("root", spacing = Space.M, style = Style(padding = Space.M)) {
        text("title", "Sign in", TextStyleToken.Title, a11y = A11y(heading = true))
        input(
            "email", "email", "Email", InputKind.Email,
            listOf(InputRule.Required("Enter your email"), InputRule.Pattern(".+@.+\\..+", "Not an email")),
        )
        input("password", "password", "Password", InputKind.Password, listOf(InputRule.MinLength(6, "At least 6 characters")))
        toggle("remember", "remember", "Remember me")
        row("actions", spacing = Space.S) {
            button("feed", "Feed", NodeEvent.Navigate("feed"), ButtonVariant.Text)
            flexibleSpacer("push")
            button("submit", "Sign in", NodeEvent.Submit)
        }
    }
}

private val feed = screen("feed") {
    scrollColumn("root", spacing = Space.M, style = Style(padding = Space.M)) {
        text("title", "Feed", TextStyleToken.Title, a11y = A11y(heading = true))
        lazyRow("tags", spacing = Space.S) {
            listOf("new", "paid", "shipped", "cancelled", "returned").forEach {
                text(it, it, TextStyleToken.Caption, style = Style(padding = Space.S, background = ColorToken.Surface, corner = Space.S))
            }
        }
        lazyColumn("orders", height = 240, spacing = Space.S) {
            (1..12).forEach { index ->
                row("order$index", spacing = Space.S) {
                    image("star", "star", size = 20, a11y = A11y(hidden = true))
                    text("title", "Order $index")
                    flexibleSpacer("push")
                    text("status", if (index % 2 == 0) "paid" else "new", TextStyleToken.Caption, ColorToken.Secondary)
                }
            }
        }
        box("banner", style = Style(padding = Space.M, background = ColorToken.Primary, corner = Space.M)) {
            text("text", "Boxes stack their children", color = ColorToken.Background)
        }
        button("update", "Update screen", NodeEvent.Navigate("update"), ButtonVariant.Secondary)
    }
}

private const val UPDATE_JSON = """
{
  "id": "update",
  "root": {
    "type": "column", "id": "root", "spacing": "m", "style": {"padding": "m"},
    "children": [
      {"type": "text", "id": "title", "text": "Served as JSON", "textStyle": "title", "a11y": {"heading": true}},
      {"type": "carousel", "id": "promo",
       "fallback": {"type": "text", "id": "fallback", "text": "This version cannot show a carousel. Update the app.", "color": "secondary"}},
      {"type": "map", "id": "silent"},
      {"type": "custom", "id": "badge", "key": "badge", "props": {"label": "Native badge"}},
      {"type": "button", "id": "login", "label": "Sign in again", "event": {"type": "navigate", "destination": "login"}, "variant": "text"}
    ]
  }
}
"""

private val fixtures: Map<String, ScreenSpec> = mapOf("login" to login, "feed" to feed, "update" to ScreenSpec.decode(UPDATE_JSON))
