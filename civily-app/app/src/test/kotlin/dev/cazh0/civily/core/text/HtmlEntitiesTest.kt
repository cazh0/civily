package dev.cazh0.civily.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlEntitiesTest {

    @Test
    fun `text without entities is returned unchanged`() {
        assertEquals("plain text", HtmlEntities.decode("plain text"))
    }

    @Test
    fun `decodes the common named entities`() {
        assertEquals("Tom & Jerry", HtmlEntities.decode("Tom &amp;amp; Jerry"))
        assertEquals("<tag>", HtmlEntities.decode("&lt;tag&gt;"))
        assertEquals("\"quoted\"", HtmlEntities.decode("&quot;quoted&quot;"))
    }

    @Test
    fun `decodes numeric and hex references`() {
        assertEquals("'", HtmlEntities.decode("&#39;"))
        assertEquals("A", HtmlEntities.decode("&#x41;"))
    }

    @Test
    fun `the API's second layer of escaping is undone`() {
        // Verified on the wire: region=lazarus&q=factbook opens with the literal bytes
        // "&amp;#43457;" inside CDATA. One pass leaves "&#43457;" on the reader's screen.
        assertEquals("꧁", HtmlEntities.decode("&amp;#43457;"))
    }

    @Test
    fun `a singly escaped entity in the same text still decodes`() {
        // The same factbook mixes both depths: "&amp;#715;&deg;" is one of each.
        assertEquals("ˋ°", HtmlEntities.decode("&amp;#715;&deg;"))
    }

    @Test
    fun `a reference above the basic plane survives as a surrogate pair`() {
        assertEquals("🐦", HtmlEntities.decode("&amp;#128038;"))
    }

    @Test
    fun `a numeric reference into the C1 range is Windows-1252`() {
        // "&#149;" is a bullet on the site, not an unprintable control. This is the same
        // mistake NsText repairs, arriving as a reference rather than as a raw byte.
        assertEquals("•", HtmlEntities.decode("&#149;"))
    }

    @Test
    fun `a C1 slot Windows-1252 leaves empty draws nothing`() {
        assertEquals("", HtmlEntities.decode("&#129;"))
    }

    @Test
    fun `the NationStates icon font is dropped rather than drawn as tofu`() {
        // Every [font=nationstates] run in Lazarus's factbook is a Private Use code point.
        assertEquals("", HtmlEntities.decode("&amp;#59425;"))
        assertEquals("Join", HtmlEntities.decode("&amp;#59431;Join"))
    }

    @Test
    fun `the factbook line that exposed all of this now reads as the site shows it`() {
        assertEquals(
            "꧁ AXIOTARIAN",
            HtmlEntities.decode("&amp;#43457; AXIOTARIAN"),
        )
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

    @Test
    fun `three layers of escaping stop at two`() {
        // Nothing on the wire needs a third pass, and a third would start eating text an
        // author escaped on purpose.
        assertEquals("&lt;", HtmlEntities.decode("&amp;amp;lt;"))
    }
}
