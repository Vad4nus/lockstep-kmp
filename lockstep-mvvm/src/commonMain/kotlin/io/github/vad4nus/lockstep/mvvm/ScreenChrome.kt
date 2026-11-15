package io.github.vad4nus.lockstep.mvvm

import dev.icerock.moko.resources.desc.StringDesc

/** Which loader a request shows; see [ScreenChrome.loading] and [ScreenChrome.backgroundLoading]. */
public enum class LoadingKind { Blocking, Background, Refresh }

/** What went wrong, for an error view that picks an icon or an illustration. The default views show only the message. */
public enum class UiErrorKind { Network, Unauthorized, Server, Unknown }

/** The tone of a message; the default views tint an [Error] one. */
public enum class MessageKind { Info, Success, Error }

/** How the user closed a dialog: with the confirm button, or else with the dismiss button, a tap outside or the back button. */
public enum class DialogResult { Confirm, Dismiss }

/**
 * An error as the screen shows it. [retryLabel] is the text of the retry button, or null when
 * [MvvmViewModel.retry] has nothing to rerun.
 */
public data class UiError(val message: StringDesc, val kind: UiErrorKind, val retryLabel: StringDesc? = null)

/** A queued snackbar or toast. Both platforms keep it on screen for [durationMillis]. */
public data class UiMessage(
    val id: Long,
    val text: StringDesc,
    val kind: MessageKind,
    val actionLabel: StringDesc?,
    val durationMillis: Long,
)

/**
 * A dialog as the screen shows it. Without a [dismiss] text it has the confirm button only. [destructive] marks
 * a confirmation that deletes something or cannot be undone, and both platforms style the button for it.
 */
public data class UiDialog(
    val id: Long,
    val title: StringDesc?,
    val text: StringDesc,
    val confirm: StringDesc,
    val dismiss: StringDesc?,
    val destructive: Boolean,
)

/**
 * Everything the base screen draws around the content.
 *
 * [loading] is [LoadingKind.Blocking] or [LoadingKind.Refresh] while such a request runs, and [backgroundLoading] tells
 * about [LoadingKind.Background] ones. [error] is the screen error; it replaces the content and stays until
 * [MvvmViewModel.retry] or a new run of the request that failed. Loading and error do not depend on each other:
 * a loader can run over the error view.
 */
public data class ScreenChrome(
    val loading: LoadingKind? = null,
    val backgroundLoading: Boolean = false,
    val error: UiError? = null,
    val messages: List<UiMessage> = emptyList(),
    val dialog: UiDialog? = null,
)

/** A button on a message: its [label] and what [onClick] does. */
public class MessageAction(public val label: StringDesc, public val onClick: () -> Unit)
