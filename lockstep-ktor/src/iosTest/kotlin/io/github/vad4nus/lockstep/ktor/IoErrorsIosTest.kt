package io.github.vad4nus.lockstep.ktor

import io.github.vad4nus.lockstep.result.AppError
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class IoErrorsIosTest {

    private fun darwinFailure(code: Int) =
        IOException("Exception in http request: Error Domain=NSURLErrorDomain Code=$code \"description\" UserInfo={}")

    @Test
    fun rejectedCertificateIsNotALostConnection() {
        val failure = darwinFailure(-1202)
        assertEquals(AppError.Unknown(failure.message, failure), KtorErrorMapper.map(failure))
    }

    @Test
    fun connectivityAndTimeoutAreToldApart() {
        assertEquals(AppError.NoConnection, KtorErrorMapper.map(darwinFailure(-1009)))
        assertEquals(AppError.NoConnection, KtorErrorMapper.map(darwinFailure(-1005)))
        assertEquals(AppError.Timeout, KtorErrorMapper.map(darwinFailure(-1001)))
        assertEquals(AppError.NoConnection, KtorErrorMapper.map(IOException("offline")))
    }
}
