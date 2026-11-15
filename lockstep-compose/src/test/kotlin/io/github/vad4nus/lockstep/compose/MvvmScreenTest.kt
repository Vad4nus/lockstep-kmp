package io.github.vad4nus.lockstep.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import io.github.vad4nus.lockstep.compose.screens.RenderNode
import io.github.vad4nus.lockstep.mvvm.DialogResult
import io.github.vad4nus.lockstep.mvvm.ErrorDisplay
import io.github.vad4nus.lockstep.mvvm.LoadingKind
import io.github.vad4nus.lockstep.mvvm.MessageAction
import io.github.vad4nus.lockstep.mvvm.MvvmViewModel
import io.github.vad4nus.lockstep.result.AppError
import io.github.vad4nus.lockstep.result.AppResult
import io.github.vad4nus.lockstep.screens.FormState
import io.github.vad4nus.lockstep.screens.Node
import io.github.vad4nus.lockstep.screens.NodeEvent
import io.github.vad4nus.lockstep.screens.reduce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MvvmScreenTest {

    private class ScreenViewModel : MvvmViewModel<String, Unit>("content") {
        fun message(text: String, action: MessageAction? = null) =
            showMessage(StringDesc.Raw(text), action = action, durationMillis = 1_000)

        fun failScreen() = request<Unit>({ AppResult.Failure(AppError.NoConnection) }, errors = ErrorDisplay.Screen) {}

        fun loadForever() = request<Unit>({ awaitCancellation() }, key = "other") {}

        fun loadInBackground() = request<Unit>({ awaitCancellation() }, loading = LoadingKind.Background, key = "background") {}

        fun confirm(onResult: (DialogResult) -> Unit) = showDialog(StringDesc.Raw("Delete?"), destructive = true, onResult = onResult)
    }

    private class FormViewModel : MvvmViewModel<FormState, Unit>(FormState()) {
        val field = Node.Input("email", "email", "Email")

        fun onEvent(event: NodeEvent) = updateState { reduce(event, field) }
    }

    @get:Rule
    val rule = createComposeRule()

    private val viewModel = ScreenViewModel()

    private fun showScreen() = rule.setContent {
        MvvmScreen(viewModel, loading = { Box(Modifier.testTag("loader")) }) { state, _ -> Text(state) }
    }

    @Test
    fun loaderRunsOverTheErrorViewAndRetryCarriesTheLabelOfTheError() {
        showScreen()
        rule.onNodeWithText("content").assertIsDisplayed()

        rule.runOnIdle {
            viewModel.failScreen()
            viewModel.loadForever()
        }

        rule.onNodeWithText("No connection").assertIsDisplayed()
        rule.onNodeWithText("Try again").assertIsDisplayed()
        rule.onNodeWithTag("loader").assertExists()
        rule.onNodeWithText("content").assertDoesNotExist()
    }

    @Test
    fun backgroundRequestDrawsItsLineOverTheContent() {
        rule.setContent {
            MvvmScreen(viewModel, backgroundLoading = { Box(Modifier.testTag("line")) }) { state, _ -> Text(state) }
        }
        rule.onNodeWithTag("line").assertDoesNotExist()

        rule.runOnIdle { viewModel.loadInBackground() }

        rule.onNodeWithTag("line").assertExists()
        rule.onNodeWithText("content").assertIsDisplayed()
    }

    @Test
    fun messageLeavesWhenItsTimeIsUp() {
        showScreen()
        rule.mainClock.autoAdvance = false

        rule.runOnUiThread { viewModel.message("Saved") }
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Saved").assertIsDisplayed()

        rule.mainClock.advanceTimeBy(1_500)
        rule.onNodeWithText("Saved").assertDoesNotExist()
        assertEquals(emptyList(), viewModel.chrome.value.messages)
    }

    @Test
    fun messageOnScreenLeavesTheQueueWithTheScreen() {
        var shown by mutableStateOf(true)
        rule.setContent { if (shown) MvvmScreen(viewModel) { state, _ -> Text(state) } }
        rule.runOnIdle { viewModel.message("Saved") }
        rule.onNodeWithText("Saved").assertIsDisplayed()

        rule.runOnIdle { shown = false }

        rule.runOnIdle { assertEquals(emptyList(), viewModel.chrome.value.messages) }
    }

    @Test
    fun messageActionRunsOnTap() {
        var undone = false
        showScreen()
        rule.runOnIdle { viewModel.message("Deleted", MessageAction(StringDesc.Raw("Undo")) { undone = true }) }

        rule.onNodeWithText("Undo").performClick()

        rule.runOnIdle {
            assertTrue(undone)
            assertEquals(emptyList(), viewModel.chrome.value.messages)
        }
    }

    @Test
    fun dialogButtonReportsTheResult() {
        val results = mutableListOf<DialogResult>()
        showScreen()
        rule.runOnIdle { viewModel.confirm { results += it } }
        rule.onNodeWithText("Delete?").assertIsDisplayed()

        rule.onNodeWithText("OK").performClick()

        rule.runOnIdle { assertEquals(listOf(DialogResult.Confirm), results) }
        rule.onNodeWithText("Delete?").assertDoesNotExist()
    }

    @Test
    fun typedTextGoesThroughTheViewModelAndBackIntoTheField() {
        val form = FormViewModel()
        rule.setContent { MvvmScreen(form) { state, _ -> RenderNode(form.field, state, form::onEvent) } }

        rule.onNode(hasSetTextAction()).performTextInput("a@b.c")

        rule.onNode(hasSetTextAction()).assertTextContains("a@b.c")
        assertEquals("a@b.c", form.state.value.values["email"])
    }

    @Test
    fun sheetDrawsItsLoaderOverTheContent() {
        rule.setContent {
            MvvmSheet(viewModel, loading = { Box(Modifier.size(10.dp).testTag("loader")) }) { Text(it) }
        }

        rule.runOnIdle { viewModel.loadForever() }

        rule.onNodeWithTag("loader").assertExists()
        rule.onNodeWithText("content").assertTopPositionInRootIsEqualTo(0.dp)
    }
}
