package io.github.vad4nus.lockstep.mvvm

import androidx.lifecycle.ViewModelStore
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import io.github.vad4nus.lockstep.result.AppError
import io.github.vad4nus.lockstep.result.AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MvvmViewModelTest {

    private val failure = AppError.Http(500, "boom")
    private val reported = mutableListOf<AppError>()
    private val env = MvvmEnvironment(errorReporter = { reported += it })
    private val unknownText = StringDesc.Raw("Something went wrong")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.finish() = advanceUntilIdle()

    @Test
    fun updateStateIsVisibleImmediately() {
        val vm = TestViewModel()
        vm.set(5)
        assertEquals(5, vm.current.value)
        assertEquals(5, vm.state.value.value)
    }

    @Test
    fun cancelPreviousRunsOnlyTheSecondOnSuccess() = runTest {
        val vm = TestViewModel()
        val seen = mutableListOf<Int>()
        vm.run({ delay(100); AppResult.Success(1) }, key = "k") { seen += it }
        vm.run({ delay(100); AppResult.Success(2) }, key = "k") { seen += it }
        finish()
        assertEquals(listOf(2), seen)
    }

    @Test
    fun keepRunningIgnoresTheSecondCall() = runTest {
        val vm = TestViewModel()
        val seen = mutableListOf<Int>()
        vm.run({ delay(100); AppResult.Success(1) }, key = "k", policy = RequestPolicy.KeepRunning) { seen += it }
        vm.run({ delay(100); AppResult.Success(2) }, key = "k", policy = RequestPolicy.KeepRunning) { seen += it }
        finish()
        assertEquals(listOf(1), seen)
    }

    @Test
    fun parallelRequestsKeepLoadingUntilBothFinish() = runTest {
        val vm = TestViewModel()
        vm.run({ delay(50); AppResult.Success(1) })
        vm.run({ delay(150); AppResult.Success(2) })
        runCurrent()
        assertEquals(LoadingKind.Blocking, vm.chrome.value.loading)
        advanceTimeBy(60)
        runCurrent()
        assertEquals(LoadingKind.Blocking, vm.chrome.value.loading)
        finish()
        assertNull(vm.chrome.value.loading)
    }

    @Test
    fun backgroundLoadingHasItsOwnFlag() = runTest {
        val vm = TestViewModel()
        vm.run({ delay(50); AppResult.Success(1) }, loading = LoadingKind.Background)
        runCurrent()
        assertNull(vm.chrome.value.loading)
        assertTrue(vm.chrome.value.backgroundLoading)
        finish()
        assertEquals(false, vm.chrome.value.backgroundLoading)
    }

    @Test
    fun blockingLoadingWinsOverRefresh() = runTest {
        val vm = TestViewModel()
        vm.run({ delay(100); AppResult.Success(1) }, loading = LoadingKind.Refresh)
        runCurrent()
        assertEquals(LoadingKind.Refresh, vm.chrome.value.loading)
        vm.run({ delay(50); AppResult.Success(2) })
        runCurrent()
        assertEquals(LoadingKind.Blocking, vm.chrome.value.loading)
        advanceTimeBy(60)
        runCurrent()
        assertEquals(LoadingKind.Refresh, vm.chrome.value.loading)
        finish()
        assertNull(vm.chrome.value.loading)
    }

    @Test
    fun messageErrorDisplayAppendsOneMessageAndShownRemovesIt() = runTest {
        val vm = TestViewModel()
        vm.run<Int>({ AppResult.Failure(failure) })
        finish()
        val messages = vm.chrome.value.messages
        assertEquals(1, messages.size)
        assertEquals(MessageKind.Error, messages.single().kind)
        assertEquals(StringDesc.Raw("boom"), messages.single().text)
        vm.messageShown(messages.single().id)
        assertEquals(emptyList(), vm.chrome.value.messages)
    }

    @Test
    fun messageActionClickedRunsOnClickOnce() {
        val vm = TestViewModel()
        var clicks = 0
        vm.message("saved", MessageAction(StringDesc.Raw("Undo")) { clicks++ })
        val id = vm.chrome.value.messages.single().id
        vm.messageActionClicked(id)
        vm.messageActionClicked(id)
        assertEquals(1, clicks)
        assertEquals(emptyList(), vm.chrome.value.messages)
    }

    @Test
    fun messageWithAnActionStaysLonger() {
        val vm = TestViewModel()
        vm.message("plain")
        vm.message("undo", MessageAction(StringDesc.Raw("Undo")) {})
        assertEquals(listOf(4_000L, 8_000L), vm.chrome.value.messages.map { it.durationMillis })
    }

    @Test
    fun screenErrorIsRetryableAndRetryRerunsTheCall() = runTest {
        val vm = TestViewModel()
        var calls = 0
        val seen = mutableListOf<Int>()
        vm.run(
            call = { calls++; if (calls == 1) AppResult.Failure(failure) else AppResult.Success(7) },
            errors = ErrorDisplay.Screen,
        ) { seen += it }
        finish()
        assertEquals(
            UiError(StringDesc.Raw("boom"), UiErrorKind.Server, StringDesc.Raw("Try again")),
            vm.chrome.value.error,
        )
        vm.retry()
        finish()
        assertEquals(2, calls)
        assertEquals(listOf(7), seen)
        assertEquals(ScreenChrome(), vm.chrome.value)
    }

    @Test
    fun screenErrorAndLoadingDoNotOverwriteEachOther() = runTest {
        val vm = TestViewModel()
        vm.run({ delay(100); AppResult.Success(1) })
        vm.run<Int>({ delay(10); AppResult.Failure(failure) }, errors = ErrorDisplay.Screen)
        advanceTimeBy(50)
        runCurrent()
        assertEquals(LoadingKind.Blocking, vm.chrome.value.loading)
        assertNotNull(vm.chrome.value.error)
        finish()
        assertNull(vm.chrome.value.loading)
        assertNotNull(vm.chrome.value.error)
        vm.run({ AppResult.Success(1) }, key = "other")
        finish()
        assertNotNull(vm.chrome.value.error)
    }

    @Test
    fun retryClearsTheScreenErrorWhateverTheLoader() = runTest {
        for (kind in listOf(LoadingKind.Background, LoadingKind.Refresh, null)) {
            val vm = TestViewModel()
            var calls = 0
            vm.run(
                call = { if (calls++ == 0) AppResult.Failure(failure) else AppResult.Success(1) },
                loading = kind,
                errors = ErrorDisplay.Screen,
            )
            finish()
            assertNotNull(vm.chrome.value.error)
            vm.retry()
            finish()
            assertEquals(ScreenChrome(), vm.chrome.value)
        }
    }

    @Test
    fun retryRerunsEveryFailedScreenRequest() = runTest {
        val vm = TestViewModel()
        val calls = mutableListOf<String>()
        vm.run<Int>({ calls += "a"; AppResult.Failure(failure) }, key = "a", errors = ErrorDisplay.Screen)
        finish()
        vm.run<Int>({ calls += "b"; AppResult.Failure(failure) }, key = "b", errors = ErrorDisplay.Screen)
        vm.run<Int>({ calls += "c"; AppResult.Failure(failure) }, errors = ErrorDisplay.Screen)
        vm.run<Int>({ calls += "d"; AppResult.Failure(failure) }, errors = ErrorDisplay.Screen)
        finish()
        calls.clear()
        vm.retry()
        finish()
        assertEquals(listOf("a", "b", "c", "d"), calls)
        assertNotNull(vm.chrome.value.error)
    }

    @Test
    fun startingTheRequestAgainClearsItsScreenError() = runTest {
        for (key in listOf("load", null)) {
            val vm = TestViewModel()
            var calls = 0
            val load = {
                vm.run(
                    call = { delay(10); if (calls++ == 0) AppResult.Failure(failure) else AppResult.Success(1) },
                    key = key,
                    errors = ErrorDisplay.Screen,
                )
            }
            load()
            finish()
            assertNotNull(vm.chrome.value.error)
            load()
            runCurrent()
            assertEquals(ScreenChrome(loading = LoadingKind.Blocking), vm.chrome.value)
            finish()
            assertEquals(ScreenChrome(), vm.chrome.value)
        }
    }

    @Test
    fun screenErrorShownByHandIsNotRetryable() {
        val vm = TestViewModel()
        vm.fail(failure, ErrorDisplay.Screen)
        assertEquals(UiError(StringDesc.Raw("boom"), UiErrorKind.Server), vm.chrome.value.error)
        vm.retry()
        assertNull(vm.chrome.value.error)
    }

    @Test
    fun onErrorReplacesDefaultHandling() = runTest {
        val vm = TestViewModel()
        var received: AppError? = null
        vm.run<Int>({ AppResult.Failure(failure) }, onError = { received = it })
        finish()
        assertEquals(failure, received)
        assertEquals(emptyList(), vm.chrome.value.messages)
    }

    @Test
    fun thrownExceptionIsReportedAndItsTextIsNotShown() = runTest {
        val vm = TestViewModel(env)
        vm.run<Int>({ error("thrown") })
        finish()
        assertEquals("thrown", assertIs<AppError.Unknown>(reported.single()).message)
        assertEquals(unknownText, vm.chrome.value.messages.single().text)
    }

    @Test
    fun exceptionInOnSuccessIsReportedAndShownAsAFailure() = runTest {
        val vm = TestViewModel(env)
        var finished = 0
        vm.run({ AppResult.Success(1) }, onFinally = { finished++ }) { error("bug in onSuccess") }
        finish()
        assertEquals(1, finished)
        assertEquals("bug in onSuccess", assertIs<AppError.Unknown>(reported.single()).message)
        assertEquals(unknownText, vm.chrome.value.messages.single().text)
        assertNull(vm.chrome.value.loading)
    }

    @Test
    fun exceptionInOnErrorIsReportedAndShown() = runTest {
        val vm = TestViewModel(env)
        vm.run<Int>({ AppResult.Failure(failure) }, onError = { error("bug in onError") })
        finish()
        assertEquals(listOf(failure, "bug in onError"), reported.map { (it as? AppError.Unknown)?.message ?: it })
        assertEquals(unknownText, vm.chrome.value.messages.single().text)
    }

    @Test
    fun onFinallyRunsForACancelledRequest() = runTest {
        val vm = TestViewModel()
        var finished = 0
        vm.run({ delay(100); AppResult.Success(1) }, key = "k", onFinally = { finished++ })
        runCurrent()
        vm.run({ AppResult.Success(2) }, key = "k", onFinally = { finished++ })
        finish()
        assertEquals(2, finished)
    }

    @Test
    fun everyActionSentBeforeCollectionIsDelivered() = runTest {
        val vm = TestViewModel()
        repeat(200) { vm.act(TestAction.Ping) }
        assertEquals(200, vm.actions.take(200).toList().size)
    }

    @Test
    fun textsComeFromTheEnvironment() = runTest {
        val texts = MvvmTexts(
            ok = StringDesc.Raw("Fine"),
            retry = StringDesc.Raw("Once more"),
            noConnection = StringDesc.Raw("Offline"),
        )
        val vm = TestViewModel(MvvmEnvironment(texts = texts))
        vm.run<Int>({ AppResult.Failure(AppError.NoConnection) }, errors = ErrorDisplay.Screen)
        finish()
        assertEquals(
            UiError(StringDesc.Raw("Offline"), UiErrorKind.Network, StringDesc.Raw("Once more")),
            vm.chrome.value.error,
        )
        vm.dialog("sure?") {}
        assertEquals(StringDesc.Raw("Fine"), vm.chrome.value.dialog!!.confirm)
    }

    @Test
    fun standardPresenterShowsServerTextsAndHidesDiagnostics() {
        val presenter = ErrorPresenter.standard()
        val shown = listOf(
            AppError.Http(502, null),
            AppError.Http(429, "Slow down"),
            AppError.Unauthorized(message = "Session expired"),
            AppError.Unauthorized(),
            AppError.Serialization("Unexpected JSON token at offset 3"),
            AppError.Unknown("NullPointerException"),
            AppError.Custom("limit", StringDesc.Raw("Limit reached")),
            AppError.Custom("limit"),
        ).map { presenter.present(it).message }
        assertEquals(
            listOf("Server error 502", "Slow down", "Session expired", "Authorization required")
                .map { StringDesc.Raw(it) } + listOf(unknownText, unknownText, StringDesc.Raw("Limit reached"), unknownText),
            shown,
        )
    }

    @Test
    fun dialogResultRunsCallbackAndClearsDialog() {
        val vm = TestViewModel()
        val results = mutableListOf<DialogResult>()
        vm.dialog("sure?") { results += it }
        val id = vm.chrome.value.dialog!!.id
        vm.dialogResult(id, DialogResult.Confirm)
        vm.dialogResult(id, DialogResult.Dismiss)
        assertEquals(listOf(DialogResult.Confirm), results)
        assertNull(vm.chrome.value.dialog)
    }

    @Test
    fun replacedDialogGetsDismissAndItsLateResultIsIgnored() {
        val vm = TestViewModel()
        val first = mutableListOf<DialogResult>()
        val second = mutableListOf<DialogResult>()
        vm.dialog("first") { first += it }
        val firstId = vm.chrome.value.dialog!!.id
        vm.dialog("second") { second += it }
        assertEquals(listOf(DialogResult.Dismiss), first)
        vm.dialogResult(firstId, DialogResult.Confirm)
        assertEquals(emptyList(), second)
        vm.dialogResult(vm.chrome.value.dialog!!.id, DialogResult.Confirm)
        assertEquals(listOf(DialogResult.Dismiss), first)
        assertEquals(listOf(DialogResult.Confirm), second)
    }

    @Test
    fun dialogOpenedFromACallbackStaysOpen() {
        val vm = TestViewModel()
        vm.dialog("first") { vm.dialog("second") {} }
        val firstId = vm.chrome.value.dialog!!.id
        vm.dialogResult(firstId, DialogResult.Confirm)
        val second = assertNotNull(vm.chrome.value.dialog)
        vm.dialogResult(firstId, DialogResult.Dismiss)
        assertEquals(second, vm.chrome.value.dialog)
    }

    @Test
    fun clearingTheStoreCancelsRequestsAndRunsOnCleared() = runTest {
        val store = ViewModelStore()
        val vm = store.put("vm") { TestViewModel() }
        val gate = CompletableDeferred<AppResult<Int>>()
        var success = 0
        var error = 0
        vm.run({ gate.await() }, onError = { error++ }) { success++ }
        runCurrent()
        assertEquals(LoadingKind.Blocking, vm.chrome.value.loading)
        store.clear()
        finish()
        assertEquals(0, success)
        assertEquals(0, error)
        assertNull(vm.chrome.value.loading)
        assertEquals(1, vm.clearedCalls)
    }

    @Test
    fun firstAppearRunsOncePerInstance() {
        val vm = TestViewModel()
        vm.screenAppeared()
        vm.screenAppeared()
        vm.screenDisappeared()
        assertEquals(1, vm.firstAppearCalls)
        assertEquals(2, vm.appearCalls)
        assertEquals(1, vm.disappearCalls)
    }
}
