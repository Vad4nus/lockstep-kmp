package io.github.vad4nus.lockstep.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vad4nus.lockstep.mvvm.DialogResult
import io.github.vad4nus.lockstep.mvvm.MvvmViewModel
import io.github.vad4nus.lockstep.mvvm.UiDialog
import io.github.vad4nus.lockstep.mvvm.UiError

/**
 * Full screen bound to an [MvvmViewModel]: scaffold, snackbar messages, confirmation dialog, blocking loader,
 * background loading line, error view, lifecycle hooks and one-shot actions.
 *
 * The [error] view replaces the content, and the [loading] overlay is drawn over either of them, so a retry shows
 * its loader over the error. The overlay covers the content area only: the bars stay usable, as they do on iOS.
 * [backgroundLoading] is drawn along the top edge of the content while a background request runs.
 * [loading], [backgroundLoading], [error], [snackbar] and [dialog] replace the pieces of [ScreenDefaults] for this screen.
 * [onBack] installs a back handler for screens that are not inside a `NavigatorDisplay`.
 */
@Composable
public fun <S : Any, A : Any> MvvmScreen(
    viewModel: MvvmViewModel<S, A>,
    modifier: Modifier = Modifier,
    onAction: (A) -> Unit = {},
    screenName: String? = null,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    utilityBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    imeAware: Boolean = false,
    loading: @Composable () -> Unit = { ScreenDefaults.LoadingOverlay() },
    backgroundLoading: @Composable () -> Unit = { ScreenDefaults.BackgroundLoading() },
    error: @Composable (UiError, (() -> Unit)?) -> Unit = { e, retry -> ScreenDefaults.ErrorView(e, retry) },
    snackbar: @Composable (SnackbarData) -> Unit = { ScreenDefaults.Snackbar(it) },
    dialog: @Composable (UiDialog, (DialogResult) -> Unit) -> Unit = { d, onResult -> ScreenDefaults.Dialog(d, onResult) },
    content: @Composable (S, PaddingValues) -> Unit,
) {
    val host = MvvmHost(viewModel, onAction, screenName, dialog)
    if (onBack != null) BackHandler(onBack = onBack)

    ScreenScaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        utilityBar = utilityBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = { SnackbarHost(host.snackbarHostState, snackbar = snackbar) },
        overlay = {
            if (host.backgroundLoading) backgroundLoading()
            if (host.blocking) loading()
        },
        contentWindowInsets = contentWindowInsets,
        imeAware = imeAware,
        onRefresh = onRefresh,
        refreshing = host.refreshing,
    ) { padding ->
        val failure = host.error
        if (failure != null) {
            Box(Modifier.padding(padding)) { error(failure, host.retry) }
        } else {
            content(host.state, padding)
        }
    }
}
