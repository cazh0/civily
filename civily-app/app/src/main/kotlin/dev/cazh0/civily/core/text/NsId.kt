package dev.cazh0.civily.core.text

import java.text.Normalizer
import java.util.Locale

/**
 * Converts between a displayed nation or region name and the id the API addresses it by.
 *
 * NationStates ids are lowercase with spaces as underscores. Names may contain accented
 * characters the API does not accept, so they are decomposed and stripped to ASCII first —
 * `Åland` and `Aland` must resolve to the same nation.
 */
object NsId {

    fun fromName(name: String): String =
        toAscii(name).lowercase(Locale.US).replace(' ', '_')

    /**
     * Turns an id back into something worth showing. The API hands back bare ids for
     * delegates, founders and endorsements, and `the_north_pacific` is not a name.
     *
     * Three rules, in order: Roman numerals go fully upper (`vii` to `VII`), the first word
     * is always capitalised, and articles and prepositions in any of the languages players
     * use stay lower (`empire_of_the_sun` to `Empire of the Sun`).
     */
    fun toName(id: String): String =
        fromName(id).split('_').mapIndexed { wordIndex, word ->
            // Why no trailing-separator repair: Kotlin's split keeps empty trailing parts, so
            // "foo-" survives the round trip. Java's drops them, which is why the original
            // needed a fix-up step here.
            word.split('-').mapIndexed { partIndex, part ->
                when {
                    part.isEmpty() -> part
                    ROMAN_NUMERALS.matches(part) -> part.uppercase(Locale.US)
                    wordIndex == 0 && partIndex == 0 -> capitalise(part)
                    part in LOWERCASE_WORDS -> part
                    else -> capitalise(part)
                }
            }.joinToString("-")
        }.joinToString(" ")

    /**
     * NFD splits an accented character into base letter plus combining mark; dropping
     * everything above U+007F then leaves the bare ASCII letter.
     */
    private fun toAscii(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return buildString(decomposed.length) {
            for (c in decomposed) if (c.code <= MAX_ASCII) append(c)
        }
    }

    private fun capitalise(word: String): String =
        word.replaceFirstChar { it.uppercaseChar() }

    private const val MAX_ASCII = 0x7F

    private val ROMAN_NUMERALS = Regex(
        "^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Articles, prepositions and conjunctions that stay lower mid-name. Player nation names
     * are routinely French, German, Spanish, Italian, Latin or Dutch, so the list is not
     * English-only.
     */
    private val LOWERCASE_WORDS = setOf(
        // the
        "the", "le", "la", "les", "el", "lo", "los", "las", "al",
        "der", "die", "das", "des", "dem", "il", "het",
        // of
        "of", "du", "de", "del", "dello", "della", "dei", "degli", "delle", "von", "no",
        // a / an
        "an", "a", "un", "une", "ein", "eine", "einer", "eines", "einem", "einen",
        "uno", "una", "unos", "unas",
        // to
        "to", "au", "ad", "in", "zu", "zum",
        // and
        "and", "et", "e", "ac", "atque", "und", "y",
    )
}
