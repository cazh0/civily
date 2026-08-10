package dev.cazh0.civily.core.text

import java.util.Locale

/**
 * Decodes the HTML entities NationStates serves inside BBCode.
 *
 * **Why two passes, verified against the live API rather than assumed.** Every text field comes
 * wrapped in `CDATA`, so the XML layer unescapes nothing at all — what arrives is the site's
 * own HTML source, escaped once more on the way out. Lazarus's factbook is literally
 * `&amp;amp;#43457;` on the wire: one pass leaves `&amp;#43457;` on screen, which is exactly the
 * defect this fixes. The first pass undoes the API's escaping and hands back the HTML the site
 * would serve to a browser; the second reads that HTML the way a browser does. The legacy
 * client does the same thing in two steps — `replace("&amp;amp;", "&amp;")` and then
 * `Html.fromHtml` — which is what has kept it readable for years.
 *
 * Exactly two, never "until it stops changing": a third pass would start eating text an author
 * escaped on purpose, and nothing on the wire needs one.
 *
 * Each pass runs left to right, so a `&amp;` produced mid-pass can never be re-read within that
 * same pass. Anything unrecognised is left exactly as written rather than guessed at.
 */
object HtmlEntities {

    fun decode(input: String): String = decodeOnce(decodeOnce(input))

    private fun decodeOnce(input: String): String {
        if ('&' !in input) return input

        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c != '&') {
                out.append(c)
                i++
                continue
            }
            val end = input.indexOf(';', i + 1)
            val replacement = if (end < 0 || end - i > MAX_ENTITY_LENGTH) {
                null
            } else {
                resolve(input.substring(i + 1, end))
            }
            if (replacement == null) {
                out.append(c)
                i++
            } else {
                out.append(replacement)
                i = end + 1
            }
        }
        return out.toString()
    }

    private fun resolve(body: String): String? = when {
        body.isEmpty() -> null
        body.startsWith("#x", ignoreCase = true) ->
            body.drop(2).toIntOrNull(HEX_RADIX)?.let(::codePoint)

        body.startsWith("#") -> body.drop(1).toIntOrNull()?.let(::codePoint)
        else -> NAMED[body.lowercase(Locale.US)]
    }

    /**
     * Null means "not an entity, leave the text as the author wrote it". An empty string means
     * "recognised, and deliberately not drawn".
     */
    private fun codePoint(value: Int): String? = when {
        // A numeric reference into the C1 range is Windows-1252 arriving the long way round:
        // `&#149;` is a bullet, not an unprintable control. NsText repairs the same mistake
        // when it arrives as raw bytes, and browsers are required to resolve it this way.
        // Where Windows-1252 assigns nothing there is nothing to draw, so nothing is drawn.
        value in C1_START..C1_END -> NsText.windows1252(value)?.toString().orEmpty()

        // NationStates dresses its own pages with an icon webfont — the `[font=nationstates]`
        // runs all over Lazarus's factbook are `&#59425;` and friends. Private Use code points
        // mean nothing without the font that defines them, and Civily does not ship it, so
        // every one of them would draw as a tofu box. Dropping them loses no words.
        value in PUA_START..PUA_END -> ""

        value in 1..Character.MAX_CODE_POINT -> String(Character.toChars(value))

        else -> null
    }

    private const val C1_START = 0x80
    private const val C1_END = 0x9F

    private const val PUA_START = 0xE000
    private const val PUA_END = 0xF8FF

    private const val HEX_RADIX = 16

    /** Longer than any entity anyone writes; stops a stray `&` scanning half a factbook. */
    private const val MAX_ENTITY_LENGTH = 12

    private val NAMED = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
        "hellip" to "…",
        "mdash" to "—",
        "ndash" to "–",
        "ldquo" to "“",
        "rdquo" to "”",
        "lsquo" to "‘",
        "rsquo" to "’",
        "bull" to "•",
        "middot" to "·",
        "deg" to "°",
        "copy" to "©",
        "reg" to "®",
        "trade" to "™",
    )
}
