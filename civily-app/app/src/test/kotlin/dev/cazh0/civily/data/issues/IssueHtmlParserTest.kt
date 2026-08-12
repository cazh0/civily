package dev.cazh0.civily.data.issues

import dev.cazh0.civily.core.result.Outcome
import dev.cazh0.civily.core.result.CivilyError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueHtmlParserTest {

    @Test
    fun `parses the tokenised enact form for the chosen option`() {
        val result = IssueHtmlParser.parseEnactForm(
            html = dilemmaHtml,
            nationId = "ecstasiae",
            issueId = 633,
            optionId = 2,
        )

        assertTrue(result is Outcome.Success)
        val form = (result as Outcome.Success).value
        assertEquals("/page=enact_dilemma/dilemma=633", form.action)
        assertEquals("ecstasiae", form.fields["nation"])
        assertEquals("633", form.fields["id"])
        assertEquals("enact", form.fields["kind"])
        assertEquals("token-633", form.fields["asform_token"])
        assertEquals("1", form.fields["choice-2"])
    }

    @Test
    fun `enact form missing its token is malformed`() {
        val html = """
            <body id="loggedin" data-nname="ecstasiae">
            <form action="/page=enact_dilemma/dilemma=633" method="POST">
              <input type="hidden" name="nation" value="ecstasiae">
              <input type="hidden" name="id" value="633">
              <input type="hidden" name="kind" value="enact">
              <button type="submit" name="choice-2" value="1">Accept</button>
            </form></body>
        """.trimIndent()

        assertTrue(
            IssueHtmlParser.parseEnactForm(
                html = html,
                nationId = "ecstasiae",
                issueId = 633,
                optionId = 2,
            ) is Outcome.Failure,
        )
    }

    @Test
    fun `enact form for another issue is malformed`() {
        assertTrue(
            IssueHtmlParser.parseEnactForm(
                html = dilemmaHtml,
                nationId = "ecstasiae",
                issueId = 634,
                optionId = 2,
            ) is Outcome.Failure,
        )
    }

    @Test
    fun `logged out issue page is unauthorized`() {
        val result = IssueHtmlParser.parseEnactForm(
            html = "<body><form action=\"/page=enact_dilemma/dilemma=633\" method=\"POST\"></form>",
            nationId = "ecstasiae",
            issueId = 633,
            optionId = 2,
        )

        assertEquals(Outcome.Failure(CivilyError.Unauthorized), result)
    }

    @Test
    fun `parses result text trends and every newspaper from legislation papers`() {
        val result = IssueHtmlParser.parseResult(aftermathHtml)

        assertTrue(result is Outcome.Success)
        val page = (result as Outcome.Success).value
        assertEquals(
            "Motorists must pay to enter inner-cities during peak hours.",
            page.description,
        )
        assertEquals(4, page.headlines.size)
        assertEquals(
            listOf(
                "Public Transport Has Ecstasiae On The Move",
                "Car Sales Fall, But Never Better Time To Buy, Says Yard",
                "Environment Groups Applaud Government Initiative",
                "Lower Taxes Put Spring In Step, Money In Pocket",
            ),
            page.headlines.map { it.text },
        )
        assertEquals(
            listOf(
                listOf(
                    "https://www.nationstates.net/images/newspaper/i16-1.jpg",
                    "https://www.nationstates.net/images/newspaper/p15-2.jpg",
                ),
                listOf(
                    "https://www.nationstates.net/images/newspaper/l7-1.jpg",
                    "https://www.nationstates.net/images/newspaper/i7-2.jpg",
                ),
                listOf(
                    "https://www.nationstates.net/images/newspaper/p8-1.jpg",
                    "https://www.nationstates.net/images/newspaper/l18-2.jpg",
                ),
                listOf(
                    "https://www.nationstates.net/images/newspaper/y108-1.jpg",
                    "https://www.nationstates.net/images/newspaper/p7-2.jpg",
                ),
            ),
            page.headlines.map { it.imageUrls },
        )
        assertEquals(47, page.rankings.size)
        assertEquals(57, page.rankings.first().scaleId)
        assertEquals(13.4, page.rankings.first().percentChange, 0.000001)
        // A fall is marked only by the `wcr` class; the page prints the number unsigned.
        assertEquals(-0.51, page.rankings.first { it.scaleId == 89 }.percentChange, 0.000001)
    }

    @Test
    fun `a paper still reads when the page prints one cutout`() {
        val html = """
            <h5>The Talking Point</h5><p>Done.</p><h5>Recent Headlines</h5>
            <div class="legislation-papers">
              <div class="dilemmapaper"><div class="dpaper4"><p>Incomplete</p></div>
                <div class="dpaper5box">
                  <img src="/images/newspaper/dpaper5.png" class="dpaperslice">
                  <img src="/images/newspaper/i16-1.jpg" class="dpaperpic dpaperpic1">
                </div>
              </div>
            </div>
        """.trimIndent()

        val page = (IssueHtmlParser.parseResult(html) as Outcome.Success).value
        assertEquals(
            listOf("https://www.nationstates.net/images/newspaper/i16-1.jpg"),
            page.headlines.single().imageUrls,
        )
    }

    @Test
    fun `cutouts are read off the page rather than rebuilt from a slot pattern`() {
        val html = """
            <h5>The Talking Point</h5><p>Done.</p><h5>Recent Headlines</h5>
            <div class="legislation-papers">
              <div class="dilemmapaper"><div class="dpaper4"><p>Named Its Own Way</p></div>
                <div class="dpaper5box">
                  <img src="/images/newspaper/dpaper5.png" class="dpaperslice">
                  <img src="/images/newspaper/y108.jpg" class="dpaperpic dpaperpic1">
                  <img src="/images/newspaper/z3-7.jpg" class="dpaperpic dpaperpic2">
                </div>
              </div>
            </div>
        """.trimIndent()

        val page = (IssueHtmlParser.parseResult(html) as Outcome.Success).value
        assertEquals(
            listOf(
                "https://www.nationstates.net/images/newspaper/y108.jpg",
                "https://www.nationstates.net/images/newspaper/z3-7.jpg",
            ),
            page.headlines.single().imageUrls,
        )
    }

    @Test
    fun `missing legislation papers is a malformed result`() {
        assertTrue(
            IssueHtmlParser.parseResult("<h5>The Talking Point</h5><p>Done.</p>") is
                Outcome.Failure,
        )
    }

    @Test
    fun `a paper without a headline is malformed`() {
        val html = """
            <h5>The Talking Point</h5><p>Done.</p><h5>Recent Headlines</h5>
            <div class="legislation-papers">
              <div class="dilemmapaper">
                <div class="dpaper5box">
                  <img src="/images/newspaper/i16-1.jpg" class="dpaperpic dpaperpic1">
                </div>
              </div>
            </div>
        """.trimIndent()

        assertTrue(IssueHtmlParser.parseResult(html) is Outcome.Failure)
    }

    private val dilemmaHtml = """
        <body id="loggedin" data-nname="ecstasiae">
        <div class="dilemma"><h5>The Issue</h5>
        <form action="/page=enact_dilemma/dilemma=633" method="POST">
          <input type="hidden" name="nation" value="ecstasiae">
          <input type="hidden" name="id" value="633">
          <input type="hidden" name="kind" value="enact">
          <ol class="diloptions">
            <li><p>First option.</p>
              <p class="dilemmaaccept"><button type="submit" name="choice-0" value="1">Accept</button>
            <li><p>Chosen option.</p>
              <p class="dilemmaaccept"><button type="submit" name="choice-2" value="1">Accept</button>
          </ol>
          <input type="hidden" name="asform_token" value="token-633">
        </form>
        <form action="page=dilemmas/dismiss=633" method="POST">
          <button type="submit" name="choice--1" value="1">Dismiss This Issue</button>
        </form></div></body>
    """.trimIndent()

    /**
     * The page NationStates actually returned when this nation enacted issue 633, kept whole.
     *
     * Whole because the parser's job is to find four papers inside a full site page — nav,
     * sidebar, login form, trend list and all — and a fixture trimmed to the interesting part
     * tests a page the server never sends.
     */
    private val aftermathHtml: String =
        checkNotNull(javaClass.getResourceAsStream(AFTERMATH)) { "missing fixture $AFTERMATH" }
            .use { it.reader().readText() }

    private companion object {
        const val AFTERMATH = "/issues/aftermath_enacted.html"
    }
}
