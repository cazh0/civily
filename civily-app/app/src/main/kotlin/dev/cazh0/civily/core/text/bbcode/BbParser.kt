package dev.cazh0.civily.core.text.bbcode

import dev.cazh0.civily.core.text.HtmlEntities
import dev.cazh0.civily.core.text.NsId
import java.util.Locale

/**
 * Turns NationStates BBCode into [BbBlock]s.
 *
 * Three properties matter more than tag coverage, because this input is written by thousands
 * of strangers and a great deal of it is malformed:
 *
 *  1. **It never throws.** Every path returns blocks.
 *  2. **It never loses text.** An unclosed, mismatched or unknown tag costs the markup, never
 *     the words inside it.
 *  3. **It never shows markup.** A tag this parser does not implement is dropped, not printed.
 *     Users should not see `[table]` on screen.
 *
 * Unclosed tags are closed implicitly at the end of input; a stray closing tag is ignored.
 */
object BbParser {

    fun parse(source: String): List<BbBlock> {
        if (source.isEmpty()) return emptyList()
        return Reader(tokenize(source, happenings = false)).readBlocks()
    }

    /**
     * The same parser, reading one line of the happenings feed.
     *
     * Happenings are a second dialect of the same content: no BBCode, but nations and regions
     * arrive delimited — `@@testlandia@@ lodged a message on the %%testregionia%% Regional
     * Message Board.` They become the same links `[nation]` and `[region]` produce, so a
     * happening renders through the one renderer and reaches the screen with no markup in it.
     *
     * Why a separate entry point rather than always recognising the delimiters: a factbook is
     * allowed to contain `%%`, and turning an author's per-cent signs into a region link is
     * how you lose half a sentence. The dialect is recognised only where the API speaks it.
     */
    fun parseHappening(source: String): List<BbBlock> {
        if (source.isEmpty()) return emptyList()
        return Reader(tokenize(source, happenings = true)).readBlocks()
    }

    // ---------------------------------------------------------------- tokens

    private sealed interface Token {
        data class Text(val value: String) : Token
        data class Open(val name: String, val value: String?) : Token
        data class Close(val name: String) : Token
    }

    /**
     * Only a well-formed tag is treated as a tag. A bare `[` — as in "[1] see footnote" — is
     * text and stays text.
     */
    private val TAG = Regex("""\[(/?)([a-zA-Z*]+)(?:=([^\]\n]*))?\]""")

    /**
     * NationStates issue text is not pure BBCode: it carries HTML too, so a film title arrives
     * as `<i>The War of the Planets</i>`. Left alone that prints the tags at the reader.
     *
     * A letter must follow the `<` and a `>` must close it, so ordinary prose — "5 < 6", "a <
     * b" — is untouched.
     */
    private val HTML = Regex("""<(/?)([a-zA-Z][a-zA-Z0-9]*)(?:\s[^>\n]*)?>""")

    /** HTML tags that mean the same as a BBCode tag this parser already understands. */
    private val HTML_EQUIVALENTS = mapOf(
        "i" to "i",
        "em" to "i",
        "b" to "b",
        "strong" to "b",
        "u" to "u",
        "s" to "strike",
        "del" to "strike",
        "strike" to "strike",
        "sup" to "sup",
        "sub" to "sub",
    )

    /** `@@nation@@` and `%%region%%`, the happenings feed's two delimiters. */
    private const val NATION_DELIMITER = "@@"
    private const val REGION_DELIMITER = "%%"

    private fun tokenize(source: String, happenings: Boolean): List<Token> {
        val tokens = mutableListOf<Token>()
        val literal = StringBuilder()
        var i = 0

        fun flushLiteral() {
            if (literal.isNotEmpty()) {
                // Why entities are decoded here and not over the whole source first: `&#91;`
                // is a literal `[`, and decoding before tokenizing would turn an author's
                // escaped bracket into a tag they never wrote.
                tokens += Token.Text(HtmlEntities.decode(literal.toString()))
                literal.setLength(0)
            }
        }

        while (i < source.length) {
            val delimited = if (happenings) delimitedAt(source, i) else null
            if (delimited != null) {
                flushLiteral()
                // Written as the tag it is equivalent to, so the reader below has one case to
                // handle for both dialects — including turning the id into a display name.
                tokens += Token.Open(delimited.tag, null)
                tokens += Token.Text(delimited.id)
                tokens += Token.Close(delimited.tag)
                i = delimited.end
                continue
            }

            val bracket = if (source[i] == '[') TAG.matchAt(source, i) else null
            if (bracket != null) {
                flushLiteral()
                val name = bracket.groupValues[2].lowercase(Locale.US)
                val value = if (bracket.groups[3] != null) bracket.groupValues[3] else null
                tokens += if (bracket.groupValues[1] == "/") {
                    Token.Close(name)
                } else {
                    Token.Open(name, value)
                }
                i = bracket.range.last + 1
                continue
            }

            val html = if (source[i] == '<') HTML.matchAt(source, i) else null
            if (html != null) {
                flushLiteral()
                val name = html.groupValues[2].lowercase(Locale.US)
                val closing = html.groupValues[1] == "/"
                when {
                    name == "br" -> tokens += Token.Text("\n")
                    // An HTML tag with a BBCode meaning becomes that tag; anything else is
                    // dropped, the same as an unknown BBCode tag. Markup never reaches a reader.
                    HTML_EQUIVALENTS.containsKey(name) -> {
                        val equivalent = HTML_EQUIVALENTS.getValue(name)
                        tokens += if (closing) {
                            Token.Close(equivalent)
                        } else {
                            Token.Open(equivalent, null)
                        }
                    }
                }
                i = html.range.last + 1
                continue
            }

            literal.append(source[i])
            i++
        }
        flushLiteral()
        return tokens
    }

    private class Delimited(val tag: String, val id: String, val end: Int)

    /**
     * The delimited nation or region starting at [start], or null when there isn't one.
     *
     * A delimiter with no partner is not a delimiter, and neither is one wrapped around
     * anything but an id: "Top 10%%" and a stray `@@` stay exactly as the API wrote them
     * rather than swallowing the rest of the line.
     */
    private fun delimitedAt(source: String, start: Int): Delimited? {
        val (tag, delimiter) = when {
            source.startsWith(NATION_DELIMITER, start) -> "nation" to NATION_DELIMITER
            source.startsWith(REGION_DELIMITER, start) -> "region" to REGION_DELIMITER
            else -> return null
        }
        val idStart = start + delimiter.length
        val close = source.indexOf(delimiter, idStart)
        if (close < 0) return null

        val id = source.substring(idStart, close)
        if (id.isEmpty() || id.any { it.isWhitespace() }) return null
        return Delimited(tag, id, close + delimiter.length)
    }

    // ---------------------------------------------------------------- parsing

    private val INLINE_STYLES = setOf("b", "i", "u", "strike", "sup", "sub", "color", "colour")
    private val BLOCKS = setOf("quote", "spoiler", "list", "pre", "hr")
    private const val ITEM = "*"

    /** State to restore when a tag closes. */
    private class Frame(val tag: String, val style: BbStyle, val target: BbTarget?)

    private class Reader(private val tokens: List<Token>) {
        private var index = 0

        private fun peek(): Token? = tokens.getOrNull(index)

        private fun advance() {
            index++
        }

        /**
         * @param stopOnClose closing tags that end this run without being consumed — the
         *   caller owns them.
         * @param stopOnOpen opening tags that end this run the same way. `[*]` needs this:
         *   a list item is terminated by the *next item*, which is an opening tag.
         */
        fun readBlocks(
            stopOnClose: Set<String> = emptySet(),
            stopOnOpen: Set<String> = emptySet(),
        ): List<BbBlock> {
            val blocks = mutableListOf<BbBlock>()
            val spans = mutableListOf<BbSpan>()
            val frames = ArrayDeque<Frame>()
            var style = BbStyle()
            var target: BbTarget? = null

            fun flushParagraph() {
                // Why the edges are trimmed: authors leave blank lines before and after a
                // quote or a rule, and those become leading and trailing newlines inside the
                // paragraph next to it. Rendered, they stack on top of the layout's own
                // spacing and open a hole in the middle of the post.
                val trimmed = spans.trimEdges()
                if (trimmed.isNotEmpty()) blocks += BbBlock.Paragraph(trimmed)
                spans.clear()
            }

            fun append(text: String, linkTo: BbTarget? = target) {
                if (text.isEmpty()) return
                spans += if (linkTo == null) {
                    BbSpan.Plain(text, style)
                } else {
                    BbSpan.Link(text, linkTo, style)
                }
            }

            while (true) {
                val token = peek() ?: break

                if (token is Token.Close && token.name in stopOnClose) break
                if (token is Token.Open && token.name in stopOnOpen) break

                when (token) {
                    is Token.Text -> {
                        advance()
                        append(token.value)
                    }

                    is Token.Close -> {
                        advance()
                        // Restore the state from before the matching open tag. A close with
                        // no open is dropped rather than allowed to corrupt the stack.
                        val depth = frames.indexOfLast { it.tag == token.name }
                        if (depth >= 0) {
                            var restored: Frame? = null
                            while (frames.size > depth) restored = frames.removeLast()
                            restored?.let {
                                style = it.style
                                target = it.target
                            }
                        }
                    }

                    is Token.Open -> when (token.name) {
                        in BLOCKS -> {
                            advance()
                            flushParagraph()
                            readBlock(token)?.let { blocks += it }
                        }

                        in INLINE_STYLES -> {
                            advance()
                            frames.addLast(Frame(token.name, style, target))
                            style = style.applying(token.name, token.value)
                        }

                        "url" -> {
                            advance()
                            frames.addLast(Frame(token.name, style, target))
                            target = webTarget(token.value) ?: target
                        }

                        "nation", "region" -> {
                            advance()
                            // Any `=value` here is a display modifier — `short`, `noflag` —
                            // not an identifier. The body is the nation or region.
                            val body = readIdentifier(token.name)
                            if (body != null && body.isNotEmpty()) {
                                append(NsId.toName(body), linkTo = targetFor(token.name, body))
                            }
                            // A null body means the tag was never closed. It is dropped and
                            // the following text flows on as ordinary text, because the
                            // alternative — treating the rest of the post as a nation name —
                            // both loses the sentence and links somewhere that cannot exist.
                        }

                        // An unknown tag drops out; whatever it wrapped keeps flowing.
                        else -> advance()
                    }
                }
            }

            flushParagraph()
            return blocks
        }

        /**
         * The body of `[nation]…[/nation]`, which is a bare name.
         *
         * Looks ahead before committing: nothing is consumed unless a matching close is found
         * with only text between. An unterminated tag therefore costs the tag alone, and the
         * text after it is left for the caller to render normally.
         *
         * @return the name, or null when the tag is never closed.
         */
        private fun readIdentifier(tag: String): String? {
            var scan = index
            val builder = StringBuilder()
            while (true) {
                val token = tokens.getOrNull(scan)
                when {
                    token is Token.Close && token.name == tag -> {
                        index = scan + 1
                        return builder.toString().trim()
                    }

                    token is Token.Text -> {
                        builder.append(token.value)
                        scan++
                    }

                    else -> return null
                }
            }
        }

        private fun readBlock(open: Token.Open): BbBlock? = when (open.name) {
            "hr" -> BbBlock.Rule

            "pre" -> BbBlock.Preformatted(readVerbatim("pre"))

            "quote" -> BbBlock.Quote(
                // [quote=author;postid] — the id addresses a post this app cannot open yet.
                author = open.value?.substringBefore(';')?.trim()?.takeIf { it.isNotEmpty() },
                blocks = readBlocks(stopOnClose = setOf("quote")),
            ).also { consumeClose("quote") }

            "spoiler" -> BbBlock.Spoiler(
                title = open.value?.trim()?.takeIf { it.isNotEmpty() },
                blocks = readBlocks(stopOnClose = setOf("spoiler")),
            ).also { consumeClose("spoiler") }

            "list" -> readList(ordered = open.value != null)

            else -> null
        }

        /** `[pre]` keeps its markup and whitespace exactly — that is what preformatted means. */
        private fun readVerbatim(tag: String): String {
            val builder = StringBuilder()
            while (true) {
                val token = peek() ?: break
                if (token is Token.Close && token.name == tag) {
                    advance()
                    break
                }
                advance()
                when (token) {
                    is Token.Text -> builder.append(token.value)
                    is Token.Open ->
                        builder.append(if (token.value == null) "[${token.name}]" else "[${token.name}=${token.value}]")

                    is Token.Close -> builder.append("[/${token.name}]")
                }
            }
            return builder.toString()
        }

        private fun readList(ordered: Boolean): BbBlock.Bullets {
            val items = mutableListOf<List<BbBlock>>()
            while (true) {
                val token = peek() ?: break
                if (token is Token.Close && token.name == "list") {
                    advance()
                    break
                }
                if (token is Token.Open && token.name == ITEM) {
                    advance()
                    items += readBlocks(
                        stopOnClose = setOf("list", ITEM),
                        stopOnOpen = setOf(ITEM),
                    )
                    consumeClose(ITEM)
                    continue
                }
                // Anything before the first [*] is preamble the game itself ignores.
                advance()
            }
            return BbBlock.Bullets(items, ordered)
        }

        private fun consumeClose(tag: String) {
            val token = peek()
            if (token is Token.Close && token.name == tag) advance()
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun BbStyle.applying(tag: String, value: String?): BbStyle = when (tag) {
        "b" -> copy(bold = true)
        "i" -> copy(italic = true)
        "u" -> copy(underline = true)
        "strike" -> copy(strikethrough = true)
        "sup" -> copy(superscript = true, subscript = false)
        "sub" -> copy(subscript = true, superscript = false)
        "color", "colour" -> copy(color = value?.trim()?.takeIf { it.isNotEmpty() })
        else -> this
    }

    /** Drops blank spans at either end and trims the whitespace off what remains. */
    private fun List<BbSpan>.trimEdges(): List<BbSpan> {
        var start = 0
        var end = size
        while (start < end && this[start].text.isBlank()) start++
        while (end > start && this[end - 1].text.isBlank()) end--
        if (start >= end) return emptyList()

        val kept = subList(start, end).toMutableList()
        kept[0] = kept[0].withText(kept[0].text.trimStart())
        kept[kept.lastIndex] = kept[kept.lastIndex].withText(kept[kept.lastIndex].text.trimEnd())
        return kept.filter { it.text.isNotEmpty() }
    }

    private fun webTarget(value: String?): BbTarget? =
        value?.trim()?.takeIf { it.isNotEmpty() }?.let(BbTarget::Web)

    private fun targetFor(tag: String, raw: String): BbTarget? {
        val id = NsId.fromName(raw)
        if (id.isEmpty()) return null
        return if (tag == "nation") BbTarget.Nation(id) else BbTarget.Region(id)
    }
}
