package dev.cazh0.stately.data.wa

import dev.cazh0.stately.data.NsXml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec §5 makes coverage of parsing mandatory.
 *
 * The empty-chamber sample is the exact response the live API returned while this was being
 * written. Between votes it is the normal answer, not an edge case.
 */
class AssemblyDtoTest {

    @Test
    fun `an empty chamber parses and reports nothing at vote`() {
        val assembly = NsXml.decodeFromString(
            AssemblyDto.serializer(),
            """
            <WA council="1">
            <NUMNATIONS>14575</NUMNATIONS>
            <NUMDELEGATES>680</NUMDELEGATES>
            <RESOLUTION></RESOLUTION>
            </WA>
            """.trimIndent(),
        )

        assertEquals(14575, assembly.memberCount)
        assertEquals(680, assembly.delegateCount)
        assertFalse(assembly.resolution.isAtVote)
    }

    @Test
    fun `a resolution at vote parses`() {
        val assembly = NsXml.decodeFromString(
            AssemblyDto.serializer(),
            """
            <WA council="1">
            <NUMNATIONS>14575</NUMNATIONS>
            <NUMDELEGATES>680</NUMDELEGATES>
            <RESOLUTION>
                <NAME>Repeal "Test Resolution"</NAME>
                <CATEGORY>Repeal</CATEGORY>
                <PROPOSED_BY>testlandia</PROPOSED_BY>
                <TOTAL_VOTES_FOR>7500</TOTAL_VOTES_FOR>
                <TOTAL_VOTES_AGAINST>2500</TOTAL_VOTES_AGAINST>
            </RESOLUTION>
            </WA>
            """.trimIndent(),
        )

        val resolution = assembly.resolution
        assertTrue(resolution.isAtVote)
        assertEquals("Repeal \"Test Resolution\"", resolution.name)
        assertEquals("Repeal", resolution.category)
        assertEquals("testlandia", resolution.proposedBy)
        assertEquals(7500, resolution.votesFor)
        assertEquals(2500, resolution.votesAgainst)
    }

    @Test
    fun `the resolution body is carried through as bbcode`() {
        val assembly = NsXml.decodeFromString(
            AssemblyDto.serializer(),
            """<WA council="1"><RESOLUTION><NAME>X</NAME><DESC>[b]Be it resolved[/b]</DESC></RESOLUTION></WA>""",
        )

        assertEquals("[b]Be it resolved[/b]", assembly.resolution.body)
    }

    @Test
    fun `vote share is computed without dividing by zero`() {
        val fresh = Resolution("Fresh", "", "", votesFor = 0, votesAgainst = 0, body = emptyList())
        val split = Resolution("Split", "", "", votesFor = 3, votesAgainst = 1, body = emptyList())

        assertEquals(0f, fresh.supportFraction, 0.0001f)
        assertEquals(0.75f, split.supportFraction, 0.0001f)
        assertEquals(4, split.totalVotes)
    }

    @Test
    fun `a missing resolution element is treated as nothing at vote`() {
        val assembly = NsXml.decodeFromString(
            AssemblyDto.serializer(),
            """<WA council="2"><NUMNATIONS>14575</NUMNATIONS></WA>""",
        )

        assertFalse(assembly.resolution.isAtVote)
    }
}
