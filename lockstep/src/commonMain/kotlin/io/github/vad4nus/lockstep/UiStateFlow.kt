package io.github.vad4nus.lockstep

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A [StateFlow] that Swift can subscribe to; the Lockstep plugin exposes it on iOS as an observable `UiState` of a native value.
 *
 * It implements [StateFlow] by delegation, which kotlinx.coroutines marks as experimental for inheritance:
 * build against the coroutines version the library was compiled with (see the compatibility table).
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
public class UiStateFlow<T>(private val origin: StateFlow<T>) : StateFlow<T> by origin {

    /** Delivers the current value and every later one on `Dispatchers.Main.immediate` until [UiSubscription.cancel]. */
    public fun subscribe(onValue: (T) -> Unit): UiSubscription {
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            origin.collect { onValue(it) }
        }
        return UiSubscription { job.cancel() }
    }
}

/** Wraps the state flow so that Swift can observe it; see [UiStateFlow]. */
public fun <T> StateFlow<T>.asUiStateFlow(): UiStateFlow<T> = UiStateFlow(this)
