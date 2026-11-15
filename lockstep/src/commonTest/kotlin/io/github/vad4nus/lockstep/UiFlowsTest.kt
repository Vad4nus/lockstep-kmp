package io.github.vad4nus.lockstep

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UiFlowsTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun valueEqualsSourceValue() {
        val source = MutableStateFlow("a")
        val ui = source.asUiStateFlow()
        assertEquals("a", ui.value)
        source.value = "b"
        assertEquals("b", ui.value)
    }

    @Test
    fun subscribeReceivesInitialValueAndUpdatesInOrder() {
        val source = MutableStateFlow(1)
        val received = mutableListOf<Int>()
        source.asUiStateFlow().subscribe { received += it }
        source.value = 2
        source.value = 3
        assertEquals(listOf(1, 2, 3), received)
    }

    @Test
    fun cancelStopsDelivery() {
        val source = MutableStateFlow(1)
        val received = mutableListOf<Int>()
        val subscription = source.asUiStateFlow().subscribe { received += it }
        source.value = 2
        subscription.cancel()
        source.value = 3
        assertEquals(listOf(1, 2), received)
    }

    @Test
    fun uiFlowReceivesEveryElementAndCompletes() {
        val channel = Channel<Int>(Channel.BUFFERED)
        val received = mutableListOf<Int>()
        val completions = mutableListOf<Throwable?>()
        channel.receiveAsFlow().asUiFlow().subscribe({ received += it }, { completions += it })
        channel.trySend(1)
        channel.trySend(2)
        channel.close()
        assertEquals(listOf(1, 2), received)
        assertEquals(1, completions.size)
        assertNull(completions.single())

        val finite = mutableListOf<Int>()
        val finiteCompletions = mutableListOf<Throwable?>()
        flowOf(7, 8).asUiFlow().subscribe({ finite += it }, { finiteCompletions += it })
        assertEquals(listOf(7, 8), finite)
        assertEquals(listOf<Throwable?>(null), finiteCompletions)
    }

    @Test
    fun uiFlowReportsFailureAndNotCancellation() {
        val failure = IllegalStateException("boom")
        val completions = mutableListOf<Throwable?>()
        flow<Int> { throw failure }.asUiFlow().subscribe({}, { completions += it })
        assertEquals(1, completions.size)
        assertSame(failure, completions.single())

        val channel = Channel<Int>(Channel.BUFFERED)
        val cancelled = mutableListOf<Throwable?>()
        val subscription = channel.receiveAsFlow().asUiFlow().subscribe({}, { cancelled += it })
        subscription.cancel()
        assertTrue(cancelled.isEmpty())
    }

    @Test
    fun upstreamCancellationCompletesTheSubscription() {
        val thrown = mutableListOf<Throwable?>()
        flow<Int> { throw CancellationException("upstream") }.asUiFlow().subscribe({}, { thrown += it })
        assertEquals(listOf<Throwable?>(null), thrown)

        val channel = Channel<Int>(Channel.BUFFERED)
        val closed = mutableListOf<Throwable?>()
        channel.receiveAsFlow().asUiFlow().subscribe({}, { closed += it })
        channel.cancel()
        assertEquals(listOf<Throwable?>(null), closed)

        val timedOut = mutableListOf<Throwable?>()
        flow<Int> { withTimeout(100) { awaitCancellation() } }.asUiFlow().subscribe({}, { timedOut += it })
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf<Throwable?>(null), timedOut)
    }

    @Test
    fun uiStateFlowIsUsableAsStateFlow() {
        val ui = MutableStateFlow(5).asUiStateFlow()
        val asStateFlow: StateFlow<Int> = ui
        assertEquals(5, asStateFlow.value)
    }
}
