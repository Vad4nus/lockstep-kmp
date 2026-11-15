package io.github.vad4nus.lockstep.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import io.github.vad4nus.lockstep.result.AppError
import io.github.vad4nus.lockstep.result.AppResult
import kotlinx.coroutines.Job
import kotlin.reflect.KClass

data class TestState(val value: Int = 0)

sealed interface TestAction {
    data object Ping : TestAction
}

class TestViewModel(env: MvvmEnvironment = MvvmEnvironment.Default) :
    MvvmViewModel<TestState, TestAction>(TestState(), env) {
    var firstAppearCalls = 0
    var appearCalls = 0
    var disappearCalls = 0
    var clearedCalls = 0

    val current: TestState get() = currentState

    fun set(value: Int) = updateState { copy(value = value) }

    fun act(action: TestAction) = sendAction(action)

    fun <T> run(
        call: suspend () -> AppResult<T>,
        loading: LoadingKind? = LoadingKind.Blocking,
        key: Any? = null,
        policy: RequestPolicy = RequestPolicy.CancelPrevious,
        errors: ErrorDisplay = ErrorDisplay.Message,
        onError: ((AppError) -> Unit)? = null,
        onFinally: (() -> Unit)? = null,
        onSuccess: suspend (T) -> Unit = {},
    ): Job = request(call, loading, key, policy, errors, onError, onFinally, onSuccess)

    fun message(text: String, action: MessageAction? = null) =
        showMessage(StringDesc.Raw(text), MessageKind.Info, action)

    fun dialog(text: String, onResult: (DialogResult) -> Unit) = showDialog(StringDesc.Raw(text), onResult = onResult)

    fun fail(error: AppError, display: ErrorDisplay) = handleError(error, display)

    override fun onFirstAppear() {
        firstAppearCalls++
    }

    override fun onAppear() {
        appearCalls++
    }

    override fun onDisappear() {
        disappearCalls++
    }

    override fun onCleared() {
        super.onCleared()
        clearedCalls++
    }
}

@Suppress("UNCHECKED_CAST")
fun <VM : ViewModel> ViewModelStore.put(key: String, factory: () -> VM): VM {
    val provider = ViewModelProvider.create(
        this,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = factory() as T
        },
    )
    return provider.get(key, ViewModel::class) as VM
}
