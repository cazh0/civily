package dev.cazh0.civily.core.text

import java.util.Locale

/**
 * The one or two letters that stand in for a nation on an avatar.
 *
 * Nation names are player-written and can be a single word, eight words, or punctuation with a
 * letter hiding in it, so this takes the first letter of the first two *letter-bearing* words
 * rather than the first two characters. "The Republic of ǅ" should not render as "Th".
 */
object Initials {

    fun of(displayName: String): String {
        val words = displayName
            .split(' ', '-', '_')
            .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }

        return when {
            words.isEmpty() -> FALLBACK
            words.size == 1 -> words[0].toString().uppercase(Locale.US)
            else -> "${words[0]}${words[1]}".uppercase(Locale.US)
        }
    }

    /** Shown when a name carries no letters at all, which player names occasionally do not. */
    private const val FALLBACK = "?"
}
