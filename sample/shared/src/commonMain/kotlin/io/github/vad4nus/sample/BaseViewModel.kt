package io.github.vad4nus.sample

import io.github.vad4nus.lockstep.UiFlow
import io.github.vad4nus.lockstep.UiStateFlow
import io.github.vad4nus.lockstep.asUiFlow
import io.github.vad4nus.lockstep.asUiStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : Any, A : Any>(initial: S) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stateCell = MutableStateFlow(initial)
    private val actionChannel = Channel<A>(Channel.BUFFERED)

    val state: UiStateFlow<S> = stateCell.asUiStateFlow()
    val actions: UiFlow<A> = actionChannel.receiveAsFlow().asUiFlow()

    protected fun updateState(transform: S.() -> S) {
        stateCell.update { it.transform() }
    }

    protected fun sendAction(action: A) {
        scope.launch { actionChannel.send(action) }
    }

    protected fun launch(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    fun clear() {
        scope.cancel()
    }
}
