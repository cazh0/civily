package dev.cazh0.civily.data.issues

import dev.cazh0.civily.data.NsXml
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec §5 makes coverage of parsing mandatory, including a missing-element case.
 *
 * The counter on the Issues button is read from `unread`, alongside the four counters the app
 * has nowhere to show — so the test that matters most is the one proving the others are ignored
 * rather than mis-mapped onto the issues count.
 */
class IssueBadgeDtoTest {

    @Test
    fun `parses the issue count and the next issue's instant`() {
        val badge = NsXml.decodeFromString(
            IssueBadgeDto.serializer(),
            """
            <NATION id="testlandia">
            <UNREAD>
                <ISSUES>3</ISSUES>
                <TELEGRAMS>7</TELEGRAMS>
                <NOTICES>2</NOTICES>
                <RMB>19</RMB>
                <WA>1</WA>
                <NEWS>4</NEWS>
            </UNREAD>
            <NEXTISSUETIME>1755000000</NEXTISSUETIME>
            </NATION>
            """.trimIndent(),
        )

        assertEquals(3, badge.unread.issues)
        assertEquals(1755000000L, badge.nextIssueTime)
    }

    @Test
    fun `a nation with nothing waiting and nothing scheduled parses to zeroes`() {
        val badge = NsXml.decodeFromString(
            IssueBadgeDto.serializer(),
            """
            <NATION id="testlandia">
            <UNREAD>
                <TELEGRAMS>0</TELEGRAMS>
            </UNREAD>
            </NATION>
            """.trimIndent(),
        )

        // Zero is the sentinel the repository turns into "no next issue"; it must never come
        // back as a 1970 instant the countdown would report as long overdue.
        assertEquals(0, badge.unread.issues)
        assertEquals(0L, badge.nextIssueTime)
    }
}
