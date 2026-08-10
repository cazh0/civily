package dev.cazh0.stately.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {

    @Test
    fun `a single word gives one letter`() {
        assertEquals("T", Initials.of("Testlandia"))
    }

    @Test
    fun `two or more words give two letters`() {
        assertEquals("TN", Initials.of("The North Pacific"))
    }

    @Test
    fun `hyphens and underscores separate words`() {
        assertEquals("NL", Initials.of("New-Land"))
        assertEquals("TO", Initials.of("the_oceania_region"))
    }

    @Test
    fun `leading punctuation is skipped rather than shown`() {
        assertEquals("AB", Initials.of("\"Alpha\" (Bravo)"))
    }

    @Test
    fun `digits count as letters`() {
        assertEquals("T1", Initials.of("Tethys 13"))
    }

    @Test
    fun `a name with no letters falls back`() {
        assertEquals("?", Initials.of("!!! ---"))
        assertEquals("?", Initials.of(""))
    }
}
