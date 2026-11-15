package io.github.vad4nus.lockstep.compose

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.vad4nus.lockstep.mvvm.DialogResult
import io.github.vad4nus.lockstep.mvvm.LoadingKind
import io.github.vad4nus.lockstep.mvvm.MessageKind
import io.github.vad4nus.lockstep.mvvm.MvvmViewModel
import io.github.vad4nus.lockstep.mvvm.ScreenChrome
import io.github.vad4nus.lockstep.mvvm.UiDialog
import io.github.vad4nus.lockstep.mvvm.UiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull

internal class HostState<S>(val state: S, chrome: ScreenChrome, retry: () -> Unit, val snackbarHostState: SnackbarHostState) {
    val blocking: Boolean = chrome.loading == LoadingKind.Blocking
    val refreshing: Boolean = chrome.loading == LoadingKind.Refresh
    val backgroundLoading: Boolean = chrome.backgroundLoading
    val error: UiError? = chrome.error
    val retry: (() -> Unit)? = retry.takeIf { chrome.error?.retryLabel != null }
}

internal class MessageVisuals(val kind: MessageKind, override val message: String, override val actionLabel: String?) : SnackbarVisuals {
    override val withDismissAction: Boolean get() = false
    override val duration: SnackbarDuration get() = SnackbarDuration.Indefinite
}

/** The kind of the message a snackbar of `MvvmScreen` or `MvvmSheet` shows, for a custom snackbar. */
public val SnackbarData.messageKind: MessageKind
    get() = (visuals as? MessageVisuals)?.kind ?: MessageKind.Info

/**
 * Binds a ViewModel to the composition: state, one-shot actions, the message queue, the dialog and the lifecycle hooks.
 *
 * The state is collected on the immediate main dispatcher, so a text field bound to it never sees a value older than
 * the one the user has just typed. A message stays for its own duration, extended for assistive technologies, and
 * leaves the queue when the time is up, when its action is tapped, or when the screen it is on goes away.
 */
@Composable
internal fun <S : Any, A : Any> MvvmHost(
    viewModel: MvvmViewModel<S, A>,
    onAction: (A) -> Unit,
    screenName: String?,
    dialog: @Composable (UiDialog, (DialogResult) -> Unit) -> Unit,
): HostState<S> {
    val state by viewModel.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    val chrome by viewModel.chrome.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val accessibility = LocalAccessibilityManager.current
    val tracker = LocalScreenTracker.current
    val currentOnAction by rememberUpdatedState(onAction)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.actions.collect { currentOnAction(it) }
        }
    }

    val message = chrome.messages.firstOrNull()
    LaunchedEffect(message?.id) {
        if (message == null) return@LaunchedEffect
        val label = message.actionLabel?.toString(context)
        val visuals = MessageVisuals(message.kind, message.text.toString(context), label)
        val timeout = accessibility
            ?.calculateRecommendedTimeoutMillis(message.durationMillis, containsText = true, containsControls = label != null)
            ?: message.durationMillis
        var clicked = false
        try {
            clicked = withTimeoutOrNull(timeout) { snackbarHostState.showSnackbar(visuals) } == SnackbarResult.ActionPerformed
        } finally {
            if (clicked) viewModel.messageActionClicked(message.id) else viewModel.messageShown(message.id)
        }
    }

    chrome.dialog?.let { shown -> dialog(shown) { viewModel.dialogResult(shown.id, it) } }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.screenAppeared()
        if (screenName != null) tracker?.screenViewed(screenName)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.screenDisappeared() }

    return HostState(state, chrome, viewModel::retry, snackbarHostState)
}
