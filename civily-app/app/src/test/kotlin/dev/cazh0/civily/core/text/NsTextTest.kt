package dev.cazh0.civily.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NsTextTest {

    @Test
    fun `text without C1 characters is returned untouched`() {
        val input = "An ordinary post with — an em dash and “quotes”."

        assertSame(input, NsText.repair(input))
    }

    @Test
    fun `a smuggled right single quote becomes an apostrophe`() {
        // The exact defect seen live on the Osiris board: "doesnt have".
        assertEquals("doesn’t have", NsText.repair("doesnt have"))
    }

    @Test
    fun `a smuggled trademark sign is restored`() {
        assertEquals(
            "DISCOMBOBULATOR 3000™ GO",
            NsText.repair("DISCOMBOBULATOR 3000 GO"),
        )
    }

    @Test
    fun `restores the rest of the punctuation range`() {
        assertEquals(
            "€…“”–—",
            NsText.repair(""),
        )
    }

    @Test
    fun `unassigned slots are left alone rather than invented`() {
        assertEquals("", NsText.repair(""))
    }

    @Test
    fun `characters outside the range are not touched`() {
        assertEquals(" é漢", NsText.repair(" é漢"))
    }
}
