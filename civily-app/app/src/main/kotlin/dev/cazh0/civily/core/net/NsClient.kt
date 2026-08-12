package dev.cazh0.civily.core.net

import android.util.Log
import dev.cazh0.civily.core.result.Outcome
import dev.cazh0.civily.core.result.CivilyError
import dev.cazh0.civily.core.result.map
import dev.cazh0.civily.core.session.Session
import dev.cazh0.civily.core.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * The only route to the NationStates servers.
 *
 * Every request that leaves the app passes through here, which is what makes three of the
 * spec's hard rules structural rather than aspirational: the rate limit cannot be bypassed
 * (§3), credentials are attached in one place and logged in none (§3), and no failure can
 * reach a caller as anything but an [Outcome.Failure] (§1.2, §5).
 *
 * All work runs on [Dispatchers.IO]; nothing here may be called from the main thread's
 * critical path (§2 R2b).
 */
class NsClient(
    private val http: OkHttpClient,
    private val limiter: RateLimiter,
    private val session: SessionStore,
    private val appVersion: String,
) {

    /** A successful sign-in and the credentials NationStates issued for it. */
    data class SignIn(val body: String, val autologin: String, val pin: String?)

    /**
     * @param userClick true when a human action triggered this request. NationStates uses the
     *   parameter to distinguish deliberate actions from background polling, and rejects some
     *   endpoints without it.
     */
    suspend fun get(url: HttpUrl, userClick: Boolean = false): Outcome<String> =
        execute(
            builder = Request.Builder().url(withUserClick(url, userClick)).get(),
            headers = sessionHeaders(),
            adoptCredentials = true,
            invalidateSessionOnAuthFailure = true,
        ).map { it.body }

    suspend fun getSitePage(url: HttpUrl): Outcome<String> =
        execute(
            builder = Request.Builder().url(url).get(),
            headers = siteHeaders(referer = null),
            adoptCredentials = true,
            invalidateSessionOnAuthFailure = false,
        ).map { it.body }

    suspend fun post(
        url: HttpUrl,
        form: Map<String, String>,
        userClick: Boolean = true,
    ): Outcome<String> {
        val body = FormBody.Builder().apply {
            form.forEach { (key, value) -> add(key, value) }
        }.build()
        return execute(
            builder = Request.Builder().url(withUserClick(url, userClick)).post(body),
            headers = sessionHeaders(),
            adoptCredentials = true,
            invalidateSessionOnAuthFailure = true,
        ).map { it.body }
    }

    suspend fun postSiteForm(
        url: HttpUrl,
        form: Map<String, String>,
        referer: HttpUrl,
    ): Outcome<String> {
        val body = FormBody.Builder().apply {
            form.forEach { (key, value) -> add(key, value) }
        }.build()
        return execute(
            builder = Request.Builder().url(url).post(body),
            headers = siteHeaders(referer = referer),
            adoptCredentials = true,
            invalidateSessionOnAuthFailure = false,
        ).map { it.body }
    }

    /**
     * Trades a password for a long-lived autologin token.
     *
     * The password is sent once, in the `X-Password` header, to nationstates.net and nowhere
     * else. It is never stored, never logged and never returned — only the token NationStates
     * issues in exchange survives this call (spec §3).
     *
     * [url] must request at least one private shard. A request for public data alone can
     * succeed regardless of the password, which would let a wrong one look like a right one.
     */
    suspend fun signIn(url: HttpUrl, password: String): Outcome<SignIn> {
        // Why the session is deliberately not consulted here: signing in as a second nation
        // must not carry the first nation's cookies, and a failed sign-in must not disturb
        // the session that already works.
        val outcome = execute(
            builder = Request.Builder().url(url).get(),
            headers = signInHeaders(password),
            adoptCredentials = false,
            invalidateSessionOnAuthFailure = true,
        )

        return when {
            outcome is Outcome.Failure && outcome.error == CivilyError.Unauthorized ->
                Outcome.Failure(CivilyError.InvalidLogin)

            outcome is Outcome.Success -> {
                val autologin = outcome.value.autologin
                // Why: NationStates issues X-Autologin only when it accepted the password.
                // Its absence is a rejected sign-in whatever the status code said.
                if (autologin.isNullOrEmpty()) {
                    Outcome.Failure(CivilyError.InvalidLogin)
                } else {
                    Outcome.Success(SignIn(outcome.value.body, autologin, outcome.value.pin))
                }
            }

            else -> outcome as Outcome.Failure
        }
    }

    private suspend fun execute(
        builder: Request.Builder,
        headers: Headers,
        adoptCredentials: Boolean,
        invalidateSessionOnAuthFailure: Boolean,
    ): Outcome<RawResponse> = withContext(Dispatchers.IO) {
        limiter.acquire()
        val request = builder.headers(headers).build()
        try {
            http.newCall(request).execute().use { response ->
                val raw = readCredentials(response)
                limiter.sync(
                    limit = response.header(HEADER_RATE_LIMIT_LIMIT)?.toIntOrNull(),
                    remaining = response.header(HEADER_RATE_LIMIT_REMAINING)?.toIntOrNull(),
                    resetInSeconds = response.header(HEADER_RATE_LIMIT_RESET)?.toIntOrNull(),
                )
                if (adoptCredentials) adopt(raw)

                if (!response.isSuccessful) {
                    Log.w(TAG, "Request to ${request.url.encodedPath} returned HTTP ${response.code}")
                }

                when {
                    response.isSuccessful -> Outcome.Success(
                        raw.copy(body = response.body?.string().orEmpty()),
                    )

                    response.code == HTTP_UNAUTHORIZED || response.code == HTTP_FORBIDDEN -> {
                        // Why the account is forgotten rather than merely reported: the API
                        // documents X-Autologin as valid until the nation's password changes,
                        // and an expired PIN falls back to it automatically. A rejected
                        // session request therefore means the token is dead for good. Keeping
                        // it would leave the app showing a signed-in nation whose every
                        // request fails, with no way for the user to tell why.
                        //
                        // Site form pages can also answer 403 for page/form reasons after an
                        // API request just succeeded. Those calls pass
                        // `invalidateSessionOnAuthFailure = false`, so they surface as server
                        // failures without destroying a known-good session.
                        //
                        // Only the rejected nation goes. Any other account the user has stored
                        // holds a separate session and is unaffected.
                        if (invalidateSessionOnAuthFailure) {
                            val active = session.current
                            if (adoptCredentials && active != null) session.forget(active.nationId)
                            Outcome.Failure(CivilyError.Unauthorized)
                        } else {
                            Outcome.Failure(CivilyError.Server(response.code))
                        }
                    }

                    response.code == HTTP_NOT_FOUND -> Outcome.Failure(CivilyError.NotFound)

                    response.code == HTTP_TOO_MANY_REQUESTS -> {
                        // Why the limiter is told before the caller: repeated 429s escalate
                        // from a short lockout to fifteen minutes or more. Blocking every
                        // outgoing request for exactly as long as the server asked is the
                        // only thing that keeps a retry loop from earning the longer penalty.
                        limiter.backOff(retryAfterSeconds(response))
                        Outcome.Failure(CivilyError.RateLimited)
                    }

                    response.code == HTTP_CONFLICT -> Outcome.Failure(CivilyError.LoginConflict)

                    else -> Outcome.Failure(CivilyError.Server(response.code))
                }
            }
        } catch (e: IOException) {
            // Why: §5 allows catching only at an IO boundary, and only when the failure is
            // both logged and surfaced. The log records the path alone — never headers,
            // never the query — because both can carry credentials (§3).
            Log.w(TAG, "Request to ${request.url.encodedPath} failed", e)
            Outcome.Failure(CivilyError.NoConnection)
        }
    }

    /**
     * How long the server says to wait. `Retry-After` is documented in seconds; the window
     * reset is the next best answer, and one full window the last resort.
     */
    private fun retryAfterSeconds(response: Response): Int =
        response.header(HEADER_RETRY_AFTER)?.toIntOrNull()
            ?: response.header(HEADER_RATE_LIMIT_RESET)?.toIntOrNull()
            ?: (RateLimiter.WINDOW_MS / 1000L).toInt()

    private fun readCredentials(response: Response): RawResponse = RawResponse(
        body = "",
        autologin = response.header(HEADER_AUTOLOGIN)?.takeIf { it.isNotEmpty() },
        pin = (
            response.header(HEADER_PIN)
                ?: response.headers(HEADER_SET_COOKIE).firstNotNullOfOrNull { pinFromCookie(it) }
            )?.takeIf { it != Session.INVALID_PIN },
    )

    /** Folds a rotated token or PIN into the session that is already signed in. */
    private fun adopt(raw: RawResponse) {
        raw.autologin?.let(session::updateAutologin)
        raw.pin?.let(session::updatePin)
    }

    private fun sessionHeaders(): Headers {
        val active = session.current
        val builder = Headers.Builder()
            .add(HEADER_USER_AGENT, UserAgent.of(appVersion, active?.nationId))

        if (active != null) {
            val autologin = active.autologin
            val cookieToken = AutologinToken.forCookie(active.nationId, autologin)
            builder.add(HEADER_AUTOLOGIN, AutologinToken.forHeader(active.nationId, autologin))

            val pin = active.validPin
            if (pin == null) {
                builder.add(HEADER_COOKIE, "autologin=$cookieToken")
            } else {
                builder.add(HEADER_COOKIE, "autologin=$cookieToken; pin=$pin")
                builder.add(HEADER_PIN, pin)
            }
        }
        return builder.build()
    }

    private fun siteHeaders(referer: HttpUrl?): Headers {
        val builder = sessionHeaders().newBuilder()
            .add(HEADER_ACCEPT, ACCEPT_HTML)

        if (referer != null) {
            // Why: the site enact endpoint is a browser form guarded by a CSRF token. Browser
            // submissions carry same-origin context as well as the hidden token; without it the
            // server can reject the POST before returning the aftermath page.
            builder
                .add(HEADER_ORIGIN, ORIGIN)
                .add(HEADER_REFERER, referer.toString())
        }
        return builder.build()
    }

    private fun signInHeaders(password: String): Headers = Headers.Builder()
        .add(HEADER_USER_AGENT, UserAgent.of(appVersion, null))
        .add(HEADER_PASSWORD, password)
        .build()

    private fun withUserClick(url: HttpUrl, userClick: Boolean): HttpUrl =
        if (!userClick) {
            url
        } else {
            url.newBuilder()
                .addQueryParameter(PARAM_USER_CLICK, System.currentTimeMillis().toString())
                .build()
        }

    private data class RawResponse(
        val body: String,
        val autologin: String?,
        val pin: String?,
    )

    private companion object {
        const val TAG = "NsClient"

        const val HEADER_USER_AGENT = "User-Agent"
        const val HEADER_COOKIE = "Cookie"
        const val HEADER_SET_COOKIE = "Set-Cookie"
        const val HEADER_PIN = "X-Pin"
        const val HEADER_PASSWORD = "X-Password"
        const val HEADER_AUTOLOGIN = "X-Autologin"
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_ORIGIN = "Origin"
        const val HEADER_REFERER = "Referer"
        const val HEADER_RATE_LIMIT_LIMIT = "RateLimit-Limit"
        const val HEADER_RATE_LIMIT_REMAINING = "RateLimit-Remaining"
        const val HEADER_RATE_LIMIT_RESET = "RateLimit-Reset"
        const val HEADER_RETRY_AFTER = "Retry-After"

        const val PARAM_USER_CLICK = "userclick"
        const val ACCEPT_HTML = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        const val ORIGIN = "https://www.nationstates.net"

        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_TOO_MANY_REQUESTS = 429

        // Why `find` and not `matches`: a Set-Cookie header carries attributes after the value
        // (`pin=123; path=/; HttpOnly`). The legacy regex was anchored with `matches()`, so it
        // required the whole header to be the pin and therefore never fired — the cookie
        // fallback has been dead code since it was written.
        val COOKIE_PIN = Regex("""(?:^|[;\s])pin=(\d+)""")

        fun pinFromCookie(header: String): String? =
            COOKIE_PIN.find(header)?.groupValues?.get(1)
    }
}
