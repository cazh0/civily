package dev.cazh0.stately.core.net

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec §5 makes coverage of the rate limiter mandatory.
 *
 * The limiter's clock is wired to the test scheduler's virtual clock, so a 30-second wait
 * costs nothing and every assertion is on exact times rather than tolerances.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RateLimiterTest {

    private val budget = RateLimiter.DEFAULT_SERVER_LIMIT - RateLimiter.BUFFER

    @Test
    fun `spends the whole budget without waiting`() = runTest {
        val limiter = RateLimiter { testScheduler.currentTime }

        repeat(budget) { limiter.acquire() }

        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun `waits out the window once the budget is spent`() = runTest {
        val limiter = RateLimiter { testScheduler.currentTime }
        repeat(budget) { limiter.acquire() }

        limiter.acquire()

        assertEquals(RateLimiter.WINDOW_MS, testScheduler.currentTime)
    }

    @Test
    fun `budget refills once the window rolls over`() = runTest {
        val limiter = RateLimiter { testScheduler.currentTime }
        repeat(budget) { limiter.acquire() }
        limiter.acquire() // Rolls the window.

        repeat(budget - 1) { limiter.acquire() }

        assertEquals(RateLimiter.WINDOW_MS, testScheduler.currentTime)
    }

    @Test
    fun `server remaining count overrides local bookkeeping`() = runTest {
        val limiter = RateLimiter { testScheduler.currentTime }

        // Server says 6 of 50 are left, so 44 are spent -- one below our buffered budget of 45.
        limiter.sync(limit = 50, remaining = 6, resetInSeconds = null)
        limiter.acquire()
        assertEquals(0L, testScheduler.currentTime)

        limiter.acquire()
        assertEquals(RateLimiter.WINDOW_MS, testScheduler.currentTime)
    }

    @Test
    fun `reset header ends the window at the instant the server reported`() = runTest {
        val limiter = RateLimiter { testScheduler.currentTime }

        limiter.sync(limit = 50, remaining = 0, resetInSeconds = 10)
        limiter.acquire()

        // Why this is the regression test for the legacy bug: setting the window *start* to
        // now + reset would have made this wait a full 30s past the server's own reset.
        assertEquals(10_000L, testScheduler.currentTime)
    }

    @Test
    fun `a server lockout blocks every request for exactly as long as asked`() = runTest {
        val limiter = RateLimiter { testScheduler.currentTime }

        limiter.backOff(seconds = 12)
        limiter.acquire()

        assertEquals(12_000L, testScheduler.currentTime)
    }

    @Test
    fun `a second lockout cannot shorten the first`() = runTest {
        val limiter = RateLimiter { testScheduler.currentTime }

        limiter.backOff(seconds = 60)
        limiter.backOff(seconds = 5)
        limiter.acquire()

        assertEquals(60_000L, testScheduler.currentTime)
    }

    @Test
    fun `absent headers leave the budget untouched`() = runTest {
        val limiter = RateLimiter { testScheduler.currentTime }

        limiter.sync(limit = null, remaining = null, resetInSeconds = null)
        repeat(budget) { limiter.acquire() }

        assertEquals(0L, testScheduler.currentTime)
    }
}
