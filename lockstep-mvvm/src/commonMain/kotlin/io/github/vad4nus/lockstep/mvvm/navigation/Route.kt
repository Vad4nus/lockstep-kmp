package io.github.vad4nus.lockstep.mvvm.navigation

/**
 * A destination in a [StackNavigator]. Declare routes as `data class` or `data object`: the navigator and both
 * platform hosts compare them by value, and Android derives the saved-state key of a screen from the route's
 * `toString()`. A route may be in the stack more than once: every occurrence is a screen of its own, with its own
 * saved state and ViewModel.
 */
public interface Route

/** A route shown as a bottom sheet over the stack. One sheet is open at a time. */
public interface SheetRoute : Route

/**
 * Names a kind of result a screen hands back with [StackNavigator.popWithResult].
 *
 * A key is matched by identity: declare it once, as a top-level `val`, and use that instance on both sides.
 * [name] only describes it in logs.
 */
public class ResultKey<R : Any>(public val name: String) {
    override fun toString(): String = "ResultKey($name)"
}
