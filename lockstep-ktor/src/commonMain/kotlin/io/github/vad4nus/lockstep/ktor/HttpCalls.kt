package io.github.vad4nus.lockstep.ktor

import io.github.vad4nus.lockstep.result.AppError
import io.github.vad4nus.lockstep.result.AppResult
import io.github.vad4nus.lockstep.result.ErrorMapper
import io.github.vad4nus.lockstep.result.catching
import io.github.vad4nus.lockstep.result.flatMap
import io.github.vad4nus.lockstep.result.getOrNull
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException

/**
 * Turns a non-2xx response into the app's error, usually by reading the server's message out of the body.
 *
 * Return null, or throw, to get the default: [AppError.Unauthorized] for 401 and [AppError.Http] without a message
 * for the rest. A 401 always comes out as [AppError.Unauthorized]: an [AppError.Http] returned for it keeps only
 * its message.
 */
public fun interface ErrorBodyParser {
    /** The error for [response], or null for the default one. */
    public suspend fun parse(response: HttpResponse): AppError?

    public companion object {
        /** Reads nothing: every non-2xx response gets the default error. */
        public val None: ErrorBodyParser = ErrorBodyParser { null }
    }
}

/**
 * Maps what Ktor throws: timeouts to [AppError.Timeout], decoding problems to [AppError.Serialization] and lost
 * connections to [AppError.NoConnection]. A failure that is not about connectivity, such as a rejected
 * certificate, is an [AppError.Unknown] with the cause, so that it reaches the logs instead of being shown as
 * a lost connection.
 */
public object KtorErrorMapper : ErrorMapper {
    override fun map(throwable: Throwable): AppError = when (throwable) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> AppError.Timeout
        is IOException -> throwable.toAppError()
        is ContentConvertException, is NoTransformationFoundException -> AppError.Serialization(throwable.message)
        else -> AppError.Unknown(throwable.message, throwable)
    }
}

internal expect fun IOException.toAppError(): AppError

/**
 * Runs the request and returns its body, or the reason it failed. A non-2xx response goes through [parser], and
 * what Ktor throws goes through [KtorErrorMapper]. Cancellation is rethrown.
 *
 * A response without a body decodes only as `Unit`; for any other [T] it is an [AppError.Serialization].
 */
public suspend inline fun <reified T> HttpClient.call(
    parser: ErrorBodyParser = ErrorBodyParser.None,
    noinline block: HttpRequestBuilder.() -> Unit = {},
): AppResult<T> = executeCall(parser, block) { it.body<T>() }

@PublishedApi
internal suspend fun <T> HttpClient.executeCall(
    parser: ErrorBodyParser,
    block: HttpRequestBuilder.() -> Unit,
    read: suspend (HttpResponse) -> T,
): AppResult<T> = catching(KtorErrorMapper) {
    val response = try {
        request(block)
    } catch (failure: ResponseException) {
        failure.response
    }
    if (response.status.isSuccess()) AppResult.Success(read(response)) else AppResult.Failure(response.toAppError(parser))
}.flatMap { it }

private suspend fun HttpResponse.toAppError(parser: ErrorBodyParser): AppError {
    val parsed = catching { parser.parse(this) }.getOrNull()
    val unauthorized = status == HttpStatusCode.Unauthorized
    return when {
        unauthorized && parsed is AppError.Http -> AppError.Unauthorized(message = parsed.message)
        parsed != null -> parsed
        unauthorized -> AppError.Unauthorized()
        else -> AppError.Http(status.value, null)
    }
}

/**
 * Installs [HttpRequestRetry]: a server error or a lost connection is retried up to [retries] times with a growing
 * delay, but only for [retryMethods]. The default is the methods that change nothing on the server; a repeated
 * `POST` could create a second order. Timeouts are not retried.
 */
public fun HttpClientConfig<*>.installLockstepDefaults(
    retries: Int = 2,
    retryMethods: Set<HttpMethod> = setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options),
) {
    install(HttpRequestRetry) {
        retryIf(retries) { request, response -> request.method in retryMethods && response.status.value in 500..599 }
        retryOnExceptionIf(retries) { request, cause ->
            request.method in retryMethods && KtorErrorMapper.map(cause) == AppError.NoConnection
        }
        delayMillis { retry -> retry * 200L }
    }
}
