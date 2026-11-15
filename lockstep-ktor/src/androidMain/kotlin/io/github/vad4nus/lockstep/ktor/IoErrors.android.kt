package io.github.vad4nus.lockstep.ktor

import io.github.vad4nus.lockstep.result.AppError
import kotlinx.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

internal actual fun IOException.toAppError(): AppError = when (this) {
    is SocketTimeoutException -> AppError.Timeout
    is SSLException -> AppError.Unknown(message, this)
    else -> AppError.NoConnection
}
