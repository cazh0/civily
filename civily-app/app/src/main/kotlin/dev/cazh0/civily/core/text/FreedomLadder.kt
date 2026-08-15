package dev.cazh0.civily.core.text

import java.util.Locale

/**
 * Which way along its own scale a freedom rating moved: "the Economy **fell** from Reasonable
 * to Developing".
 *
 * **This answers a different question from [FreedomRating], and the two are allowed to
 * disagree.** [FreedomRating] asks whether a word is good news; this asks which of two words
 * sits higher on the scale that produced them. Civil rights running from World Benchmark to
 * Excessive is a rise by score and bad news by word, and the aftermath says both — the verb
 * comes from here and the colour from there. Collapsing them is the legacy client's mistake in
 * the other direction: it reads position only, so it reports a move to "Frightening" as good.
 *
 * One list for three ladders. NationStates words Civil Rights, Economy and Political Freedom
 * from separate vocabularies that overlap in the middle, and each rating's own rungs appear
 * here in ascending order:
 *
 * | Rating | Its rungs, low to high |
 * |---|---|
 * | Economy | Imploded · Basket Case · Struggling · Fragile · Weak · Developing · Fair · Reasonable · Good · Strong · Very Strong · Thriving · Powerhouse · All-Consuming · Frightening |
 * | Civil Rights | Outlawed · Unheard Of · Rare · Few · Some · Below Average · Average · Good · Very Good · Excellent · Superb · World Benchmark · Excessive · Frightening |
 * | Political Freedom | Outlawed · Unheard Of · Rare · Few · Some · Below Average · Average · Good · Very Good · Excellent · Superb · World Benchmark · Excessive · Widely Abused · Corrupted |
 *
 * A word this list does not know gives [Move.Changed], which is the honest verb: NationStates
 * can add a rung without notice, and the cost of not knowing one is a sentence that says less
 * — never one that says the wrong direction.
 */
object FreedomLadder {

    enum class Move { Rose, Fell, Changed }

    fun move(from: String, to: String): Move {
        val start = ORDER.indexOf(from.trim().lowercase(Locale.US))
        val end = ORDER.indexOf(to.trim().lowercase(Locale.US))
        return when {
            start < 0 || end < 0 || start == end -> Move.Changed
            end > start -> Move.Rose
            else -> Move.Fell
        }
    }

    private val ORDER = listOf(
        "imploded",
        "basket case",
        "struggling",
        "fragile",
        "weak",
        "developing",
        "fair",
        "reasonable",
        "outlawed",
        "unheard of",
        "rare",
        "few",
        "some",
        "below average",
        "average",
        "good",
        "strong",
        "very strong",
        "thriving",
        "powerhouse",
        "all-consuming",
        "very good",
        "excellent",
        "superb",
        "world benchmark",
        "excessive",
        "widely abused",
        "frightening",
        "corrupted",
    )
}
