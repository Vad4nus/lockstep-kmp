package io.github.vad4nus.lockstep.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.resources.desc.StringDesc
import io.github.vad4nus.lockstep.UiFlow
import io.github.vad4nus.lockstep.UiStateFlow
import io.github.vad4nus.lockstep.asUiFlow
import io.github.vad4nus.lockstep.asUiStateFlow
import io.github.vad4nus.lockstep.result.AppError
import io.github.vad4nus.lockstep.result.AppResult
import io.github.vad4nus.lockstep.result.catching
import io.github.vad4nus.lockstep.result.flatMap
import io.github.vad4nus.lockstep.result.onFailure
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What a request does when another one with the same key is still running: [CancelPrevious] cancels that one,
 * [KeepRunning] leaves it and does not start the new one, [Parallel] runs both. It applies only to requests that
 * have a key.
 */
public enum class RequestPolicy { CancelPrevious, KeepRunning, Parallel }

/**
 * Where a failed request shows its error: [Message] queues a snackbar or toast, [Screen] replaces the content with
 * the error view and its retry button, [Dialog] opens an alert, and [Silent] shows nothing.
 */
public enum class ErrorDisplay { Message, Screen, Dialog, Silent }

/**
 * The base ViewModel: the screen [state], the [chrome] the base screen draws around it, and one-shot [actions].
 *
 * Use it from the main thread only: requests run on `Dispatchers.Main.immediate` and nothing here is synchronized.
 */
public abstract class MvvmViewModel<S : Any, A : Any>(
    initialState: S,
    private val env: MvvmEnvironment = MvvmEnvironment.Default,
) : ViewModel() {

    private val stateFlow = MutableStateFlow(initialState)
    private val chromeFlow = MutableStateFlow(ScreenChrome())
    private val actionChannel = Channel<A>(Channel.UNLIMITED)

    /** The screen data. */
    public val state: UiStateFlow<S> = stateFlow.asUiStateFlow()

    /** What the base screen draws around the content: loaders, the screen error, messages and the dialog. */
    public val chrome: UiStateFlow<ScreenChrome> = chromeFlow.asUiStateFlow()

    /** Screen effects such as scroll or focus. They wait until the screen collects them; collect them in one place. */
    public val actions: UiFlow<A> = actionChannel.receiveAsFlow().asUiFlow()

    private class Retry(val key: Any?, val rerun: () -> Unit)

    private class ScreenFailure(val key: Any?, val error: UiError, val rerun: (() -> Unit)?)

    private val jobs = mutableMapOf<Any, Job>()
    private val loading = mutableMapOf<LoadingKind, Int>()
    private val failures = mutableListOf<ScreenFailure>()
    private val messageActions = mutableMapOf<Long, () -> Unit>()
    private var dialogCallback: ((DialogResult) -> Unit)? = null
    private var failing: Retry? = null
    private var nextId = 1L
    private var firstAppearDone = false

    /** The latest state. */
    protected val currentState: S get() = stateFlow.value

    /** Replaces the state with what [transform] makes of the current one. */
    protected fun updateState(transform: S.() -> S) {
        stateFlow.update { it.transform() }
    }

    /** Sends a one-shot [action] to the screen. It waits in a queue until the screen collects [actions]. */
    protected fun sendAction(action: A) {
        actionChannel.trySend(action)
    }

    /**
     * Runs [call] in `viewModelScope` and hands its value to [onSuccess].
     *
     * - [loading] picks the loader shown while the request runs, or none.
     * - [key] names the request. [policy] decides what happens when a request with the same key is still running;
     *   without a key every call runs on its own and [policy] does nothing.
     * - [errors] picks where a failure is shown; [onError] replaces that for this request. A screen error goes away
     *   on [retry] or when a request with the same key starts. Screen requests without a key count as one key here,
     *   so give keys to the requests whose screen errors must not clear each other.
     * - An exception thrown by [call], [onSuccess] or [onError] is mapped by the environment's error mapper and
     *   handled as a failure. Every failure goes to the environment's error reporter first.
     * - [onFinally] runs after success, failure and cancellation alike, so it also runs for a request that a newer one
     *   replaced and for the requests of a cleared ViewModel.
     */
    protected fun <T> request(
        call: suspend () -> AppResult<T>,
        loading: LoadingKind? = LoadingKind.Blocking,
        key: Any? = null,
        policy: RequestPolicy = RequestPolicy.CancelPrevious,
        errors: ErrorDisplay = ErrorDisplay.Message,
        onError: ((AppError) -> Unit)? = null,
        onFinally: (() -> Unit)? = null,
        onSuccess: suspend (T) -> Unit,
    ): Job {
        if (key != null) {
            val running = jobs[key]
            if (running != null && running.isActive) {
                when (policy) {
                    RequestPolicy.CancelPrevious -> running.cancel()
                    RequestPolicy.KeepRunning -> return running
                    RequestPolicy.Parallel -> Unit
                }
            }
        }
        if ((key != null || errors == ErrorDisplay.Screen) && failures.removeAll { it.key == key }) refreshChrome()
        val retry = Retry(key) { request(call, loading, key, policy, errors, onError, onFinally, onSuccess) }
        val job = viewModelScope.launch {
            beginLoading(loading)
            try {
                val result = catching(env.errorMapper) {
                    call().also { if (it is AppResult.Success) onSuccess(it.value) }
                }.flatMap { it }
                if (result is AppResult.Failure) fail(result.error, errors, onError, retry)
            } finally {
                endLoading(loading)
                onFinally?.invoke()
            }
        }
        if (key != null && policy != RequestPolicy.Parallel) {
            jobs[key] = job
            job.invokeOnCompletion { if (jobs[key] === job) jobs.remove(key) }
        }
        return job
    }

    private suspend fun fail(error: AppError, display: ErrorDisplay, onError: ((AppError) -> Unit)?, retry: Retry) {
        env.errorReporter(error)
        val outer = failing
        failing = retry
        try {
            if (onError == null) return handleError(error, display)
            catching(env.errorMapper) { onError(error) }.onFailure { thrown ->
                env.errorReporter(thrown)
                handleError(thrown, display)
            }
        } finally {
            failing = outer
        }
    }

    /** Takes the screen error away and reruns every request that put one there. */
    public fun retry() {
        val reruns = failures.mapNotNull { it.rerun }
        failures.clear()
        refreshChrome()
        reruns.forEach { it() }
    }

    /**
     * Shows [error] where [display] says. Override it for app-wide rules, for example to open the login screen on
     * [AppError.Unauthorized]. Called for [ErrorDisplay.Screen] outside a failed request it shows an error without
     * a retry button.
     */
    protected open fun handleError(error: AppError, display: ErrorDisplay) {
        val presented = env.errorPresenter.present(error)
        when (display) {
            ErrorDisplay.Message -> showMessage(presented.message, MessageKind.Error)
            ErrorDisplay.Screen -> {
                failures += ScreenFailure(failing?.key, presented, failing?.rerun)
                refreshChrome()
            }
            ErrorDisplay.Dialog -> showDialog(text = presented.message) {}
            ErrorDisplay.Silent -> Unit
        }
    }

    /**
     * Queues a message. It stays on screen for [durationMillis]; Android extends that for users of assistive
     * technologies, and iOS announces the message to VoiceOver.
     */
    protected fun showMessage(
        text: StringDesc,
        kind: MessageKind = MessageKind.Info,
        action: MessageAction? = null,
        durationMillis: Long = if (action == null) 4_000 else 8_000,
    ) {
        val id = nextId++
        if (action != null) messageActions[id] = action.onClick
        chromeFlow.update { it.copy(messages = it.messages + UiMessage(id, text, kind, action?.label, durationMillis)) }
    }

    /** Shows a dialog in place of the open one, whose [onResult] then gets [DialogResult.Dismiss]. */
    protected fun showDialog(
        text: StringDesc,
        title: StringDesc? = null,
        confirm: StringDesc = env.texts.ok,
        dismiss: StringDesc? = null,
        destructive: Boolean = false,
        onResult: (DialogResult) -> Unit,
    ) {
        val dialog = UiDialog(nextId++, title, text, confirm, dismiss, destructive)
        val replaced = dialogCallback
        dialogCallback = onResult
        chromeFlow.update { it.copy(dialog = dialog) }
        replaced?.invoke(DialogResult.Dismiss)
    }

    /** The base screen reports that the message [id] has left the screen: it leaves the queue. */
    public fun messageShown(id: Long) {
        messageActions.remove(id)
        chromeFlow.update { chrome -> chrome.copy(messages = chrome.messages.filterNot { it.id == id }) }
    }

    /** The base screen reports a tap on the action of the message [id]: the message leaves the queue and its action runs. */
    public fun messageActionClicked(id: Long) {
        val action = messageActions.remove(id)
        messageShown(id)
        action?.invoke()
    }

    /** Closes the dialog [id] with [result]. A report about a dialog that is no longer the open one is ignored. */
    public fun dialogResult(id: Long, result: DialogResult) {
        if (chromeFlow.value.dialog?.id != id) return
        val callback = dialogCallback
        dialogCallback = null
        chromeFlow.update { it.copy(dialog = null) }
        callback?.invoke(result)
    }

    /** The base screen reports that the screen became visible: runs [onFirstAppear] the first time, then [onAppear]. */
    public fun screenAppeared() {
        if (!firstAppearDone) {
            firstAppearDone = true
            onFirstAppear()
        }
        onAppear()
    }

    /** The base screen reports that the screen stopped being visible: runs [onDisappear]. */
    public fun screenDisappeared() {
        onDisappear()
    }

    /** The screen became visible for the first time in the life of this ViewModel: the place to load its data. */
    protected open fun onFirstAppear() {}

    /** The screen became visible: it was opened, uncovered by a pop, or the app returned to the foreground. */
    protected open fun onAppear() {}

    /** The screen stopped being visible: it was closed, covered by another screen, or the app went to the background. */
    protected open fun onDisappear() {}

    override fun onCleared() {
        messageActions.clear()
        dialogCallback = null
        failures.clear()
        jobs.clear()
    }

    private fun beginLoading(kind: LoadingKind?) {
        if (kind == null) return
        loading[kind] = (loading[kind] ?: 0) + 1
        refreshChrome()
    }

    private fun endLoading(kind: LoadingKind?) {
        if (kind == null) return
        loading[kind] = ((loading[kind] ?: 0) - 1).coerceAtLeast(0)
        refreshChrome()
    }

    private fun isLoading(kind: LoadingKind): Boolean = (loading[kind] ?: 0) > 0

    private fun refreshChrome() {
        val shown = failures.lastOrNull()?.error
        val retryable = failures.any { it.rerun != null }
        chromeFlow.update {
            it.copy(
                loading = LoadingKind.Blocking.takeIf(::isLoading) ?: LoadingKind.Refresh.takeIf(::isLoading),
                backgroundLoading = isLoading(LoadingKind.Background),
                error = shown?.copy(retryLabel = if (retryable) shown.retryLabel ?: env.texts.retry else null),
            )
        }
    }
}
