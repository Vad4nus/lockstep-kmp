package io.github.vad4nus.lockstep.result

import kotlin.coroutines.cancellation.CancellationException

/** The outcome of a call: a value or an [AppError]. */
public sealed interface AppResult<out T> {
    /** The call returned [value]. */
    public data class Success<out T>(val value: T) : AppResult<T>

    /** The call failed with [error]. */
    public data class Failure(val error: AppError) : AppResult<Nothing>

    public companion object {
        /** Both values, or the first failure. */
        public fun <A, B> zip(first: AppResult<A>, second: AppResult<B>): AppResult<Pair<A, B>> =
            first.flatMap { a -> second.map { b -> a to b } }

        /** Every value in order, or the first failure. */
        public fun <T> combine(results: List<AppResult<T>>): AppResult<List<T>> {
            val values = ArrayList<T>(results.size)
            for (result in results) {
                when (result) {
                    is Success -> values.add(result.value)
                    is Failure -> return result
                }
            }
            return Success(values)
        }
    }
}

/** The result with its value replaced by [transform]; a failure stays as it is. */
public inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

/** The result with its error replaced by [transform]; a success stays as it is. */
public inline fun <T> AppResult<T>.mapError(transform: (AppError) -> AppError): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Failure -> AppResult.Failure(transform(error))
}

/** What [transform] returns for the value, which may be a failure of its own; a failure stays as it is. */
public inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(value)
    is AppResult.Failure -> this
}

/** What [onSuccess] returns for the value, or [onFailure] for the error. */
public inline fun <T, R> AppResult<T>.fold(onSuccess: (T) -> R, onFailure: (AppError) -> R): R = when (this) {
    is AppResult.Success -> onSuccess(value)
    is AppResult.Failure -> onFailure(error)
}

/** A success in every case: a failure becomes the value [transform] returns for its error. */
public inline fun <T> AppResult<T>.recover(transform: (AppError) -> T): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Failure -> AppResult.Success(transform(error))
}

/** Calls [action] with the value of a success and returns the result unchanged. */
public inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(value)
    return this
}

/** Calls [action] with the error of a failure and returns the result unchanged. */
public inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

/** The value, or null for a failure. */
public fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value

/** The value, or what [default] returns for the error. */
public inline fun <T> AppResult<T>.getOrElse(default: (AppError) -> T): T = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> default(error)
}

/** Fails with [error] when the value is null; the default is an [AppError.Unknown] for the logs. */
public fun <T : Any> AppResult<T?>.ensureNotNull(
    error: () -> AppError = { AppError.Unknown("Data is null") },
): AppResult<T> = when (this) {
    is AppResult.Success -> value?.let { AppResult.Success(it) } ?: AppResult.Failure(error())
    is AppResult.Failure -> this
}

/**
 * Runs [block] and maps a thrown [Exception] with [mapper]. Cancellation is rethrown, and an [Error]
 * (out of memory, a failed assertion, `TODO()`) is not caught: it is a bug, not a failed call.
 */
public suspend fun <T> catching(
    mapper: ErrorMapper = ErrorMapper.Default,
    block: suspend () -> T,
): AppResult<T> = try {
    AppResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Exception) {
    AppResult.Failure(mapper.map(failure))
}
