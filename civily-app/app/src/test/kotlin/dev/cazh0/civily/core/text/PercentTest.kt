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
}
