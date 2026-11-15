package io.github.vad4nus.lockstep.mvvm.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data object Home : Route
private data object B : Route
private data object C : Route
private data object D : Route
private data class S(val id: Int) : SheetRoute

@OptIn(ExperimentalCoroutinesApi::class)
class StackNavigatorTest {

    private val s1 = S(1)
    private val s2 = S(2)

    private fun navigator() = StackNavigator(Home)

    private val StackNavigator.now: NavStack get() = stack.value

    @Test
    fun popOnEmptyPathReturnsFalse() {
        assertFalse(navigator().pop())
    }

    @Test
    fun pushAndPopMoveThePath() {
        val nav = navigator()
        nav.push(B)
        nav.push(C)
        assertEquals(listOf(B, C), nav.now.path)
        assertTrue(nav.pop())
        assertEquals(listOf(B), nav.now.path)
    }

    @Test
    fun popToKeepsTargetUnlessInclusive() {
        val nav = navigator()
        nav.setPath(listOf(B, C, D))
        assertTrue(nav.popTo(B))
        assertEquals(listOf(B), nav.now.path)
        assertTrue(nav.popTo(B))
        nav.setPath(listOf(B, C, D))
        assertTrue(nav.popTo(C, inclusive = true))
        assertEquals(listOf(B), nav.now.path)
    }

    @Test
    fun popToRootClearsThePathAndUnknownRouteIsIgnored() {
        val nav = navigator()
        nav.setPath(listOf(B, C))
        assertFalse(nav.popTo(D))
        assertEquals(listOf(B, C), nav.now.path)
        assertTrue(nav.popTo(Home))
        assertEquals(emptyList(), nav.now.path)
    }

    @Test
    fun onSystemPopAppliesOnlyAShorterBeginningOfThePath() {
        val nav = navigator()
        nav.setPath(listOf(B, C))
        nav.onSystemPop(listOf(B))
        assertEquals(listOf(B), nav.now.path)
        nav.onSystemPop(listOf(B, C, D))
        assertEquals(listOf(B), nav.now.path)
        nav.setPath(listOf(C, D))
        nav.onSystemPop(listOf(B))
        assertEquals(listOf(C, D), nav.now.path)
        nav.onSystemPop(emptyList())
        assertEquals(emptyList(), nav.now.path)
    }

    @Test
    fun replaceAllResetsEverything() {
        val nav = navigator()
        nav.push(B)
        nav.push(s1)
        nav.replaceAll(C, listOf(D))
        assertEquals(NavStack(C, listOf(D)), nav.now)
    }

    @Test
    fun resultIsReceivedOnceByLateCollector() = runTest {
        val nav = navigator()
        val key = ResultKey<String>("k")
        nav.push(B)
        nav.popWithResult(key, "done")
        assertEquals("done", nav.results(key).first())
        assertEquals(emptyList(), nav.now.path)
    }

    @Test
    fun uncollectedResultIsDroppedByTheNextStackChange() = runTest {
        val nav = navigator()
        val key = ResultKey<String>("k")
        nav.push(B)
        nav.popWithResult(key, "stale")
        nav.push(C)
        val received = mutableListOf<String>()
        backgroundScope.launch { nav.results(key).collect { received += it } }
        runCurrent()
        assertEquals(emptyList(), received)
    }

    @Test
    fun resultGoesToTheCollectorThatStartedLast() = runTest {
        val nav = navigator()
        val key = ResultKey<Int>("k")
        val first = mutableListOf<Int>()
        val second = mutableListOf<Int>()
        backgroundScope.launch { nav.results(key).collect { first += it } }
        runCurrent()
        val later = backgroundScope.launch { nav.results(key).collect { second += it } }
        runCurrent()
        repeat(4) { nav.popWithResult(key, it) }
        runCurrent()
        assertEquals(emptyList(), first)
        assertEquals(listOf(0, 1, 2, 3), second)
        later.cancel()
        runCurrent()
        nav.popWithResult(key, 9)
        runCurrent()
        assertEquals(listOf(9), first)
    }

    @Test
    fun keysWithTheSameNameDoNotShareResults() = runTest {
        val nav = navigator()
        val text = ResultKey<String>("same")
        val number = ResultKey<Int>("same")
        nav.popWithResult(number, 1)
        val received = mutableListOf<String>()
        backgroundScope.launch { nav.results(text).collect { received += it } }
        runCurrent()
        assertEquals(emptyList(), received)
        assertEquals(1, nav.results(number).first())
    }

    @Test
    fun popHappensBeforeTheResultIsDelivered() = runTest {
        val nav = navigator()
        val key = ResultKey<Int>("k")
        backgroundScope.launch(Dispatchers.Unconfined) { nav.results(key).collect { nav.push(D) } }
        nav.push(B)
        nav.popWithResult(key, 1)
        assertEquals(listOf(D), nav.now.path)
    }

    @Test
    fun pushSheetSetsSheetAndReplacesTheOpenOne() {
        val nav = navigator()
        nav.push(B)
        nav.push(s1)
        assertEquals(NavStack(Home, listOf(B), s1), nav.now)
        nav.push(s2)
        assertEquals(s2, nav.now.sheet)
        assertEquals(listOf(B), nav.now.path)
    }

    @Test
    fun pushingTheSameSheetEmitsNothingNew() = runTest {
        val nav = navigator()
        nav.push(s1)
        val before = nav.now
        nav.push(S(1))
        assertTrue(before === nav.now)
    }

    @Test
    fun pushingAScreenClosesTheSheetInOneUpdate() {
        val nav = navigator()
        nav.push(B)
        nav.push(s1)
        nav.push(C)
        assertEquals(NavStack(Home, listOf(B, C), null), nav.now)
    }

    @Test
    fun popClosesTheSheetFirst() {
        val nav = navigator()
        nav.push(B)
        nav.push(s1)
        assertTrue(nav.pop())
        assertNull(nav.now.sheet)
        assertEquals(listOf(B), nav.now.path)
    }

    @Test
    fun popWithResultFromSheetClosesItAndDeliversOnce() = runTest {
        val nav = navigator()
        val key = ResultKey<Int>("sheet")
        nav.push(s1)
        nav.popWithResult(key, 42)
        assertNull(nav.now.sheet)
        assertEquals(42, nav.results(key).first())
    }

    @Test
    fun popToSetPathAndReplaceAllCloseTheSheet() {
        val nav = navigator()
        nav.setPath(listOf(B, C))
        nav.push(s1)
        nav.popTo(B)
        assertNull(nav.now.sheet)
        nav.push(s1)
        nav.setPath(listOf(B))
        assertNull(nav.now.sheet)
        nav.push(s1)
        nav.replaceAll(Home)
        assertNull(nav.now.sheet)
    }

    @Test
    fun systemDismissClearsOnlyTheExpectedSheet() {
        val nav = navigator()
        nav.push(s1)
        nav.onSystemDismissSheet(s1)
        assertNull(nav.now.sheet)
    }

    @Test
    fun lateDismissAfterScreenPushKeepsThePath() {
        val nav = navigator()
        nav.push(s1)
        nav.push(C)
        nav.onSystemDismissSheet(s1)
        assertEquals(listOf(C), nav.now.path)
    }

    @Test
    fun lateDismissAfterSheetReplacementKeepsTheNewSheet() {
        val nav = navigator()
        nav.push(s1)
        nav.push(s2)
        nav.onSystemDismissSheet(s1)
        assertEquals(s2, nav.now.sheet)
    }

    @Test
    fun sheetRoutesAreRejectedInRootAndPath() {
        assertFailsWith<IllegalArgumentException> { StackNavigator(s1) }
        assertFailsWith<IllegalArgumentException> { navigator().setPath(listOf(s1)) }
        assertFailsWith<IllegalArgumentException> { navigator().replaceAll(Home, listOf(s1)) }
    }
}
