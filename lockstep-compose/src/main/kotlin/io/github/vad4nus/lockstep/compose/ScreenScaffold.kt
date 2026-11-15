package io.github.vad4nus.lockstep.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Slot-based screen frame. [contentWindowInsets] is the only inset policy; the content receives the
 * resulting padding. [imeAware] lifts the whole frame, snackbar included, above the keyboard.
 * [onRefresh] wraps the content in pull-to-refresh. [overlay] is drawn over the content area, between the bars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    utilityBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHost: @Composable () -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    imeAware: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = if (imeAware) modifier.imePadding() else modifier,
        topBar = {
            Column {
                topBar()
                utilityBar()
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = snackbarHost,
        contentWindowInsets = contentWindowInsets,
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            if (onRefresh == null) {
                content(padding)
            } else {
                val direction = LocalLayoutDirection.current
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.padding(top = padding.calculateTopPadding()),
                ) {
                    content(
                        PaddingValues(
                            start = padding.calculateStartPadding(direction),
                            end = padding.calculateEndPadding(direction),
                            bottom = padding.calculateBottomPadding(),
                        ),
                    )
                }
            }
            Box(Modifier.matchParentSize().padding(padding), content = overlay)
        }
    }
}
