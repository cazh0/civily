package dev.cazh0.civily.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_000_000_000L
    private val nowMs = now * 1000

    private fun elapsed(seconds: Long) = RelativeTime.elapsed(now - seconds, nowMs)

    @Test
    fun `the gap is reported as a count and a unit for the plural to use`() {
        assertEquals(RelativeTime.Elapsed(3, RelativeTime.Grain.Hours), elapsed(3 * 60 * 60))
        assertEquals(RelativeTime.Elapsed(1, RelativeTime.Grain.Hours), elapsed(60 * 60))
        assertEquals(RelativeTime.Elapsed(45, RelativeTime.Grain.Minutes), elapsed(45 * 60))
        assertEquals(RelativeTime.Elapsed(2, RelativeTime.Grain.Days), elapsed(2 * 24 * 60 * 60))
        assertEquals(
            RelativeTime.Elapsed(6, RelativeTime.Grain.Weeks),
            elapsed(6L * 7 * 24 * 60 * 60),
        )
        assertEquals(
            RelativeTime.Elapsed(2, RelativeTime.Grain.Years),
            elapsed(2L * 365 * 24 * 60 * 60),
        )
    }

    @Test
    fun `a gap under a minute has no count to pluralise`() {
        assertEquals(RelativeTime.Elapsed(0, RelativeTime.Grain.Now), elapsed(0))
        assertEquals(RelativeTime.Elapsed(0, RelativeTime.Grain.Now), elapsed(59))
        // Still true when the device clock runs behind the server's.
        assertEquals(
            RelativeTime.Elapsed(0, RelativeTime.Grain.Now),
            RelativeTime.elapsed(now + 30, nowMs),
        )
    }
}
