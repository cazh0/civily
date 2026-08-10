package dev.cazh0.stately.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_000_000_000L
    private val nowMs = now * 1000

    private fun ago(seconds: Long) = RelativeTime.compact(now - seconds, nowMs)

    @Test
    fun `seconds ago reads as now`() {
        assertEquals("now", ago(0))
        assertEquals("now", ago(59))
    }

    @Test
    fun `a clock running behind the server does not produce negative time`() {
        // The device clock can be a little behind; "-3s" is not a thing anyone should see.
        assertEquals("now", RelativeTime.compact(now + 30, nowMs))
    }

    @Test
    fun `minutes`() {
        assertEquals("1m", ago(60))
        assertEquals("59m", ago(59 * 60))
    }

    @Test
    fun `hours`() {
        assertEquals("1h", ago(60 * 60))
        assertEquals("23h", ago(23 * 60 * 60))
    }

    @Test
    fun `days`() {
        assertEquals("1d", ago(24 * 60 * 60))
        assertEquals("6d", ago(6 * 24 * 60 * 60))
    }

    @Test
    fun `weeks`() {
        assertEquals("1w", ago(7 * 24 * 60 * 60))
        assertEquals("51w", ago(51 * 7 * 24 * 60 * 60))
    }

    @Test
    fun `years`() {
        assertEquals("1y", ago(400L * 24 * 60 * 60))
        assertEquals("3y", ago(3L * 365 * 24 * 60 * 60))
    }
}
