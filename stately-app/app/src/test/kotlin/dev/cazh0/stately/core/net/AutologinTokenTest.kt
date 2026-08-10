package dev.cazh0.stately.core.net

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A device that logged in years ago still holds an old-format token, so both formats have to
 * survive the round trip. Getting this wrong signs the user out silently.
 */
class AutologinTokenTest {

    private val nationId = "testlandia"

    @Test
    fun `new format token gains the nation prefix in the cookie`() {
        assertEquals(
            "testlandia=.abcdef",
            AutologinToken.forCookie(nationId, ".abcdef"),
        )
    }

    @Test
    fun `new format token stays bare in the header`() {
        assertEquals(".abcdef", AutologinToken.forHeader(nationId, ".abcdef"))
    }

    @Test
    fun `old format token is decoded but not prefixed twice`() {
        assertEquals(
            "testlandia=abcdef",
            AutologinToken.forCookie(nationId, "testlandia%3Dabcdef"),
        )
    }

    @Test
    fun `old format token loses its prefix in the header`() {
        assertEquals("abcdef", AutologinToken.forHeader(nationId, "testlandia%3Dabcdef"))
    }

    @Test
    fun `plus signs in base64 tokens survive`() {
        // Why this matters: URLDecoder would have turned each `+` into a space, and the legacy
        // repair step turned every space back into `+` -- corrupting any genuine space.
        assertEquals("testlandia=.ab+cd+ef", AutologinToken.forCookie(nationId, ".ab+cd+ef"))
        assertEquals(".ab+cd+ef", AutologinToken.forHeader(nationId, ".ab+cd+ef"))
    }

    @Test
    fun `empty token is passed through untouched`() {
        assertEquals("", AutologinToken.forCookie(nationId, ""))
    }
}
