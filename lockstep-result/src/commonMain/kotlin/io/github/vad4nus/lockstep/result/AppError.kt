package io.github.vad4nus.lockstep.result

import dev.icerock.moko.resources.desc.StringDesc

/** One field-level problem a server reports in an error body. */
public data class ApiErrorDetail(val field: String?, val message: String)

/** Why a call failed. The texts a user sees come from the app's error presenter, not from here. */
public sealed interface AppError {
    /** The request did not reach the server: there is no network, or the connection was lost. */
    public data object NoConnection : AppError

    /** The server did not answer in time. */
    public data object Timeout : AppError

    /** HTTP 401. [message] is what the server said, when an error body parser found it. */
    public data class Unauthorized(val guestLoginRequired: Boolean = false, val message: String? = null) : AppError

    /** Any other non-2xx response. [message] and [details] come from the error body; both are empty without a parser. */
    public data class Http(val code: Int, val message: String?, val details: List<ApiErrorDetail> = emptyList()) : AppError

    /** The response could not be decoded. [message] is a diagnostic for logs, not a text for the user. */
    public data class Serialization(val message: String?) : AppError

    /** Anything else. [message] and [cause] are diagnostics for logs, not texts for the user. */
    public data class Unknown(val message: String?, val cause: Throwable? = null) : AppError

    /** An error of the app's own domain, told apart by [code]; [message] is shown to the user as it is. */
    public data class Custom(val code: String, val message: StringDesc? = null) : AppError
}

/** Turns a thrown exception into an [AppError]. */
public fun interface ErrorMapper {
    /** The error [throwable] stands for. */
    public fun map(throwable: Throwable): AppError

    public companion object {
        /** Maps every exception to [AppError.Unknown] and keeps the exception as its cause. */
        public val Default: ErrorMapper = ErrorMapper { AppError.Unknown(it.message, it) }
    }
}
