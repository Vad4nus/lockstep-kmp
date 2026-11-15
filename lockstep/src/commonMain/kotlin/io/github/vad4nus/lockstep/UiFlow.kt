package io.github.vad4nus.lockstep

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * A [Flow] that Swift can subscribe to; the Lockstep plugin exposes it on iOS as an `AsyncSequence` of native values.
 *
 * A flow backed by a channel (`receiveAsFlow`) hands each element to exactly one collector, so collect it in one place.
 */
public class UiFlow<T>(private val origin: Flow<T>) : Flow<T> by origin {

    /**
     * Collects on `Dispatchers.Main.immediate` until [UiSubscription.cancel].
     *
     * [onComplete] is called once: with `null` when the flow ends or is cancelled upstream, with the failure when it throws.
     * It is not called after [UiSubscription.cancel].
     */
    public fun subscribe(onValue: (T) -> Unit, onComplete: (Throwable?) -> Unit): UiSubscription {
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            val failure = try {
                origin.collect { onValue(it) }
                null
            } catch (cancellation: CancellationException) {
                ensureActive()
                null
            } catch (failure: Throwable) {
                failure
            }
            onComplete(failure)
        }
        return UiSubscription { job.cancel() }
    }
}

/** Wraps the flow so that Swift can subscribe to it; see [UiFlow]. */
public fun <T> Flow<T>.asUiFlow(): UiFlow<T> = UiFlow(this)
