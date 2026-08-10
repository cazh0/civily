package dev.cazh0.civily.core.result

import androidx.annotation.StringRes
import dev.cazh0.civily.R

/**
 * Every way a request can fail, and the message the user sees for it.
 *
 * Why: binding the string resource to the error type at the point of definition means no
 * call site can invent its own wording, and no error can reach the UI without one — which
 * is what makes "every failure visible to user" (spec §1.2) enforceable rather than a habit.
 */
sealed class CivilyError(@StringRes val messageRes: Int) {

    /**
     * Whether the only way forward is signing in again.
     *
     * Retrying an [Unauthorized] can never succeed — the credentials themselves are dead — so
     * offering "Try again" would be a button that is guaranteed to fail. The failure state
     * asks for what is actually needed instead.
     */
    val needsSignIn: Boolean get() = this is Unauthorized

    /** Device offline, DNS failure, timeout — anything that never reached NationStates. */
    data object NoConnection : CivilyError(R.string.error_no_connection)

    /** Autologin or PIN rejected. The session needs re-establishing. */
    data object Unauthorized : CivilyError(R.string.error_unauthorized)

    /** A sign-in attempt was refused. Distinct from [Unauthorized]: nothing has expired,
     *  the name or password was simply wrong, and the wording must not imply otherwise. */
    data object InvalidLogin : CivilyError(R.string.error_invalid_login)

    /** The nation, region or resolution does not exist. */
    data object NotFound : CivilyError(R.string.error_not_found)

    /**
     * The API locked us out (429). The rate limiter has already been told how long for; this
     * only exists so the user is told too rather than watching a spinner.
     */
    data object RateLimited : CivilyError(R.string.error_rate_limited)

    /**
     * NationStates refused a login because one succeeded moments ago (409). Its own docs call
     * this out: logging in cancels the previous session, so two in quick succession conflict.
     */
    data object LoginConflict : CivilyError(R.string.error_login_conflict)

    /** NationStates answered, but with an error status. */
    data class Server(val code: Int) : CivilyError(R.string.error_server)

    /** The response arrived but did not parse. [detail] is for the log, never for the user. */
    data class Malformed(val detail: String) : CivilyError(R.string.error_malformed)
}
