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
    fun `parses the shards the summary and the feed are built from`() {
        // Verbatim shapes from a live response: GOVTDESC arrives in CDATA and the others do
        // not, HAPPENINGS is a list of EVENT, and FREEDOM is three nested elements.
        val nation = NsXml.decodeFromString(
            NationDto.serializer(),
            """
            <NATION id="testlandia">
                <NAME>Testlandia</NAME>
                <ANIMALTRAIT>frolics freely in the nation's sparkling oceans</ANIMALTRAIT>
                <CAPITAL>Tést City</CAPITAL>
                <LEADER>Violet</LEADER>
                <RELIGION>Neo-Violetism</RELIGION>
                <FREEDOM>
                    <CIVILRIGHTS>Below Average</CIVILRIGHTS>
                    <ECONOMY>Powerhouse</ECONOMY>
                    <POLITICALFREEDOM>Very Good</POLITICALFREEDOM>
                </FREEDOM>
                <NOTABLE>museums and concert halls, and hatred of cheese</NOTABLE>
                <ADMIRABLE>environmentally stunning</ADMIRABLE>
                <SENSIBILITIES>compassionate, democratic</SENSIBILITIES>
                <GOVTDESC><![CDATA[It juggles the competing demands of Education.]]></GOVTDESC>
                <INDUSTRYDESC>The powerhouse Testlandian economy.</INDUSTRYDESC>
                <CRIME>Crime is totally unknown.</CRIME>
                <HAPPENINGS>
                    <EVENT>
                        <TIMESTAMP>1786137533</TIMESTAMP>
                        <TEXT>@@testlandia@@ added the tag &quot;Anti-GenAI&quot;.</TEXT>
                    </EVENT>
                    <EVENT>
                        <TIMESTAMP>1785568239</TIMESTAMP>
                        <TEXT>Following new legislation in @@testlandia@@, cheese is banned.</TEXT>
                    </EVENT>
                </HAPPENINGS>
            </NATION>
            """.trimIndent(),
        )

        assertEquals("frolics freely in the nation's sparkling oceans", nation.animalTrait)
        assertEquals("Tést City", nation.capital)
        assertEquals("Violet", nation.leader)
        assertEquals("Neo-Violetism", nation.religion)
        assertEquals("Below Average", nation.freedom.civilRights)
        assertEquals("Powerhouse", nation.freedom.economy)
        assertEquals("Very Good", nation.freedom.politicalFreedom)
        assertEquals("museums and concert halls, and hatred of cheese", nation.notable)
        assertEquals("environmentally stunning", nation.admirable)
        assertEquals("compassionate, democratic", nation.sensibilities)
        assertEquals("It juggles the competing demands of Education.", nation.governmentDescription)
        assertEquals("The powerhouse Testlandian economy.", nation.industryDescription)
        assertEquals("Crime is totally unknown.", nation.crime)

        assertEquals(2, nation.happenings.events.size)
        assertEquals(1786137533L, nation.happenings.events.first().timestamp)
        assertEquals(
            """@@testlandia@@ added the tag "Anti-GenAI".""",
            nation.happenings.events.first().text,
        )
    }

    @Test
    fun `a nation with no feed and no freedom ratings parses to empty rather than failing`() {
        // A brand-new nation has no happenings at all, and a request that drops a shard omits
        // its element entirely. Neither may take the screen down (spec §1.2).
        val nation = NsXml.decodeFromString(
            NationDto.serializer(),
            """<NATION id="testlandia"><NAME>Testlandia</NAME></NATION>""",
        )

        assertEquals(emptyList<EventDto>(), nation.happenings.events)
        assertEquals("", nation.freedom.civilRights)
        assertEquals("", nation.notable)
        assertEquals("", nation.governmentDescription)
        assertEquals(emptyList<PolicyDto>(), nation.policies.policies)
        assertEquals(emptyList<CauseDto>(), nation.deaths.causes)
        assertEquals(emptyList<ScaleDto>(), nation.census.scales)
        assertEquals(0.0, nation.govt.education, 0.0)
        assertEquals(0.0, nation.sectors.government, 0.0)
        assertEquals(0L, nation.gdp)
    }

    @Test
    fun `parses the shards the government, economy and people tabs are built from`() {
        // Verbatim shapes from a live response. Note what the element names are *not*: COMMERCE
        // is the industry budget, SOCIALEQUALITY is social policy, and a CAUSE carries its name
        // in an attribute with the share as the element's text.
        val nation = NsXml.decodeFromString(
            NationDto.serializer(),
            """
            <NATION id="testlandia">
                <TAX>87.0</TAX>
                <GOVTPRIORITY>the Environment</GOVTPRIORITY>
                <GDP>3315350844221109</GDP>
                <INCOME>66427</INCOME>
                <RICHEST>79880</RICHEST>
                <POOREST>54016</POOREST>
                <MAJORINDUSTRY>Information Technology</MAJORINDUSTRY>
                <DEMONYM2>Testlandian</DEMONYM2>
                <ISSUES_ANSWERED>158</ISSUES_ANSWERED>
                <ENDORSEMENTS>nationalist_gold_union,nastres_oguan</ENDORSEMENTS>
                <GOVT>
                    <ADMINISTRATION>4.0</ADMINISTRATION>
                    <DEFENCE>12.1</DEFENCE>
                    <EDUCATION>20.9</EDUCATION>
                    <ENVIRONMENT>23.3</ENVIRONMENT>
                    <HEALTHCARE>22.6</HEALTHCARE>
                    <COMMERCE>4.7</COMMERCE>
                    <INTERNATIONALAID>0.0</INTERNATIONALAID>
                    <LAWANDORDER>6.8</LAWANDORDER>
                    <PUBLICTRANSPORT>3.3</PUBLICTRANSPORT>
                    <SOCIALEQUALITY>0.4</SOCIALEQUALITY>
                    <SPIRITUALITY>0.0</SPIRITUALITY>
                    <WELFARE>1.8</WELFARE>
                </GOVT>
                <SECTORS>
                    <BLACKMARKET>0.42</BLACKMARKET>
                    <GOVERNMENT>92.16</GOVERNMENT>
                    <INDUSTRY>5.44</INDUSTRY>
                    <PUBLIC>1.98</PUBLIC>
                </SECTORS>
                <DEATHS>
                    <CAUSE type="Lost in Wilderness">7.2</CAUSE>
                    <CAUSE type="Old Age">92.7</CAUSE>
                </DEATHS>
            </NATION>
            """.trimIndent(),
        )

        assertEquals(87.0, nation.tax, 0.0)
        assertEquals("the Environment", nation.govtPriority)
        // Past Int.MAX_VALUE, which is why this field is a Long.
        assertEquals(3_315_350_844_221_109L, nation.gdp)
        assertEquals(66_427L, nation.income)
        assertEquals(79_880L, nation.richest)
        assertEquals(54_016L, nation.poorest)
        assertEquals("Information Technology", nation.majorIndustry)
        assertEquals("Testlandian", nation.person)
        assertEquals(158, nation.issuesAnswered)
        assertEquals("nationalist_gold_union,nastres_oguan", nation.endorsements)

        assertEquals(23.3, nation.govt.environment, 0.0)
        assertEquals(4.7, nation.govt.commerce, 0.0)
        assertEquals(0.4, nation.govt.socialEquality, 0.0)

        assertEquals(92.16, nation.sectors.government, 0.0)
        assertEquals(5.44, nation.sectors.privateIndustry, 0.0)
        assertEquals(1.98, nation.sectors.stateOwned, 0.0)
        assertEquals(0.42, nation.sectors.blackMarket, 0.0)

        assertEquals(2, nation.deaths.causes.size)
        assertEquals("Lost in Wilderness", nation.deaths.causes.first().type)
        assertEquals(7.2, nation.deaths.causes.first().percent, 0.0)
    }

    @Test
    fun `parses policies and every census scale`() {
        val nation = NsXml.decodeFromString(
            NationDto.serializer(),
            """
            <NATION id="testlandia">
                <POLICIES>
                    <POLICY>
                        <NAME>Devolution</NAME>
                        <PIC>t64</PIC>
                        <CAT>Government</CAT>
                        <DESC>Government power is substantially delegated.</DESC>
                    </POLICY>
                    <POLICY>
                        <NAME>Capital Punishment</NAME>
                        <PIC>d14</PIC>
                        <CAT>Law &amp; Order</CAT>
                        <DESC>Citizens may be executed for crimes.</DESC>
                    </POLICY>
                </POLICIES>
                <CENSUS>
                    <SCALE id="0"><SCORE>50.00</SCORE><RANK>160816</RANK><PRANK>53</PRANK></SCALE>
                    <SCALE id="3">
                        <SCORE>49909000000</SCORE>
                        <RANK>253</RANK>
                        <PRANK>0.09</PRANK>
                    </SCALE>
                </CENSUS>
            </NATION>
            """.trimIndent(),
        )

        assertEquals(2, nation.policies.policies.size)
        val policy = nation.policies.policies.first()
        assertEquals("Devolution", policy.name)
        assertEquals("t64", policy.imageId)
        assertEquals("Government", policy.category)
        // The ampersand arrives escaped and the XML layer unescapes it once.
        assertEquals("Law & Order", nation.policies.policies[1].category)

        assertEquals(2, nation.census.scales.size)
        val population = nation.census.scales[1]
        assertEquals(3, population.id)
        assertEquals(49_909_000_000.0, population.score, 0.0)
        assertEquals(253, population.worldRank)
        // Fractional at the top of a scale, which is why this is not an Int.
        assertEquals(0.09, population.percentile, 0.0)
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
