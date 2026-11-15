package io.github.vad4nus.lockstep.compose.screens

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.vad4nus.lockstep.screens.FormState
import io.github.vad4nus.lockstep.screens.InputKind
import io.github.vad4nus.lockstep.screens.Node
import io.github.vad4nus.lockstep.screens.NodeEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RenderNodeTest {

    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<NodeEvent>()

    private fun render(node: Node) = rule.setContent { RenderNode(node, FormState(), events::add) }

    @Test
    fun toggleIsOneSwitchWithItsLabel() {
        render(Node.Toggle("remember", "remember", "Remember me"))

        rule.onNodeWithText("Remember me")
            .assert(expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff()
            .performClick()

        assertEquals(listOf<NodeEvent>(NodeEvent.ValueChanged("remember", "true")), events)
    }

    @Test
    fun containerWithAnEventIsOneButton() {
        render(Node.Row("order", listOf(Node.Text("name", "Order 1")), event = NodeEvent.Navigate("orders/1")))

        rule.onNodeWithText("Order 1").assert(expectValue(SemanticsProperties.Role, Role.Button)).performClick()

        assertEquals(listOf<NodeEvent>(NodeEvent.Navigate("orders/1")), events)
    }

    @Test
    fun unknownEventsAreNeverSent() {
        render(
            Node.Column(
                "root",
                listOf(
                    Node.Box("banner", listOf(Node.Text("title", "Sale")), NodeEvent.Unknown),
                    Node.Button("go", "Go", NodeEvent.Unknown),
                ),
            ),
        )

        rule.onNodeWithText("Sale").assertHasNoClickAction()
        rule.onNodeWithText("Go").assertIsNotEnabled()
    }

    @Test
    fun missingImageKeepsItsPlace() {
        render(Node.Row("row", listOf(Node.Image("icon", "missing", 40), Node.Text("label", "Label"))))

        rule.onNodeWithText("Label").assertLeftPositionInRootIsEqualTo(40.dp)
    }

    @Test
    fun emailInputAsksForAutofill() {
        render(Node.Input("email", "email", "Email", InputKind.Email))

        rule.onNode(hasSetTextAction()).assert(expectValue(SemanticsProperties.ContentType, ContentType.EmailAddress))
    }
}
