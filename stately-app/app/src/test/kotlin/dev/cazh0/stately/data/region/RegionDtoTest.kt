package dev.cazh0.stately.data.region

import dev.cazh0.stately.data.NsXml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec §5 makes coverage of parsing mandatory.
 *
 * The founder case matters: NationStates reports "no founder" as the string `"0"`, not as a
 * missing element, so a screen that only checks for emptiness renders a link to nation zero.
 */
class RegionDtoTest {

    @Test
    fun `parses a full response`() {
        val region = NsXml.decodeFromString(
            RegionDto.serializer(),
            """
            <REGION id="testregionia">
                <NAME>Testregionia</NAME>
                <FLAG>https://www.nationstates.net/images/rflags/Test.png</FLAG>
                <NUMNATIONS>102</NUMNATIONS>
                <DELEGATE>testlandia</DELEGATE>
                <DELEGATEVOTES>14</DELEGATEVOTES>
                <FOUNDER>founderlandia</FOUNDER>
            </REGION>
            """.trimIndent(),
        )

        assertEquals("Testregionia", region.name)
        assertEquals("https://www.nationstates.net/images/rflags/Test.png", region.flagUrl)
        assertEquals(102, region.nationCount)
        assertEquals("testlandia", region.delegate)
        assertEquals(14, region.delegateVotes)
        assertEquals("founderlandia", region.founder)
        assertTrue(region.hasDelegate)
        assertTrue(region.hasFounder)
    }

    @Test
    fun `treats the zero sentinel as absent`() {
        val region = NsXml.decodeFromString(
            RegionDto.serializer(),
            """
            <REGION id="testregionia">
                <NAME>Testregionia</NAME>
                <DELEGATE>0</DELEGATE>
                <FOUNDER>0</FOUNDER>
            </REGION>
            """.trimIndent(),
        )

        assertFalse(region.hasDelegate)
        assertFalse(region.hasFounder)
    }

    @Test
    fun `missing elements fall back to defaults instead of failing`() {
        val region = NsXml.decodeFromString(
            RegionDto.serializer(),
            """<REGION id="testregionia"><NAME>Testregionia</NAME></REGION>""",
        )

        assertEquals("Testregionia", region.name)
        assertEquals("", region.flagUrl)
        assertEquals(0, region.nationCount)
        assertFalse(region.hasDelegate)
    }
}
