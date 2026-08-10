package dev.cazh0.stately.core.text.bbcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BbColorTest {

    @Test
    fun `parses six digit hex`() {
        assertEquals(0xFFFF0000.toInt(), BbColor.parse("#ff0000"))
    }

    @Test
    fun `parses three digit hex by doubling each digit`() {
        assertEquals(0xFFFF0000.toInt(), BbColor.parse("#f00"))
    }

    @Test
    fun `parses a named colour`() {
        assertEquals(0xFF0000FF.toInt(), BbColor.parse("blue"))
    }

    @Test
    fun `accepts either spelling of grey`() {
        assertEquals(BbColor.parse("gray"), BbColor.parse("grey"))
    }

    @Test
    fun `is case and whitespace insensitive`() {
        assertEquals(BbColor.parse("red"), BbColor.parse("  RED "))
    }

    @Test
    fun `an unknown name is not a colour`() {
        assertNull(BbColor.parse("chartreuse-ish"))
    }

    @Test
    fun `malformed hex is not a colour`() {
        assertNull(BbColor.parse("#gg0000"))
        assertNull(BbColor.parse("#ff00"))
        assertNull(BbColor.parse("#"))
    }

    @Test
    fun `empty is not a colour`() {
        assertNull(BbColor.parse(""))
    }
}
