package io.github.vad4nus.lockstep.result

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppResultTest {

    private val boom = AppError.Http(500, "boom")
    private val ok: AppResult<Int> = AppResult.Success(1)
    private val bad: AppResult<Int> = AppResult.Failure(boom)

    @Test
    fun mapTransformsSuccessAndKeepsFailure() {
        assertEquals(AppResult.Success(2), ok.map { it + 1 })
        assertEquals(bad, bad.map { it + 1 })
    }

    @Test
    fun mapErrorTransformsFailureOnly() {
        assertEquals(AppResult.Failure(AppError.Timeout), bad.mapError { AppError.Timeout })
        assertEquals(ok, ok.mapError { AppError.Timeout })
    }

    @Test
    fun flatMapChainsAndShortCircuits() {
        assertEquals(AppResult.Success("1"), ok.flatMap { AppResult.Success(it.toString()) })
        assertEquals(bad, ok.flatMap { bad })
        assertEquals(bad, bad.flatMap { AppResult.Success(it) })
    }

    @Test
    fun foldPicksBranch() {
        assertEquals("s1", ok.fold({ "s$it" }, { "f" }))
        assertEquals("f", bad.fold({ "s$it" }, { "f" }))
    }

    @Test
    fun recoverTurnsFailureIntoSuccess() {
        assertEquals(AppResult.Success(0), bad.recover { 0 })
        assertEquals(ok, ok.recover { 0 })
    }

    @Test
    fun onSuccessAndOnFailureRunOnlyTheirBranch() {
        val seen = mutableListOf<String>()
        ok.onSuccess { seen += "s" }.onFailure { seen += "f" }
        bad.onSuccess { seen += "s2" }.onFailure { seen += "f2" }
        assertEquals(listOf("s", "f2"), seen)
    }

    @Test
    fun getOrNullAndGetOrElse() {
        assertEquals(1, ok.getOrNull())
        assertNull(bad.getOrNull())
        assertEquals(1, ok.getOrElse { 9 })
        assertEquals(9, bad.getOrElse { 9 })
    }

    @Test
    fun ensureNotNullFailsOnNull() {
        val some: AppResult<String?> = AppResult.Success("x")
        val none: AppResult<String?> = AppResult.Success(null)
        assertEquals(AppResult.Success("x"), some.ensureNotNull())
        assertIs<AppResult.Failure>(none.ensureNotNull())
        assertEquals(AppResult.Failure(AppError.Timeout), none.ensureNotNull { AppError.Timeout })
    }

    @Test
    fun zipCombinesOrFails() {
        assertEquals(AppResult.Success(1 to 2), AppResult.zip(ok, AppResult.Success(2)))
        assertEquals(AppResult.Failure(boom), AppResult.zip(AppResult.Success(1), AppResult.Failure(boom)))
    }

    @Test
    fun combineCollectsOrReturnsFirstFailure() {
        assertEquals(AppResult.Success(listOf(1, 2)), AppResult.combine(listOf(AppResult.Success(1), AppResult.Success(2))))
        assertEquals(AppResult.Failure(boom), AppResult.combine(listOf(AppResult.Success(1), bad, AppResult.Failure(AppError.Timeout))))
    }

    @Test
    fun catchingWrapsSuccess() = runTest {
        assertEquals(AppResult.Success(3), catching { 3 })
    }

    @Test
    fun catchingMapsThrowableToUnknown() = runTest {
        val result = catching<Int> { throw IllegalStateException("bad") }
        val failure = assertIs<AppResult.Failure>(result)
        val error = assertIs<AppError.Unknown>(failure.error)
        assertEquals("bad", error.message)
        assertTrue(error.cause is IllegalStateException)
    }

    @Test
    fun catchingUsesGivenMapper() = runTest {
        val result = catching(mapper = { AppError.Timeout }) { error("x") }
        assertEquals(AppResult.Failure(AppError.Timeout), result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun catchingRethrowsCancellation() = runTest {
        val gate = CompletableDeferred<Unit>()
        var outcome: Any? = null
        val job = launch {
            try {
                outcome = catching { gate.await() }
            } catch (cancellation: CancellationException) {
                outcome = cancellation
                throw cancellation
            }
        }
        testScheduler.advanceUntilIdle()
        job.cancel()
        job.join()
        assertIs<CancellationException>(outcome)
    }

    @Test
    fun catchingDirectCancellationEscapes() = runTest {
        assertFailsWith<CancellationException> { catching<Int> { throw CancellationException("c") } }
    }

    @Test
    fun catchingLetsErrorsThrough() = runTest {
        assertFailsWith<NotImplementedError> { catching<Int> { TODO() } }
    }
}
