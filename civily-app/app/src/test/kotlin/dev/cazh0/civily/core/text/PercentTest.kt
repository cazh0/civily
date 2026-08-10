package dev.cazh0.civily.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentTest {

    @Test
    fun `a rise is signed and marked up`() {
        val change = Percent.change(5.111684)

        assertEquals("+5.1%", change.text)
        assertEquals(Percent.Direction.Up, change.direction)
    }

    @Test
    fun `a fall uses a real minus sign`() {
        val change = Percent.change(-0.389575)

        // U+2212, not a hyphen: a hyphen next to a digit reads as a dash, not a minus.
        assertEquals("−0.4%", change.text)
        assertEquals(Percent.Direction.Down, change.direction)
    }

    @Test
    fun `a movement that rounds to nothing claims no direction`() {
        // The defect this exists to stop: "-0.0%", which asserts a fall that did not happen.
        val change = Percent.change(-0.04)

        assertEquals("0.0%", change.text)
        assertEquals(Percent.Direction.Flat, change.direction)
    }

    @Test
    fun `exactly zero is flat`() {
        assertEquals("0.0%", Percent.change(0.0).text)
        assertEquals(Percent.Direction.Flat, Percent.change(0.0).direction)
    }

    @Test
    fun `rounding is to one decimal`() {
        assertEquals("+0.1%", Percent.change(0.05).text)
        assertEquals("+1.0%", Percent.change(1.028278).text)
    }

    @Test
    fun `large movements survive`() {
        assertEquals("+511.2%", Percent.change(511.16).text)
    }

    // ------------------------------------------------- shares, which carry no sign

    @Test
    fun `a trailing zero is dropped because it claims precision it does not add`() {
        // An income tax rate arrives as "87.0"; a budget line as "20.9".
        assertEquals("87", Percent.rounded(87.0))
        assertEquals("20.9", Percent.rounded(20.9))
        assertEquals("100", Percent.rounded(100.0))
        assertEquals("10", Percent.rounded(10.0))
    }

    @Test
    fun `two places are kept where they are the whole value`() {
        // A black market of 0.42% must not round away to nothing.
        assertEquals("0.42", Percent.rounded(0.42))
        assertEquals("92.16", Percent.rounded(92.16))
        // The API reports a leading percentile fractionally at the top of a scale.
        assertEquals("0.09", Percent.rounded(0.09))
    }

    @Test
    fun `zero is zero rather than an empty string`() {
        assertEquals("0", Percent.rounded(0.0))
    }
}
