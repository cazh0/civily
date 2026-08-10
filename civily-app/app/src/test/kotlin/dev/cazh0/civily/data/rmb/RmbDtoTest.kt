package dev.cazh0.civily.data.rmb

import dev.cazh0.civily.data.NsXml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec §5 makes coverage of parsing mandatory.
 *
 * The sample is trimmed from a live response: CDATA bodies, a `LIKERS` element this app does
 * not model, an `EMBASSY` element that appears only on cross-region posts, and an `EDITED`
 * element that appears only on edited ones.
 */
class RmbDtoTest {

    @Test
    fun `parses a page of posts`() {
        val page = NsXml.decodeFromString(
            RmbPageDto.serializer(),
            """
            <REGION id="osiris">
            <MESSAGES>
            <POST id="62518993">
                <TIMESTAMP>1786298383</TIMESTAMP>
                <NATION>brightonburg</NATION>
                <STATUS>0</STATUS>
                <LIKES>2</LIKES>
                <LIKERS>new_land_of_cats:yoiiatin</LIKERS>
                <EMBASSY>One big Island</EMBASSY>
                <MESSAGE><![CDATA[Aye tis true]]></MESSAGE>
            </POST>
            <POST id="62521178">
                <TIMESTAMP>1786324754</TIMESTAMP>
                <NATION>new_land_of_cats</NATION>
                <STATUS>0</STATUS>
                <EDITED>1786324858</EDITED>
                <LIKES>3</LIKES>
                <MESSAGE><![CDATA[[b]DISCOMBOBULATOR 3000[/b]]]></MESSAGE>
            </POST>
            </MESSAGES>
            </REGION>
            """.trimIndent(),
        )

        val posts = page.messages.posts
        assertEquals(2, posts.size)

        val first = posts[0]
        assertEquals("62518993", first.id)
        assertEquals("brightonburg", first.nation)
        assertEquals(1786298383L, first.timestamp)
        assertEquals(2, first.likes)
        assertEquals("One big Island", first.embassy)
        assertEquals(0L, first.edited)
        assertEquals("Aye tis true", first.message)

        val second = posts[1]
        assertEquals(1786324858L, second.edited)
        assertEquals("", second.embassy)
        assertTrue(second.message.startsWith("[b]"))
    }

    @Test
    fun `an empty board parses to no posts`() {
        val page = NsXml.decodeFromString(
            RmbPageDto.serializer(),
            """<REGION id="testregionia"><MESSAGES></MESSAGES></REGION>""",
        )

        assertEquals(emptyList<PostDto>(), page.messages.posts)
    }

    @Test
    fun `a suppressed post keeps its status`() {
        val page = NsXml.decodeFromString(
            RmbPageDto.serializer(),
            """
            <REGION id="osiris"><MESSAGES>
            <POST id="1"><NATION>a</NATION><STATUS>1</STATUS><MESSAGE></MESSAGE></POST>
            <POST id="2"><NATION>b</NATION><STATUS>2</STATUS><MESSAGE></MESSAGE></POST>
            </MESSAGES></REGION>
            """.trimIndent(),
        )

        assertEquals(listOf(1, 2), page.messages.posts.map { it.status })
    }
}
