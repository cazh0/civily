package dev.cazh0.civily.core.text

import java.util.Locale

/**
 * Whether one of a nation's three freedom ratings reads as good news or bad.
 *
 * **The word carries the judgement, not the score, and this is the whole reason this file
 * exists.** NationStates rates Civil Rights, Economy and Political Freedom on census scales
 * whose scores rise monotonically — but the words do not. Checked against live responses across
 * the full rank range of each scale:
 *
 * | Score | Civil Rights | Economy | Political Freedom |
 * |---|---|---|---|
 * | ~10 | Unheard Of | Struggling | Outlawed |
 * | ~30 | Few | Fair | Few |
 * | ~48 | Below Average | Reasonable | Below Average |
 * | ~58 | Good | Good | Good |
 * | ~79 | Superb | Very Strong | Superb |
 * | ~83 | World Benchmark | Thriving | World Benchmark |
 * | ~90 | **Excessive** | Powerhouse | **Excessive** |
 * | ~92 | **Frightening** | — | **Widely Abused** |
 * | ~97 | **Frightening** | **Frightening** | **Corrupted** |
 *
 * The top of every ladder turns *against* the nation: a maximum civil-rights score is
 * "Frightening", a maximum political-freedom score is "Corrupted", and a runaway economy is
 * "Frightening" too. The legacy client colours these cards on a red-to-green ramp indexed by
 * `score / 7`, which paints "Frightening" and "Corrupted" bright green — the highest scores in
 * the game shown as the best possible news. Reading the word instead is what fixes that, and it
 * is why Civily asks for no score here at all.
 *
 * [Standing.Middling] is also the answer for a word this table does not know. NationStates can
 * add a rung without notice, and the cost of not recognising one is a tile with no colour —
 * never a tile coloured the wrong way.
 */
object FreedomRating {

    enum class Standing { Good, Middling, Bad }

    fun standing(rating: String): Standing = when (rating.trim().lowercase(Locale.US)) {
        in GOOD -> Standing.Good
        in BAD -> Standing.Bad
        else -> Standing.Middling
    }

    /** Every rung a sweep of all three scales found that reads as the nation doing well. */
    private val GOOD = setOf(
        "good",
        "very good",
        "excellent",
        "superb",
        "world benchmark",
        "strong",
        "very strong",
        "thriving",
        "powerhouse",
    )

    /**
     * Both ends of the ladder. The first nine are the thing being absent or failing; the last
     * three are the thing having run away with itself, which the game words just as harshly.
     */
    private val BAD = setOf(
        "outlawed",
        "unheard of",
        "rare",
        "few",
        "some",
        "below average",
        "basket case",
        "struggling",
        "fragile",
        "excessive",
        "widely abused",
        "frightening",
        "corrupted",
    )
}
