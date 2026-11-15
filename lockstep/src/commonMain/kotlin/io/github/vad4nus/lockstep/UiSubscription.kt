package io.github.vad4nus.lockstep

/** A running subscription of [UiStateFlow.subscribe] or [UiFlow.subscribe]. */
public fun interface UiSubscription {
    /** Stops the delivery; safe to call from any thread and more than once. */
    public fun cancel()
}
