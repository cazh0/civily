package dev.cazh0.stately.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlEntitiesTest {

    @Test
    fun `text without entities is returned unchanged`() {
        assertEquals("plain text", HtmlEntities.decode("plain text"))
    }

    @Test
    fun `decodes the common named entities`() {
        assertEquals("Tom & Jerry", HtmlEntities.decode("Tom &amp; Jerry"))
        assertEquals("<tag>", HtmlEntities.decode("&lt;tag&gt;"))
        assertEquals("\"quoted\"", HtmlEntities.decode("&quot;quoted&quot;"))
    }

    @Test
    fun `decodes numeric and hex references`() {
        assertEquals("'", HtmlEntities.decode("&#39;"))
        assertEquals("A", HtmlEntities.decode("&#x41;"))
    }

    @Test
    fun `a decoded ampersand is not read as a new entity`() {
        // The author wrote the literal text "&lt;". One pass, left to right, keeps it that way.
        assertEquals("&lt;", HtmlEntities.decode("&amp;lt;"))
    }

    @Test
    fun `an unknown entity is left exactly as written`() {
        assertEquals("&nosuchthing;", HtmlEntities.decode("&nosuchthing;"))
    }

    @Test
    fun `a bare ampersand survives`() {
        assertEquals("R&D and Q&A", HtmlEntities.decode("R&D and Q&A"))
    }

    @Test
    fun `a stray ampersand does not scan to the end of the text`() {
        val long = "&" + "x".repeat(200) + ";"

        assertEquals(long, HtmlEntities.decode(long))
    }

    @Test
    fun `an out of range code point is not decoded`() {
        assertEquals("&#999999999;", HtmlEntities.decode("&#999999999;"))
    }
}
