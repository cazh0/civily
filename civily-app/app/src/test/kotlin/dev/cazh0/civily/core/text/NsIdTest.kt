package dev.cazh0.civily.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class NsIdTest {

    @Test
    fun `lowercases the name`() {
        assertEquals("testlandia", NsId.fromName("Testlandia"))
    }

    @Test
    fun `replaces spaces with underscores`() {
        assertEquals("the_north_pacific", NsId.fromName("The North Pacific"))
    }

    @Test
    fun `strips accents to their ascii base letter`() {
        assertEquals("aland", NsId.fromName("Åland"))
    }

    @Test
    fun `keeps hyphens and digits`() {
        assertEquals("test-landia_2", NsId.fromName("Test-Landia 2"))
    }

    @Test
    fun `fromName is idempotent on an id`() {
        val id = NsId.fromName("The North Pacific")
        assertEquals(id, NsId.fromName(id))
    }

    @Test
    fun `toName capitalises a single word`() {
        assertEquals("Testlandia", NsId.toName("testlandia"))
    }

    @Test
    fun `toName capitalises the first word even when it is an article`() {
        assertEquals("The North Pacific", NsId.toName("the_north_pacific"))
    }

    @Test
    fun `toName leaves articles lower mid-name`() {
        assertEquals("Empire of the Sun", NsId.toName("empire_of_the_sun"))
    }

    @Test
    fun `toName uppercases roman numerals`() {
        assertEquals("Pope Hope XIV", NsId.toName("pope_hope_xiv"))
    }

    @Test
    fun `toName capitalises across hyphens`() {
        assertEquals("Test-Landia 2", NsId.toName("test-landia_2"))
    }

    @Test
    fun `toName keeps a trailing hyphen`() {
        assertEquals("Foo-", NsId.toName("foo-"))
    }

    @Test
    fun `toName round-trips through fromName`() {
        assertEquals("the_north_pacific", NsId.fromName(NsId.toName("the_north_pacific")))
    }
}
