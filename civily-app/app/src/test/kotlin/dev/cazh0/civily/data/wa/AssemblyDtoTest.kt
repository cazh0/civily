package dev.cazh0.civily.data.wa

import dev.cazh0.civily.data.NsXml
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
            <WA council="3">
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
            <WA council="3">
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
            """<WA council="3"><RESOLUTION><NAME>X</NAME><DESC>[b]Be it resolved[/b]</DESC></RESOLUTION></WA>""",
        )

        assertEquals("[b]Be it resolved[/b]", assembly.resolution.body)
    }

    @Test
    fun `vote share is computed without dividing by zero`() {
        val fresh = Resolution(
            "Fresh",
            "",
            "",
            votesFor = 0,
            votesAgainst = 0,
            voteHistory = emptyList(),
            voteBreakdown = VoteBreakdown(0, 0, 0, 0),
            body = emptyList(),
        )
        val split = Resolution(
            "Split",
            "",
            "",
            votesFor = 3,
            votesAgainst = 1,
            voteHistory = emptyList(),
            voteBreakdown = VoteBreakdown(3, 1, 0, 0),
            body = emptyList(),
        )

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

    @Test
    fun `current general assembly uses council three`() {
        assertEquals(3, Council.GeneralAssembly.id)
        assertEquals(2, Council.SecurityCouncil.id)
    }

    @Test
    fun `vote history and delegate votes parse`() {
        val assembly = NsXml.decodeFromString(
            AssemblyDto.serializer(),
            """
            <WA council="3"><RESOLUTION><NAME>X</NAME>
                <VOTE_TRACK_FOR><N>4</N><N>7</N></VOTE_TRACK_FOR>
                <VOTE_TRACK_AGAINST><N>2</N><N>3</N></VOTE_TRACK_AGAINST>
                <DELVOTES_FOR><DELEGATE><NATION>testlandia</NATION><VOTES>5</VOTES></DELEGATE></DELVOTES_FOR>
                <DELVOTES_AGAINST><DELEGATE><NATION>examplestan</NATION><VOTES>8</VOTES></DELEGATE></DELVOTES_AGAINST>
            </RESOLUTION></WA>
            """.trimIndent(),
        )

        val resolution = assembly.resolution
        assertEquals(listOf(4, 7), resolution.voteTrackFor.points.map(VotePointDto::votes))
        assertEquals(listOf(2, 3), resolution.voteTrackAgainst.points.map(VotePointDto::votes))
        assertEquals("testlandia", resolution.delegateVotesFor.delegates.single().nationId)
        assertEquals(5, resolution.delegateVotesFor.delegates.single().votes)
        assertEquals("examplestan", resolution.delegateVotesAgainst.delegates.single().nationId)
        assertEquals(8, resolution.delegateVotesAgainst.delegates.single().votes)
    }

    @Test
    fun `missing vote detail remains empty`() {
        val assembly = NsXml.decodeFromString(
            AssemblyDto.serializer(),
            """<WA council="3"><RESOLUTION><NAME>X</NAME></RESOLUTION></WA>""",
        )

        val resolution = assembly.resolution
        assertTrue(resolution.voteTrackFor.points.isEmpty())
        assertTrue(resolution.voteTrackAgainst.points.isEmpty())
        assertTrue(resolution.delegateVotesFor.delegates.isEmpty())
        assertTrue(resolution.delegateVotesAgainst.delegates.isEmpty())
    }

    @Test
    fun `history ends at live tally and breakdown excludes delegate weights`() {
        val assembly = AssemblyDto(
            resolution = ResolutionDto(
                name = "X",
                votesFor = 11,
                votesAgainst = 13,
                voteTrackFor = VoteTrackDto(listOf(VotePointDto(4), VotePointDto(7))),
                voteTrackAgainst = VoteTrackDto(listOf(VotePointDto(2), VotePointDto(3))),
                delegateVotesFor = DelegateVotesDto(listOf(DelegateVoteDto("testlandia", 5))),
                delegateVotesAgainst = DelegateVotesDto(listOf(DelegateVoteDto("examplestan", 8))),
            ),
        )

        val resolution = assembly.toAssembly().resolution!!
        assertEquals(listOf(VoteTally(4, 2), VoteTally(7, 3), VoteTally(11, 13)), resolution.voteHistory)
        assertEquals(VoteBreakdown(6, 5, 5, 8), resolution.voteBreakdown)
    }
}
