package dev.cazh0.civily.data.issues

import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.data.NsXml
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec §5 makes coverage of parsing mandatory.
 *
 * The option ids matter more than anything else here: `c=issue` takes the API's own option
 * number, they count from zero, and a nation that enacts the wrong one cannot take it back.
 */
class IssueDtoTest {

    @Test
    fun `parses issues and their options`() {
        val page = NsXml.decodeFromString(
            IssuesPageDto.serializer(),
            """
            <NATION id="testlandia">
            <ISSUES>
            <ISSUE id="111">
                <TITLE>Bin There, Done That</TITLE>
                <TEXT>Rubbish is piling up.</TEXT>
                <PIC1>b21</PIC1>
                <PIC2>g1</PIC2>
                <OPTION id="0">Collect it weekly.</OPTION>
                <OPTION id="1">Let the market sort it out.</OPTION>
            </ISSUE>
            <ISSUE id="222">
                <TITLE>A Second Issue</TITLE>
                <TEXT>Something else.</TEXT>
                <OPTION id="0">Only one option.</OPTION>
            </ISSUE>
            </ISSUES>
            </NATION>
            """.trimIndent(),
        )

        val issues = page.issues.issues
        assertEquals(2, issues.size)

        val first = issues[0]
        assertEquals(111, first.id)
        assertEquals("Bin There, Done That", first.title)
        assertEquals("Rubbish is piling up.", first.text)
        assertEquals("b21", first.primaryImageId)
        assertEquals("g1", first.secondaryImageId)
        assertEquals(
            "https://www.nationstates.net/images/newspaper/g1-2.jpg",
            NsUrl.newspaperImage(first.secondaryImageId, 2),
        )
        assertEquals(
            "https://www.nationstates.net/images/newspaper/g1-2.jpg",
            NsUrl.newspaperImage("/images/newspaper/g1-2.jpg", 2),
        )
        assertEquals(listOf(0, 1), first.options.map { it.id })
        assertEquals("Collect it weekly.", first.options[0].text)
        assertEquals(222, issues[1].id)
    }

    @Test
    fun `option ids are read from the API and not inferred from position`() {
        // NationStates leaves gaps when options are withdrawn. Numbering by index would enact
        // the wrong legislation, permanently.
        val page = NsXml.decodeFromString(
            IssuesPageDto.serializer(),
            """
            <NATION id="testlandia"><ISSUES><ISSUE id="1">
                <OPTION id="3">Third</OPTION>
                <OPTION id="7">Seventh</OPTION>
            </ISSUE></ISSUES></NATION>
            """.trimIndent(),
        )

        assertEquals(listOf(3, 7), page.issues.issues.single().options.map { it.id })
    }

    @Test
    fun `a nation with no issues parses to an empty list`() {
        val page = NsXml.decodeFromString(
            IssuesPageDto.serializer(),
            """<NATION id="testlandia"><ISSUES></ISSUES></NATION>""",
        )

        assertEquals(emptyList<IssueDto>(), page.issues.issues)
    }
}
