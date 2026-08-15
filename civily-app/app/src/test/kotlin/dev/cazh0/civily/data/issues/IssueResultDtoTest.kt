package dev.cazh0.civily.data.issues

import dev.cazh0.civily.data.NsXml
import dev.cazh0.civily.data.nation.PolicyDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec §5 makes coverage of parsing mandatory.
 *
 * This sample is a real `c=issue` response, trimmed. It carries result text and rankings; the
 * exact newspaper image ids come from the separate aftermath HTML parser.
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
        <HEADLINE>Public Transport Has Ecstasiae On The Move</HEADLINE>
        <HEADLINE>Car Sales Fall, But Never Better Time To Buy, Says Yard</HEADLINE>
        <HEADLINE>Environment Groups Applaud Government Initiative</HEADLINE>
        <HEADLINE>Lower Taxes Put Spring In Step, Money In Pocket</HEADLINE>
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
            .issue.headlines.headlines.map { it.displayText }

        assertEquals(
            listOf(
                "Public Transport Has Ecstasiae On The Move",
                "Car Sales Fall, But Never Better Time To Buy, Says Yard",
                "Environment Groups Applaud Government Initiative",
                "Lower Taxes Put Spring In Step, Money In Pocket",
            ),
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

    @Test
    fun `an answer with no policy change, reclassification or unlock still parses`() {
        val issue = NsXml.decodeFromString(IssueResultPageDto.serializer(), live).issue

        assertEquals(emptyList<PolicyDto>(), issue.newPolicies.policies)
        assertEquals(emptyList<PolicyDto>(), issue.removedPolicies.policies)
        assertEquals(emptyList<BannerDto>(), issue.unlocks.banners)
        assertEquals(
            emptyList<ReclassifyDto>(),
            issue.reclassifications.reclassifications,
        )
    }

    @Test
    fun `carries the rating the answer renamed, type code and all`() {
        val reclassifications =
            NsXml.decodeFromString(IssueResultPageDto.serializer(), enacted)
                .issue.reclassifications.reclassifications

        assertEquals(1, reclassifications.size)
        assertEquals("0", reclassifications[0].type)
        assertEquals("Some", reclassifications[0].from)
        assertEquals("Few", reclassifications[0].to)
    }

    @Test
    fun `carries a policy the answer put on the books`() {
        val policies = NsXml.decodeFromString(IssueResultPageDto.serializer(), enacted)
            .issue.newPolicies.policies

        assertEquals(1, policies.size)
        assertEquals("Corporal Punishment", policies[0].name)
        assertEquals("b39", policies[0].imageId)
        assertEquals("Law & Order", policies[0].category)
        assertEquals(
            "Criminals may be ordered to undergo physical punishment.",
            policies[0].description,
        )
    }

    @Test
    fun `carries the banner the answer unlocked`() {
        val banners = NsXml.decodeFromString(IssueResultPageDto.serializer(), enacted)
            .issue.unlocks.banners

        assertEquals(listOf("s1"), banners.map { it.bannerId })
    }

    @Test
    fun `reads the rest of a whole live answer past the sections it does not model`() {
        val issue = NsXml.decodeFromString(IssueResultPageDto.serializer(), enacted).issue

        assertEquals(87, issue.id)
        assertEquals(2, issue.choice)
        assertEquals(1, issue.ok)
        // The sections arrive out of declaration order and with `OK` and `DESC` between them;
        // everything must still land, or a reordering upstream would be silent data loss.
        assertEquals(54, issue.rankings.ranks.size)
        assertEquals(5, issue.headlines.headlines.size)
        assertEquals(
            "Old Woman Waits In Vain For Help Crossing Road",
            issue.headlines.headlines.last().displayText,
        )
        assertEquals(emptyList<PolicyDto>(), issue.removedPolicies.policies)
    }

    /**
     * The whole answer NationStates returned when this nation enacted issue 87, untrimmed.
     *
     * Whole for the same reason the aftermath HTML fixture is: this is the only response seen
     * that carries `UNLOCKS`, `RECLASSIFICATIONS` and `NEW_POLICIES` at once, and the order and
     * the unmodelled section between them are exactly what the parser has to survive.
     */
    private val enacted: String =
        checkNotNull(javaClass.getResourceAsStream(ENACTED)) { "missing fixture $ENACTED" }
            .use { it.reader().readText() }

    private companion object {
        const val ENACTED = "/issues/enacted_result.xml"
    }
}
