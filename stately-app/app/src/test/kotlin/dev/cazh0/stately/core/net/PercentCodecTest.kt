package dev.cazh0.stately.core.net

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentCodecTest {

    @Test
    fun `decodes percent escapes`() {
        assertEquals("a=b", PercentCodec.decode("a%3Db"))
    }

    @Test
    fun `leaves plus alone`() {
        assertEquals("a+b", PercentCodec.decode("a+b"))
    }

    @Test
    fun `decodes an encoded space without turning plus into one`() {
        assertEquals("a b+c", PercentCodec.decode("a%20b+c"))
    }

    @Test
    fun `passes through a stray percent`() {
        assertEquals("100% sure", PercentCodec.decode("100% sure"))
    }

    @Test
    fun `passes through a truncated escape at the end`() {
        assertEquals("abc%3", PercentCodec.decode("abc%3"))
    }

    @Test
    fun `accepts lower case hex`() {
        assertEquals("/", PercentCodec.decode("%2f"))
    }
}
