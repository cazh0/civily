package dev.cazh0.civily.core.text

import dev.cazh0.civily.core.text.Countdown.Resolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The countdown drives a live display, so the interesting cases are the boundaries: what it
 * reads on the second the value changes, and how long it waits to look again.
 */
class CountdownTest {

    @Test
    fun `breaks a gap into days hours minutes and seconds`() {
        val remaining = Countdown.until(
            targetEpochSeconds = at(days = 2, hours = 3, minutes = 4, seconds = 5),
            nowMs = 0L,
        )

        assertEquals(2, remaining.days)
        assertEquals(3, remaining.hours)
        assertEquals(4, remaining.minutes)
        assertEquals(5, remaining.seconds)
        assertFalse(remaining.isDue)
    }

    @Test
    fun `hours are hours of the day, not of the whole gap`() {
        // 26 hours is one day and two hours. A clock printing "26:00:00" is a stopwatch.
        val remaining = Countdown.until(at(hours = 26), nowMs = 0L)

        assertEquals(1, remaining.days)
        assertEquals(2, remaining.hours)
    }

    @Test
    fun `a passed instant is due, and never negative`() {
        val remaining = Countdown.until(targetEpochSeconds = 100L, nowMs = 500_000L)

        assertTrue(remaining.isDue)
        assertEquals(0L, remaining.totalSeconds)
        assertEquals(0, remaining.hours)
    }

    @Test
    fun `the exact instant is due`() {
        assertTrue(Countdown.until(targetEpochSeconds = 100L, nowMs = 100_000L).isDue)
    }

    @Test
    fun `part of a second is not a second`() {
        // 1.5s left reads as 1s, not 2s: a countdown that rounds up finishes before it reaches
        // zero, and the reader is left looking at "1" through the moment it was waiting for.
        assertEquals(1L, Countdown.until(targetEpochSeconds = 10L, nowMs = 8_500L).totalSeconds)
    }

    @Test
    fun `a whole gap in milliseconds survives the unit change`() {
        assertEquals(1_500L, Countdown.millisUntil(targetEpochSeconds = 10L, nowMs = 8_500L))
        assertEquals(-400L, Countdown.millisUntil(targetEpochSeconds = 10L, nowMs = 10_400L))
    }

    @Test
    fun `waits only until the displayed value changes`() {
        // 8.5s left at second resolution: the "8" becomes a "7" in 500ms, not in a full second.
        assertEquals(
            500L,
            Countdown.delayToNextStep(
                targetEpochSeconds = 10L,
                nowMs = 1_500L,
                resolution = Resolution.Seconds,
            ),
        )

        // Same instant read by a display that only writes minutes: nothing changes for 8.5s,
        // and then not again until the next whole minute would have passed.
        assertEquals(
            8_500L,
            Countdown.delayToNextStep(
                targetEpochSeconds = 10L,
                nowMs = 1_500L,
                resolution = Resolution.Minutes,
            ),
        )
    }

    @Test
    fun `a gap already on the boundary waits a whole step`() {
        // Exactly 2s left. Waking now would rewrite the same "2"; the next change is a second
        // away.
        assertEquals(
            1_000L,
            Countdown.delayToNextStep(
                targetEpochSeconds = 10L,
                nowMs = 8_000L,
                resolution = Resolution.Seconds,
            ),
        )
    }

    @Test
    fun `nothing left to wait for once the instant has passed`() {
        assertEquals(
            0L,
            Countdown.delayToNextStep(
                targetEpochSeconds = 10L,
                nowMs = 99_000L,
                resolution = Resolution.Seconds,
            ),
        )
    }

    private fun at(days: Int = 0, hours: Int = 0, minutes: Int = 0, seconds: Int = 0): Long =
        days * 86_400L + hours * 3_600L + minutes * 60L + seconds
}
