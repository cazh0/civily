package dev.cazh0.stately.core.text.bbcode

/**
 * What NationStates BBCode becomes once parsed.
 *
 * Why a tree and not a string of HTML: the legacy client ran roughly thirty regular
 * expressions over the raw markup, one per tag. Regular expressions cannot match nested
 * delimiters, so `[b]bold [i]both[/i][/b]` and any unclosed tag produced wrong output or
 * leaked raw markup onto the screen. A tree is parsed once, is inspectable, and is what makes
 * the spec §5 requirement to test this code achievable at all.
 */
sealed interface BbBlock {
    data class Paragraph(val spans: List<BbSpan>) : BbBlock

    /** `[quote]`, or `[quote=author;postid]` where only the author is shown. */
    data class Quote(val author: String?, val blocks: List<BbBlock>) : BbBlock

    /** `[spoiler]` or `[spoiler=title]`. Rendered collapsed — that is the point of it. */
    data class Spoiler(val title: String?, val blocks: List<BbBlock>) : BbBlock

    data class Bullets(val items: List<List<BbBlock>>, val ordered: Boolean) : BbBlock

    /** `[pre]`. Whitespace is content here, so it is kept exactly. */
    data class Preformatted(val text: String) : BbBlock

    data object Rule : BbBlock
}

sealed interface BbSpan {
    val text: String
    val style: BbStyle

    data class Plain(
        override val text: String,
        override val style: BbStyle = BbStyle(),
    ) : BbSpan

    data class Link(
        override val text: String,
        val target: BbTarget,
        override val style: BbStyle = BbStyle(),
    ) : BbSpan
}

/** Same span, different words. Used when trimming whitespace at the edge of a block. */
fun BbSpan.withText(text: String): BbSpan = when (this) {
    is BbSpan.Plain -> copy(text = text)
    is BbSpan.Link -> copy(text = text)
}

/**
 * Inline formatting in force at a point in the text.
 *
 * [color] stays a string until render time because it is *content*, not design: it is whatever
 * the post's author typed. Resolving it here would put a colour value in the parser, and an
 * unresolvable one has to degrade to "no colour" rather than fail.
 */
data class BbStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val superscript: Boolean = false,
    val subscript: Boolean = false,
    val color: String? = null,
)

/** Where a link goes. Nation and region links stay inside the app; the web does not. */
sealed interface BbTarget {
    data class Nation(val id: String) : BbTarget
    data class Region(val id: String) : BbTarget
    data class Web(val url: String) : BbTarget
}
