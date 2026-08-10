package dev.cazh0.civily.core.text

import dev.cazh0.civily.core.text.Magnitude.Unit
import org.junit.Assert.assertEquals
import org.junit.Test

class MagnitudeTest {

    @Test
    fun `figures a person can already read are left alone`() {
        // An average income. "66.4 thousand" would be harder to read than the number it replaced.
        assertEquals(Magnitude.Scaled("66,427", Unit.Ones), Magnitude.scale(66_427))
        assertEquals(Magnitude.Scaled("0", Unit.Ones), Magnitude.scale(0))
        assertEquals(Magnitude.Scaled("999,999", Unit.Ones), Magnitude.scale(999_999))
    }

    @Test
    fun `a gross domestic product is scaled the way the game writes it`() {
        // Testlandia's, verbatim off the wire. Sixteen digits nobody counts.
        assertEquals(Magnitude.Scaled("3,315", Unit.Trillion), Magnitude.scale(3_315_350_844_221_109))
    }

    @Test
    fun `each unit starts at its own boundary`() {
        assertEquals(Magnitude.Scaled("1", Unit.Million), Magnitude.scale(1_000_000))
        assertEquals(Magnitude.Scaled("1", Unit.Billion), Magnitude.scale(1_000_000_000))
        assertEquals(Magnitude.Scaled("1", Unit.Trillion), Magnitude.scale(1_000_000_000_000))
    }

    @Test
    fun `a decimal is kept only where it carries information`() {
        // "1 million" throws away a third of 1.5 million; "3,315.4 trillion" spends a digit on
        // precision at a size where nobody is counting.
        assertEquals(Magnitude.Scaled("1.5", Unit.Million), Magnitude.scale(1_500_000))
        assertEquals(Magnitude.Scaled("9.9", Unit.Billion), Magnitude.scale(9_900_000_000))
        assertEquals(Magnitude.Scaled("10", Unit.Billion), Magnitude.scale(10_000_000_000))
        assertEquals(Magnitude.Scaled("105", Unit.Trillion), Magnitude.scale(105_400_000_000_000))
    }

    @Test
    fun `a whole number of units does not print a needless decimal`() {
        assertEquals(Magnitude.Scaled("2", Unit.Million), Magnitude.scale(2_000_000))
    }
}
