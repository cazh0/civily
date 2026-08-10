package dev.cazh0.stately.core.text

import java.util.Locale

/**
 * Decodes the HTML entities that survive inside NationStates BBCode.
 *
 * Why this is needed on top of XML decoding: the XML parser already unescapes the transport
 * layer, so `&amp;amp;` in the response becomes `&amp;` — and that is what the field actually
 * contains, because the site stores factbooks and posts with their entities intact. Without a
 * second pass the reader sees `&amp;` where the author wrote an ampersand.
 *
 * Decoding is a single left-to-right pass, so a decoded `&` can never be re-read as the start
 * of another entity: `&amp;lt;` yields the literal text `&lt;`, which is what the author meant.
 * Anything unrecognised is left exactly as written rather than guessed at.
 */
object HtmlEntities {

    fun decode(input: String): String {
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

    private fun codePoint(value: Int): String? =
        if (value in 1..Character.MAX_CODE_POINT) String(Character.toChars(value)) else null

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
