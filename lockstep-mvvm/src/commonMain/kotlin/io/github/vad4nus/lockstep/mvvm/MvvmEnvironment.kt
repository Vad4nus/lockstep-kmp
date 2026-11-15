package io.github.vad4nus.lockstep.mvvm

import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import io.github.vad4nus.lockstep.result.AppError
import io.github.vad4nus.lockstep.result.ErrorMapper

/** The texts the base layer shows on its own. The defaults are English; pass string resources to localize them. */
public class MvvmTexts(
    public val ok: StringDesc = StringDesc.Raw("OK"),
    public val retry: StringDesc = StringDesc.Raw("Try again"),
    public val noConnection: StringDesc = StringDesc.Raw("No connection"),
    public val timeout: StringDesc = StringDesc.Raw("The request timed out"),
    public val unauthorized: StringDesc = StringDesc.Raw("Authorization required"),
    public val serverError: (code: Int) -> StringDesc = { StringDesc.Raw("Server error $it") },
    public val unknownError: StringDesc = StringDesc.Raw("Something went wrong"),
)

/** Turns an [AppError] into what the screen shows. */
public fun interface ErrorPresenter {
    /** What the screen shows for [error]. */
    public fun present(error: AppError): UiError

    public companion object {
        /**
         * Shows the server's own message of an HTTP error and the message of an [AppError.Custom]; every other text
         * comes from [texts]. The diagnostics of [AppError.Serialization] and [AppError.Unknown] are never shown.
         */
        public fun standard(texts: MvvmTexts = MvvmTexts()): ErrorPresenter = ErrorPresenter { error ->
            when (error) {
                AppError.NoConnection -> UiError(texts.noConnection, UiErrorKind.Network)
                AppError.Timeout -> UiError(texts.timeout, UiErrorKind.Network)
                is AppError.Unauthorized ->
                    UiError(error.message?.let { StringDesc.Raw(it) } ?: texts.unauthorized, UiErrorKind.Unauthorized)
                is AppError.Http ->
                    UiError(error.message?.let { StringDesc.Raw(it) } ?: texts.serverError(error.code), UiErrorKind.Server)
                is AppError.Serialization, is AppError.Unknown -> UiError(texts.unknownError, UiErrorKind.Unknown)
                is AppError.Custom -> UiError(error.message ?: texts.unknownError, UiErrorKind.Unknown)
            }
        }
    }
}

/**
 * App-wide rules of the base layer: how exceptions become errors, which texts it shows, how errors become texts,
 * and where every failed request is reported. The default [errorReporter] prints the diagnostics the user never sees.
 *
 * A ViewModel takes [Default] unless its constructor gets another environment, so replace [Default] once at startup,
 * before the first ViewModel is created.
 */
public class MvvmEnvironment(
    public val errorMapper: ErrorMapper = ErrorMapper.Default,
    public val texts: MvvmTexts = MvvmTexts(),
    public val errorPresenter: ErrorPresenter = ErrorPresenter.standard(texts),
    public val errorReporter: (AppError) -> Unit = ::printDiagnostics,
) {
    public companion object {
        /** The environment of every ViewModel that is not given one. */
        public var Default: MvvmEnvironment = MvvmEnvironment()
    }
}

private fun printDiagnostics(error: AppError) {
    when (error) {
        is AppError.Unknown -> error.cause?.printStackTrace() ?: println("lockstep: unknown error: ${error.message}")
        is AppError.Serialization -> println("lockstep: serialization error: ${error.message}")
        AppError.NoConnection, AppError.Timeout, is AppError.Unauthorized, is AppError.Http, is AppError.Custom -> Unit
    }
}
