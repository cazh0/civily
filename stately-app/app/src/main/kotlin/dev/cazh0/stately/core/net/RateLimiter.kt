package dev.cazh0.stately.core.net

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Enforces the NationStates request budget of 50 requests per rolling 30-second window.
 *
 * Why [acquire] suspends instead of returning false: the legacy `DashHelper.addRequest`
 * returned a boolean and left each of its ~60 call sites to decide what a refusal meant.
 * Most did nothing, so a throttled request became a screen that silently never loaded.
 * Suspending makes the budget invisible to callers and impossible to bypass — the worst
 * case is a wait bounded by [WINDOW_MS].
 *
 * [nowMs] is injected so the whole class is testable without sleeping.
 */
class RateLimiter(private val nowMs: () -> Long = System::currentTimeMillis) {

    private val mutex = Mutex()

    /** The cap NationStates itself reports. Synced from `RateLimit-Limit`. */
    private var serverLimit = DEFAULT_SERVER_LIMIT

    private var used = 0
    private var windowStartMs = nowMs()

    /**
     * Set when the server has already locked us out. Nothing may leave until this passes.
     *
     * Why it exists separately from the window: the documented penalty escalates. Tripping the
     * limit costs the rest of the window, but tripping it repeatedly earns "15 minutes or
     * longer". Honouring `Retry-After` exactly is what keeps the app out of that.
     */
    private var blockedUntilMs = 0L

    /**
     * Why the buffer: image loads and redirects also spend budget, and the server's counter
     * is authoritative. Staying [BUFFER] requests short of the cap leaves room for the drift
     * between our count and theirs without ever earning a 429.
     */
    private val effectiveLimit: Int
        get() = (serverLimit - BUFFER).coerceAtLeast(1)

    /** Suspends until a request may be sent, then consumes one slot. */
    suspend fun acquire() {
        while (true) {
            val waitMs = mutex.withLock { takeSlotOrWait() }
            if (waitMs <= 0L) return
            delay(waitMs)
        }
    }

    /**
     * Reconciles local bookkeeping with the `RateLimit-*` headers on a response.
     * Every parameter is null when the header was absent or unparseable.
     */
    suspend fun sync(limit: Int?, remaining: Int?, resetInSeconds: Int?) = mutex.withLock {
        // Order matters: `remaining` is relative to the server's cap, so the cap is applied first.
        if (limit != null && limit > 0) serverLimit = limit
        if (remaining != null) used = (serverLimit - remaining).coerceAtLeast(0)
        if (resetInSeconds != null) {
            // Why: the header says how many seconds until the current window ends, so the
            // window must have *started* one full window before that instant. The legacy code
            // set the start to `now + reset`, which pushed each reset a further 30s into the
            // future and let the counter run stale under sustained load.
            windowStartMs = nowMs() + resetInSeconds * 1000L - WINDOW_MS
        }
    }

    /**
     * Records a server lockout. [seconds] comes from the `Retry-After` header on a 429.
     *
     * The longest known block wins: a second 429 arriving while an earlier one is still in
     * force must not shorten the wait.
     */
    suspend fun backOff(seconds: Int) = mutex.withLock {
        blockedUntilMs = maxOf(blockedUntilMs, nowMs() + seconds * 1000L)
    }

    /** Returns 0 when a slot was consumed, otherwise the milliseconds to wait before retrying. */
    private fun takeSlotOrWait(): Long {
        val now = nowMs()
        if (now < blockedUntilMs) return blockedUntilMs - now
        if (now - windowStartMs >= WINDOW_MS) {
            windowStartMs = now
            used = 0
        }
        return if (used < effectiveLimit) {
            used++
            0L
        } else {
            windowStartMs + WINDOW_MS - now
        }
    }

    companion object {
        const val WINDOW_MS = 30_000L
        const val BUFFER = 5

        /** NationStates' documented default; overwritten by the first `RateLimit-Limit` seen. */
        const val DEFAULT_SERVER_LIMIT = 50
    }
}
