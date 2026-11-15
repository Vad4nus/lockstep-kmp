package io.github.vad4nus.lockstep.ktor

import io.github.vad4nus.lockstep.result.ApiErrorDetail
import io.github.vad4nus.lockstep.result.AppError
import io.github.vad4nus.lockstep.result.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.engine.mock.MockRequestHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@Serializable
data class OrderDto(val id: Int, val name: String)

class HttpCallsTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun client(retries: Int? = null, handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json() }
        if (retries != null) installLockstepDefaults(retries)
    }

    @Test
    fun okBodyIsDecoded() = runTest {
        val http = client { respond("""{"id":1,"name":"a"}""", HttpStatusCode.OK, jsonHeaders) }
        assertEquals(AppResult.Success(OrderDto(1, "a")), http.call<OrderDto> { url("https://x/orders/1") })
    }

    @Test
    fun malformedBodyIsSerializationFailure() = runTest {
        val http = client { respond("""{"id":"oops"}""", HttpStatusCode.OK, jsonHeaders) }
        val failure = assertIs<AppResult.Failure>(http.call<OrderDto> { url("https://x/orders/1") })
        assertIs<AppError.Serialization>(failure.error)
    }

    @Test
    fun unauthorizedIsMapped() = runTest {
        val http = client { respondError(HttpStatusCode.Unauthorized) }
        assertEquals(AppResult.Failure(AppError.Unauthorized()), http.call<OrderDto> { url("https://x/orders/1") })
    }

    @Test
    fun unauthorizedBodyIsParsed() = runTest {
        val http = client { respondError(HttpStatusCode.Unauthorized, "guest") }
        val guest = http.call<OrderDto>({ AppError.Unauthorized(guestLoginRequired = true, message = it.bodyAsText()) }) {
            url("https://x/orders")
        }
        assertEquals(AppResult.Failure(AppError.Unauthorized(guestLoginRequired = true, message = "guest")), guest)
        val generic = http.call<OrderDto>({ AppError.Http(it.status.value, it.bodyAsText()) }) { url("https://x/orders") }
        assertEquals(AppResult.Failure(AppError.Unauthorized(message = "guest")), generic)
    }

    @Test
    fun errorBodyIsParsedIntoHttpError() = runTest {
        val http = client { respondError(HttpStatusCode.UnprocessableEntity, "bad name") }
        val details = listOf(ApiErrorDetail("name", "empty"))
        val result = http.call<OrderDto>({ AppError.Http(it.status.value, it.bodyAsText(), details) }) {
            url("https://x/orders")
        }
        assertEquals(AppResult.Failure(AppError.Http(422, "bad name", details)), result)
    }

    @Test
    fun errorWithoutParserHasNoMessage() = runTest {
        val http = client { respondError(HttpStatusCode.NotFound) }
        val result = http.call<OrderDto> { url("https://x/orders") }
        assertEquals(AppResult.Failure(AppError.Http(404, null)), result)
    }

    @Test
    fun failingParserFallsBackToTheDefault() = runTest {
        val http = client { respondError(HttpStatusCode.BadGateway, "<html>") }
        val result = http.call<OrderDto>({ error("not json") }) { url("https://x/orders") }
        assertEquals(AppResult.Failure(AppError.Http(502, null)), result)
    }

    @Test
    fun emptyBodyDecodesOnlyAsUnit() = runTest {
        val http = client { respond("", HttpStatusCode.NoContent) }
        assertEquals(AppResult.Success(Unit), http.call<Unit> { url("https://x/orders/1") })
        val failure = assertIs<AppResult.Failure>(http.call<OrderDto> { url("https://x/orders/1") })
        assertIs<AppError.Serialization>(failure.error)
    }

    @Test
    fun ioFailureIsNoConnection() = runTest {
        val http = client { throw IOException("offline") }
        assertEquals(AppResult.Failure(AppError.NoConnection), http.call<OrderDto> { url("https://x/orders") })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelledCallThrowsCancellation() = runTest {
        val http = client { awaitCancellation() }
        val call = async { http.call<OrderDto> { url("https://x/orders") } }
        runCurrent()
        call.cancel()
        assertFailsWith<CancellationException> { call.await() }
    }

    @Test
    fun defaultsRetryServerErrors() = runTest {
        var calls = 0
        val http = client(retries = 2) {
            calls++
            if (calls <= 2) respondError(HttpStatusCode.ServiceUnavailable) else respond("""{"id":2,"name":"b"}""", HttpStatusCode.OK, jsonHeaders)
        }
        assertEquals(AppResult.Success(OrderDto(2, "b")), http.call<OrderDto> { url("https://x/orders/2") })
        assertEquals(3, calls)
    }

    @Test
    fun onlySafeMethodsAreRetried() = runTest {
        val calls = mutableMapOf<HttpMethod, Int>()
        val failing = client(retries = 2) {
            calls[it.method] = (calls[it.method] ?: 0) + 1
            respondError(HttpStatusCode.InternalServerError)
        }
        val offline = client(retries = 2) {
            calls[it.method] = (calls[it.method] ?: 0) + 1
            throw IOException("offline")
        }
        for (http in listOf(failing, offline)) {
            calls.clear()
            http.call<OrderDto> { url("https://x/orders") }
            http.call<OrderDto> { method = HttpMethod.Post; url("https://x/orders") }
            assertEquals(mapOf(HttpMethod.Get to 3, HttpMethod.Post to 1), calls)
        }
    }

    @Test
    fun timeoutsAreNotRetried() = runTest {
        var calls = 0
        val http = client(retries = 2) {
            calls++
            throw HttpRequestTimeoutException("https://x/orders", 1_000)
        }
        assertEquals(AppResult.Failure(AppError.Timeout), http.call<OrderDto> { url("https://x/orders") })
        assertEquals(1, calls)
    }
}
