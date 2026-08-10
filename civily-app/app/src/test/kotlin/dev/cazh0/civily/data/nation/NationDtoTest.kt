package dev.cazh0.civily.data.nation

import dev.cazh0.civily.data.NsXml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec §5 makes coverage of parsing mandatory.
 *
 * The samples carry the shapes NationStates actually sends -- an `id` attribute we never
 * declared, and shards that go missing when the nation has no data for them.
 */
class NationDtoTest {

    @Test
    fun `parses a full response`() {
        val nation = NsXml.decodeFromString(
            NationDto.serializer(),
            """
            <NATION id="testlandia">
                <NAME>Testlandia</NAME>
                <TYPE>Hive Mind</TYPE>
                <MOTTO>Grr. Arg.</MOTTO>
                <FLAG>https://www.nationstates.net/images/flags/Test.png</FLAG>
                <CATEGORY>Inoffensive Centrist Democracy</CATEGORY>
                <REGION>Testregionia</REGION>
                <POPULATION>39000</POPULATION>
                <UNSTATUS>WA Member</UNSTATUS>
            </NATION>
            """.trimIndent(),
        )

        assertEquals("Testlandia", nation.name)
        assertEquals("Hive Mind", nation.type)
        assertEquals("Grr. Arg.", nation.motto)
        assertEquals("https://www.nationstates.net/images/flags/Test.png", nation.flagUrl)
        assertEquals("Inoffensive Centrist Democracy", nation.category)
        assertEquals("Testregionia", nation.region)
        assertEquals(39000, nation.population)
        assertEquals("WA Member", nation.waStatus)
    }

    @Test
    fun `missing elements fall back to defaults instead of failing`() {
        val nation = NsXml.decodeFromString(
            NationDto.serializer(),
            """<NATION id="testlandia"><NAME>Testlandia</NAME></NATION>""",
        )

        assertEquals("Testlandia", nation.name)
        assertEquals("", nation.motto)
        assertEquals(0, nation.population)
    }

    @Test
    fun `delegates count as World Assembly members`() {
        // Why this is a test and not an equality check at the call site: a delegate is a
        // member, and treating "WA Delegate" as non-member would hide WA features from
        // exactly the players who use them most.
        assertTrue(NationDto(waStatus = "WA Member").isWaMember)
        assertTrue(NationDto(waStatus = "WA Delegate").isWaMember)
        assertFalse(NationDto(waStatus = "Non-member").isWaMember)
        assertFalse(NationDto().isWaMember)
    }

    @Test
    fun `unknown elements are ignored rather than fatal`() {
        // Why: NationStates adds shards without notice. A new element must not take the
        // screen down (spec §1.2).
        val nation = NsXml.decodeFromString(
            NationDto.serializer(),
            """
            <NATION id="testlandia">
                <NAME>Testlandia</NAME>
                <SOMETHING_NEW>surprise</SOMETHING_NEW>
            </NATION>
            """.trimIndent(),
        )

        assertEquals("Testlandia", nation.name)
    }
}
