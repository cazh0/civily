package dev.cazh0.stately.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopulationTest {

    @Test
    fun `small populations stay in millions`() {
        val scaled = Population.scale(7)
        assertEquals("7", scaled.value)
        assertFalse(scaled.inBillions)
    }

    @Test
    fun `hundreds of millions are grouped`() {
        assertEquals("999", Population.scale(999).value)
    }

    @Test
    fun `a thousand million becomes one billion`() {
        val scaled = Population.scale(1_000)
        assertEquals("1", scaled.value)
        assertTrue(scaled.inBillions)
    }

    @Test
    fun `keeps three decimals of billions`() {
        // Testlandia's real figure, and what NationStates itself shows for it.
        assertEquals("49.902", Population.scale(49_902).value)
    }

    @Test
    fun `does not invent precision`() {
        assertEquals("50", Population.scale(50_000).value)
        assertEquals("49.9", Population.scale(49_900).value)
    }

    @Test
    fun `zero is not billions`() {
        val scaled = Population.scale(0)
        assertEquals("0", scaled.value)
        assertFalse(scaled.inBillions)
    }
}
