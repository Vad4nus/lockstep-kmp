package io.github.vad4nus.lockstep.ktor

import io.github.vad4nus.lockstep.result.AppError
import kotlinx.io.IOException

private val urlErrorCode = Regex("""NSURLErrorDomain Code=(-?\d+)""")
private val connectivityCodes = setOf(-1003, -1004, -1005, -1006, -1009, -1018, -1019, -1020)
private const val TIMED_OUT = -1001

/**
 * The Darwin engine reports an `NSError` only inside the exception message, and this module does not depend on
 * that engine, so the `NSURLErrorDomain` code is read from the text. Without a code it is a lost connection.
 */
internal actual fun IOException.toAppError(): AppError {
    val code = message?.let { urlErrorCode.find(it) }?.groupValues?.get(1)?.toIntOrNull()
    return when (code) {
        null, in connectivityCodes -> AppError.NoConnection
        TIMED_OUT -> AppError.Timeout
        else -> AppError.Unknown(message, this)
    }
}
