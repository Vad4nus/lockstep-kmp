package io.github.vad4nus.lockstep.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.localized
import io.github.vad4nus.lockstep.mvvm.DialogResult
import io.github.vad4nus.lockstep.mvvm.MessageKind
import io.github.vad4nus.lockstep.mvvm.MvvmEnvironment
import io.github.vad4nus.lockstep.mvvm.UiDialog
import io.github.vad4nus.lockstep.mvvm.UiError
import androidx.compose.material3.Snackbar as MaterialSnackbar

/** Receives the name of every screen that reaches the started state. */
public fun interface ScreenTracker {
    /** Called with the `screenName` of a screen every time it becomes visible. */
    public fun screenViewed(name: String)
}

/** The tracker `MvvmScreen` and `MvvmSheet` report their `screenName` to; null by default. */
public val LocalScreenTracker: ProvidableCompositionLocal<ScreenTracker?> =
    staticCompositionLocalOf { null }

/** Default pieces of a full screen: blocking loader, background loading line, scrollable error view, snackbar and confirmation dialog. */
public object ScreenDefaults {

    /** A spinner in the middle of the content area; touches do not reach the content under it. */
    @Composable
    public fun LoadingOverlay(modifier: Modifier = Modifier) {
        Box(modifier.fillMaxSize().pointerInput(Unit) {}, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    /** A thin progress line along the top edge, shown while a background request runs. */
    @Composable
    public fun BackgroundLoading(modifier: Modifier = Modifier) {
        LinearProgressIndicator(modifier.fillMaxWidth())
    }

    /** The error text and, when [onRetry] is given, a button labelled with the retry label of [error]. */
    @Composable
    public fun ErrorView(error: UiError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
        ErrorBody(error, onRetry, modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), Arrangement.Center)
    }

    /** A snackbar in the error colors for an error message and in the standard ones for the rest. */
    @Composable
    public fun Snackbar(data: SnackbarData, modifier: Modifier = Modifier) {
        val error = data.messageKind == MessageKind.Error
        MaterialSnackbar(
            data,
            modifier,
            containerColor = if (error) MaterialTheme.colorScheme.errorContainer else SnackbarDefaults.color,
            contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else SnackbarDefaults.contentColor,
            actionColor = if (error) MaterialTheme.colorScheme.onErrorContainer else SnackbarDefaults.actionColor,
        )
    }

    /** An alert whose confirm button is in the error color when the dialog is destructive. */
    @Composable
    public fun Dialog(dialog: UiDialog, onResult: (DialogResult) -> Unit) {
        AlertDialog(
            onDismissRequest = { onResult(DialogResult.Dismiss) },
            title = dialog.title?.let { { Text(it.localized()) } },
            text = { Text(dialog.text.localized()) },
            confirmButton = {
                TextButton(
                    onClick = { onResult(DialogResult.Confirm) },
                    colors = if (dialog.destructive) {
                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.textButtonColors()
                    },
                ) { Text(dialog.confirm.localized()) }
            },
            dismissButton = dialog.dismiss?.let {
                { TextButton(onClick = { onResult(DialogResult.Dismiss) }) { Text(it.localized()) } }
            },
        )
    }
}

@Composable
internal fun ErrorBody(
    error: UiError,
    onRetry: (() -> Unit)?,
    modifier: Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) {
    Column(modifier, verticalArrangement = verticalArrangement, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(error.message.localized(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text((error.retryLabel ?: MvvmEnvironment.Default.texts.retry).localized())
            }
        }
    }
}
