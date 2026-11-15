package io.github.vad4nus.lockstep.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.vad4nus.lockstep.mvvm.DialogResult
import io.github.vad4nus.lockstep.mvvm.MvvmViewModel
import io.github.vad4nus.lockstep.mvvm.UiDialog
import io.github.vad4nus.lockstep.mvvm.UiError

/** Compact loader and error view for sheets, sized to the content instead of the screen. */
public object SheetDefaults {

    /** A spinner centered in the width of the sheet. */
    @Composable
    public fun Loading(modifier: Modifier = Modifier) {
        Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    /** The error text and, when [onRetry] is given, a button labelled with the retry label of [error]. */
    @Composable
    public fun ErrorView(error: UiError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
        ErrorBody(error, onRetry, modifier.fillMaxWidth().padding(24.dp))
    }
}

/**
 * Sheet content bound to an [MvvmViewModel], with the same wiring as `MvvmScreen` but no scaffold,
 * top bar or pull-to-refresh. The [loading] view is drawn over the content and blocks it, [backgroundLoading] is drawn
 * along its top edge, and the snackbar over its bottom edge; the sheet owns its snackbar host because it is drawn in
 * its own window.
 * Insets and back handling belong to the enclosing `ModalBottomSheet`.
 */
@Composable
public fun <S : Any, A : Any> MvvmSheet(
    viewModel: MvvmViewModel<S, A>,
    modifier: Modifier = Modifier,
    onAction: (A) -> Unit = {},
    screenName: String? = null,
    loading: @Composable () -> Unit = { SheetDefaults.Loading() },
    backgroundLoading: @Composable () -> Unit = { ScreenDefaults.BackgroundLoading() },
    error: @Composable (UiError, (() -> Unit)?) -> Unit = { e, retry -> SheetDefaults.ErrorView(e, retry) },
    snackbar: @Composable (SnackbarData) -> Unit = { ScreenDefaults.Snackbar(it) },
    dialog: @Composable (UiDialog, (DialogResult) -> Unit) -> Unit = { d, onResult -> ScreenDefaults.Dialog(d, onResult) },
    content: @Composable ColumnScope.(S) -> Unit,
) {
    val host = MvvmHost(viewModel, onAction, screenName, dialog)
    Box(modifier) {
        Column {
            val failure = host.error
            if (failure != null) {
                error(failure, host.retry)
            } else {
                content(host.state)
            }
        }
        if (host.backgroundLoading) backgroundLoading()
        if (host.blocking) {
            Box(Modifier.matchParentSize().pointerInput(Unit) {})
            Box(Modifier.align(Alignment.Center)) { loading() }
        }
        SnackbarHost(host.snackbarHostState, Modifier.align(Alignment.BottomCenter), snackbar)
    }
}
