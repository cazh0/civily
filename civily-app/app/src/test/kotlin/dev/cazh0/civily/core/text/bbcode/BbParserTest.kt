package dev.cazh0.civily.core.text.bbcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec §5 makes coverage of BBCode mandatory.
 *
 * Most of these are malformed on purpose. NationStates content is written by thousands of
 * people in a plain textarea, so unclosed tags, stray closes and tags this app has never
 * heard of are the normal case, not the exception.
 */
class BbParserTest {

    private fun textOf(blocks: List<BbBlock>): String = buildString {
        blocks.forEach { block ->
            when (block) {
                is BbBlock.Paragraph -> block.spans.forEach { append(it.text) }
                is BbBlock.Quote -> append(textOf(block.blocks))
                is BbBlock.Spoiler -> append(textOf(block.blocks))
                is BbBlock.Bullets -> block.items.forEach { append(textOf(it)) }
                is BbBlock.Preformatted -> append(block.text)
                BbBlock.Rule -> Unit
            }
        }
    }

    private fun spansOf(blocks: List<BbBlock>): List<BbSpan> =
        blocks.filterIsInstance<BbBlock.Paragraph>().flatMap { it.spans }

    // ------------------------------------------------------------ basics

    @Test
    fun `empty input yields no blocks`() {
        assertEquals(emptyList<BbBlock>(), BbParser.parse(""))
    }

    @Test
    fun `plain text survives intact`() {
        assertEquals("Hello there.", textOf(BbParser.parse("Hello there.")))
    }

    @Test
    fun `bold is applied and the markup disappears`() {
        val spans = spansOf(BbParser.parse("a [b]bold[/b] z"))

        assertEquals("a bold z", spans.joinToString("") { it.text })
        assertEquals(listOf(false, true, false), spans.map { it.style.bold })
    }

    @Test
    fun `nested styles combine`() {
        val spans = spansOf(BbParser.parse("[b]bold [i]both[/i][/b]"))
        val both = spans.single { it.text == "both" }

        assertTrue(both.style.bold)
        assertTrue(both.style.italic)
    }

    @Test
    fun `closing the outer tag first still restores cleanly`() {
        // Crossed tags are invalid but common. Nothing may be lost and nothing may leak.
        val blocks = BbParser.parse("[b]one [i]two[/b] three[/i]")

        assertEquals("one two three", textOf(blocks))
    }

    // ------------------------------------------------------------ malformed

    @Test
    fun `an unclosed tag closes at the end of input`() {
        val spans = spansOf(BbParser.parse("[b]never closed"))

        assertEquals("never closed", spans.joinToString("") { it.text })
        assertTrue(spans.single().style.bold)
    }

    @Test
    fun `a stray closing tag is ignored`() {
        assertEquals("orphan", textOf(BbParser.parse("orphan[/b]")))
    }

    @Test
    fun `an unknown tag is dropped but its contents are kept`() {
        assertEquals("cell", textOf(BbParser.parse("[table][tr][td]cell[/td][/tr][/table]")))
    }

    @Test
    fun `a bracket that is not a tag stays literal`() {
        assertEquals("see [1] and [not a tag]", textOf(BbParser.parse("see [1] and [not a tag]")))
    }

    @Test
    fun `an unclosed nation tag does not swallow the rest of the post`() {
        // The tag is dropped and the words stay words. Turning the whole sentence into a link
        // to a nation that cannot exist would lose the sentence and mislead the reader.
        val blocks = BbParser.parse("[nation]testlandia and then more text")

        assertEquals("testlandia and then more text", textOf(blocks))
        assertTrue(spansOf(blocks).all { it is BbSpan.Plain })
    }

    // ------------------------------------------------------------ html

    @Test
    fun `html italics are honoured rather than printed`() {
        // Seen live: issue text carries HTML as well as BBCode, and the tags were reaching
        // the reader as "<i>The War of the Planets</i>".
        val spans = spansOf(BbParser.parse("his movie, <i>The War of the Planets</i>."))

        assertEquals("his movie, The War of the Planets.", spans.joinToString("") { it.text })
        assertTrue(spans.single { it.text == "The War of the Planets" }.style.italic)
    }

    @Test
    fun `html aliases map onto the same styles`() {
        assertTrue(spansOf(BbParser.parse("<em>a</em>")).single().style.italic)
        assertTrue(spansOf(BbParser.parse("<strong>a</strong>")).single().style.bold)
        assertTrue(spansOf(BbParser.parse("<del>a</del>")).single().style.strikethrough)
    }

    @Test
    fun `a break becomes a line break`() {
        assertEquals("one\ntwo", textOf(BbParser.parse("one<br>two")))
        assertEquals("one\ntwo", textOf(BbParser.parse("one<br />two")))
    }

    @Test
    fun `an unknown html tag is dropped but its contents are kept`() {
        assertEquals("cell", textOf(BbParser.parse("<table><tr><td>cell</td></tr></table>")))
    }

    @Test
    fun `a tag with attributes is still recognised`() {
        assertTrue(spansOf(BbParser.parse("""<i class="x">a</i>""")).single().style.italic)
    }

    @Test
    fun `a less-than in prose is not a tag`() {
        assertEquals("5 < 6 and a<b is fine", textOf(BbParser.parse("5 < 6 and a<b is fine")))
    }

    // ------------------------------------------------------------ links

    @Test
    fun `url becomes a web link`() {
        val span = spansOf(BbParser.parse("[url=https://example.com]click[/url]")).single()

        assertEquals("click", span.text)
        assertEquals(BbTarget.Web("https://example.com"), (span as BbSpan.Link).target)
    }

    @Test
    fun `nation becomes an in-app link with a readable label`() {
        val span = spansOf(BbParser.parse("[nation]the_north_pacific[/nation]")).single()

        assertEquals("The North Pacific", span.text)
        assertEquals(BbTarget.Nation("the_north_pacific"), (span as BbSpan.Link).target)
    }

    @Test
    fun `a nation display modifier is not mistaken for the nation`() {
        // [nation=short] asks for a short rendering; the nation is still the body.
        val span = spansOf(BbParser.parse("[nation=short]Testlandia[/nation]")).single()

        assertEquals(BbTarget.Nation("testlandia"), (span as BbSpan.Link).target)
    }

    @Test
    fun `region becomes an in-app link`() {
        val span = spansOf(BbParser.parse("[region]Testregionia[/region]")).single()

        assertEquals(BbTarget.Region("testregionia"), (span as BbSpan.Link).target)
    }

    @Test
    fun `styles inside a link are kept`() {
        val span = spansOf(BbParser.parse("[url=https://x.test][b]bold link[/b][/url]")).single()

        assertTrue(span is BbSpan.Link)
        assertTrue(span.style.bold)
    }

    // ------------------------------------------------------------ blocks

    @Test
    fun `a quote captures its author and body`() {
        val quote = BbParser.parse("[quote=Testlandia;12345]quoted words[/quote]")
            .filterIsInstance<BbBlock.Quote>()
            .single()

        assertEquals("Testlandia", quote.author)
        assertEquals("quoted words", textOf(quote.blocks))
    }

    @Test
    fun `quotes nest`() {
        val outer = BbParser.parse("[quote=A]outer [quote=B]inner[/quote] tail[/quote]")
            .filterIsInstance<BbBlock.Quote>()
            .single()
        val inner = outer.blocks.filterIsInstance<BbBlock.Quote>().single()

        assertEquals("A", outer.author)
        assertEquals("B", inner.author)
        assertEquals("inner", textOf(inner.blocks))
    }

    @Test
    fun `text around a block becomes separate paragraphs`() {
        val blocks = BbParser.parse("before[hr]after")

        assertEquals(3, blocks.size)
        assertTrue(blocks[1] is BbBlock.Rule)
        assertEquals("before", textOf(listOf(blocks[0])))
        assertEquals("after", textOf(listOf(blocks[2])))
    }

    @Test
    fun `blank lines around a block do not survive as empty space`() {
        // Authors pad around a quote. Those newlines land at the edges of the neighbouring
        // paragraphs and stack on the layout's own spacing, opening a hole mid-post.
        val blocks = BbParser.parse("before\n\n[quote=A]\n\nquoted\n\n[/quote]\n\nafter")
        val quote = blocks.filterIsInstance<BbBlock.Quote>().single()

        assertEquals("before", textOf(listOf(blocks.first())))
        assertEquals("quoted", textOf(quote.blocks))
        assertEquals("after", textOf(listOf(blocks.last())))
    }

    @Test
    fun `a paragraph of nothing but blank lines is dropped`() {
        assertEquals(emptyList<BbBlock>(), BbParser.parse("\n\n   \n"))
    }

    @Test
    fun `whitespace inside a paragraph is left alone`() {
        assertEquals("one\n\ntwo", textOf(BbParser.parse("one\n\ntwo")))
    }

    @Test
    fun `a spoiler keeps its title`() {
        val spoiler = BbParser.parse("[spoiler=Ending]he dies[/spoiler]")
            .filterIsInstance<BbBlock.Spoiler>()
            .single()

        assertEquals("Ending", spoiler.title)
        assertEquals("he dies", textOf(spoiler.blocks))
    }

    @Test
    fun `list items split on the item tag`() {
        val list = BbParser.parse("[list][*]one[*]two[*]three[/list]")
            .filterIsInstance<BbBlock.Bullets>()
            .single()

        assertEquals(3, list.items.size)
        assertEquals(listOf("one", "two", "three"), list.items.map { textOf(it) })
        assertEquals(false, list.ordered)
    }

    @Test
    fun `a numbered list is marked ordered`() {
        val list = BbParser.parse("[list=1][*]one[*]two[/list]")
            .filterIsInstance<BbBlock.Bullets>()
            .single()

        assertTrue(list.ordered)
        assertEquals(2, list.items.size)
    }

    @Test
    fun `preformatted text keeps its markup verbatim`() {
        val pre = BbParser.parse("[pre]raw [b]not bold[/b]  spaced[/pre]")
            .filterIsInstance<BbBlock.Preformatted>()
            .single()

        assertEquals("raw [b]not bold[/b]  spaced", pre.text)
    }

    @Test
    fun `colour is carried through as written`() {
        val span = spansOf(BbParser.parse("[color=#ff0000]red[/color]")).single()

        assertEquals("#ff0000", span.style.color)
    }

    @Test
    fun `british spelling of colour works too`() {
        val span = spansOf(BbParser.parse("[colour=red]red[/colour]")).single()

        assertEquals("red", span.style.color)
    }

    @Test
    fun `tag names are case insensitive`() {
        val span = spansOf(BbParser.parse("[B]shout[/B]")).single()

        assertTrue(span.style.bold)
    }
}
