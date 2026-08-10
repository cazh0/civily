package dev.cazh0.stately.data.issues

import dev.cazh0.stately.data.NsXml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec §5 makes coverage of parsing mandatory.
 *
 * This sample is a real `c=issue` response, trimmed — captured by actually enacting
 * legislation, because the command cannot be rehearsed and the shape was not documented.
 */
class IssueResultDtoTest {

    private val live = """
        <NATION id="bacata">
        <ISSUE id="728" choice="1">
        <OK>1</OK>
        <DESC>ice-filled coffins are ominously positioned in the corner of every hospital ward</DESC>
        <RANKINGS>
        <RANK id="1"><SCORE>74.15</SCORE><CHANGE>-0.29</CHANGE><PCHANGE>-0.389575</PCHANGE></RANK>
        <RANK id="79"><SCORE>244700000000.00</SCORE><CHANGE>11900000000.00</CHANGE><PCHANGE>5.111684</PCHANGE></RANK>
        </RANKINGS>
        <HEADLINES>
        <HEADLINE>Avant-Garde Gallery Popular Yet Confusing</HEADLINE>
        <HEADLINE>12-Page Health Liftout Inside</HEADLINE>
        </HEADLINES>
        </ISSUE>
        </NATION>
    """.trimIndent()

    @Test
    fun `parses a real answer response`() {
        val page = NsXml.decodeFromString(IssueResultPageDto.serializer(), live)
        val issue = page.issue

        assertEquals(728, issue.id)
        assertEquals(1, issue.choice)
        assertEquals(1, issue.ok)
        assertTrue(issue.description.startsWith("ice-filled coffins"))
    }

    @Test
    fun `carries every ranking with its scale id and signed change`() {
        val ranks = NsXml.decodeFromString(IssueResultPageDto.serializer(), live)
            .issue.rankings.ranks

        assertEquals(listOf(1, 79), ranks.map { it.id })
        assertEquals(-0.389575, ranks[0].percentChange, 0.000001)
        assertEquals(5.111684, ranks[1].percentChange, 0.000001)
        assertEquals(244700000000.00, ranks[1].score, 0.01)
    }

    @Test
    fun `carries the headlines in order`() {
        val headlines = NsXml.decodeFromString(IssueResultPageDto.serializer(), live)
            .issue.headlines.headlines.map { it.text }

        assertEquals(
            listOf("Avant-Garde Gallery Popular Yet Confusing", "12-Page Health Liftout Inside"),
            headlines,
        )
    }

    @Test
    fun `a result with no rankings or headlines still parses`() {
        val page = NsXml.decodeFromString(
            IssueResultPageDto.serializer(),
            """<NATION id="x"><ISSUE id="1" choice="0"><OK>1</OK><DESC>nothing much</DESC></ISSUE></NATION>""",
        )

        assertEquals("nothing much", page.issue.description)
        assertEquals(emptyList<RankDto>(), page.issue.rankings.ranks)
        assertEquals(emptyList<HeadlineDto>(), page.issue.headlines.headlines)
    }
}
