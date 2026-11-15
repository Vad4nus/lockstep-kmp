package io.github.vad4nus.lockstep.mvvm.navigation

import io.github.vad4nus.lockstep.UiStateFlow
import io.github.vad4nus.lockstep.asUiStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** What is on screen: the [root], the screens pushed over it, and the open [sheet]. */
public data class NavStack(val root: Route, val path: List<Route>, val sheet: Route? = null) {
    init {
        require(root !is SheetRoute) { "A SheetRoute cannot be the root: $root" }
        require(path.none { it is SheetRoute }) { "A SheetRoute cannot be in the path: $path" }
    }
}

/**
 * The navigation state of one stack, shared by the ViewModels and drawn by both platform hosts.
 *
 * Use it from the main thread only.
 */
public class StackNavigator(root: Route) {
    private val mutableStack = MutableStateFlow(NavStack(root, emptyList()))
    private val collectors = mutableMapOf<ResultKey<*>, MutableList<Channel<Any>>>()
    private val pending = mutableMapOf<ResultKey<*>, Any>()

    /** What is on screen now; both platform hosts draw it. */
    public val stack: UiStateFlow<NavStack> = mutableStack.asUiStateFlow()

    /** Opens [route]: a [SheetRoute] replaces the open sheet, any other route goes on top of the path and closes the sheet. */
    public fun push(route: Route) {
        change { if (route is SheetRoute) it.copy(sheet = route) else it.copy(path = it.path + route, sheet = null) }
    }

    /** Closes the sheet, or else the top screen. Returns false when only the root is left. */
    public fun pop(): Boolean = change { current ->
        when {
            current.sheet != null -> current.copy(sheet = null)
            current.path.isNotEmpty() -> current.copy(path = current.path.dropLast(1))
            else -> current
        }
    }

    /**
     * Pops down to the last [route] in the stack, and pops that one too when [inclusive] is set.
     * Returns false and changes nothing when the route is not in the stack.
     */
    public fun popTo(route: Route, inclusive: Boolean = false): Boolean {
        var found = false
        change { current ->
            val index = current.path.lastIndexOf(route)
            found = index >= 0 || route == current.root
            when {
                index >= 0 -> current.copy(path = current.path.take(if (inclusive) index else index + 1), sheet = null)
                found -> current.copy(path = emptyList(), sheet = null)
                else -> current
            }
        }
        return found
    }

    /** Replaces the whole stack and closes the sheet. */
    public fun replaceAll(root: Route, path: List<Route> = emptyList()) {
        change { NavStack(root, path) }
    }

    /** Replaces the screens over the root and closes the sheet. */
    public fun setPath(path: List<Route>) {
        change { NavStack(it.root, path) }
    }

    /**
     * The platform's own back navigation left [newPath] on screen. It is applied only when [newPath] is a shorter
     * beginning of the current path, so a late report cannot undo a newer change.
     */
    public fun onSystemPop(newPath: List<Route>) {
        change { current ->
            val shorter = newPath.size < current.path.size && current.path.subList(0, newPath.size) == newPath
            if (shorter) current.copy(path = newPath) else current
        }
    }

    /** The platform closed the sheet [expected]. Ignored when another sheet is open by now. */
    public fun onSystemDismissSheet(expected: Route) {
        change { if (it.sheet == expected) it.copy(sheet = null) else it }
    }

    /** Pops like [pop] and then hands [value] to the collector of [results] for [key]. */
    public fun <R : Any> popWithResult(key: ResultKey<R>, value: R) {
        pop()
        val collector = collectors[key]?.lastOrNull()
        if (collector != null) collector.trySend(value) else pending[key] = value
    }

    /**
     * The results popped for [key]. Each result is received once, by the collector that started last, which in
     * a stack is the screen nearest to the one that returned it. A result nobody collects waits for a collector
     * until the next change of the stack and is dropped after it.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <R : Any> results(key: ResultKey<R>): Flow<R> = flow {
        val channel = Channel<Any>(Channel.UNLIMITED)
        pending.remove(key)?.let { channel.trySend(it) }
        val active = collectors.getOrPut(key) { mutableListOf() }
        active += channel
        try {
            for (value in channel) emit(value as R)
        } finally {
            active -= channel
        }
    }

    private inline fun change(transform: (NavStack) -> NavStack): Boolean {
        while (true) {
            val before = mutableStack.value
            val after = transform(before)
            if (after == before) return false
            if (mutableStack.compareAndSet(before, after)) {
                pending.clear()
                return true
            }
        }
    }
}
