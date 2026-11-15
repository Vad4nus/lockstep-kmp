package io.github.vad4nus.lockstep.ktor

import io.github.vad4nus.lockstep.result.AppError
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertEquals

class IoErrorsAndroidTest {

    @Test
    fun rejectedCertificateIsNotALostConnection() {
        val failure = SSLHandshakeException("Chain validation failed")
        assertEquals(AppError.Unknown("Chain validation failed", failure), KtorErrorMapper.map(failure))
    }

    @Test
    fun connectivityAndTimeoutAreToldApart() {
        assertEquals(AppError.NoConnection, KtorErrorMapper.map(UnknownHostException("api.example.com")))
        assertEquals(AppError.Timeout, KtorErrorMapper.map(SocketTimeoutException("timeout")))
    }
}
