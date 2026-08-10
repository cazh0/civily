package dev.cazh0.stately.core.result

/**
 * The only way a repository reports the outcome of work that can fail.
 *
 * Why: spec §1.2 and §5 forbid silent failure. A thrown exception can be swallowed by an
 * empty `catch`; a nullable return can be handled with `?: return`. A [Failure] carries a
 * [StatelyError] that already knows which message the user must be shown, so the cheapest
 * path for a caller is also the correct one.
 */
sealed interface Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>
    data class Failure(val error: StatelyError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (StatelyError) -> R,
): R = when (this) {
    is Outcome.Success -> onSuccess(value)
    is Outcome.Failure -> onFailure(error)
}
