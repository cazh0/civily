package dev.cazh0.civily.core.text

/**
 * The article English wants in front of a government classification.
 *
 * "…was reclassified from **an** Inoffensive Centrist Democracy to Democratic Socialists" — the
 * first is a kind of state and takes an article, the second is a group of people and takes
 * none. The game's own sentence makes that distinction and a sentence that skipped it would
 * read as broken English on every second answer.
 */
object Classification {

    enum class Article { None, A, An }

    /**
     * Why a trailing `s` is enough: of the classifications NationStates uses, the only ones
     * ending in one are the plurals — Democratic Socialists, Liberal Democratic Socialists,
     * Iron Fist Socialists, Iron Fist Consumerists. Every singular one ends in a state, a
     * paradise, a dictatorship, a democracy, an anarchy or a utopia.
     *
     * Why the first letter is enough for a/an: no classification in the game begins with a
     * vowel spelled one way and sounded another — the trap words are "a university" and "an
     * hour", and neither shape appears. The list opens with Anarchy, Authoritarian Democracy
     * and Inoffensive Centrist Democracy, all of which take "an" by both tests.
     *
     * A classification this rule gets wrong is a wrong article in one sentence, which is what
     * makes the heuristic affordable: nothing here can name the wrong rating or the wrong
     * nation.
     */
    fun article(classification: String): Article {
        val name = classification.trim()
        return when {
            name.isEmpty() -> Article.None
            name.endsWith('s') -> Article.None
            name.first().lowercaseChar() in VOWELS -> Article.An
            else -> Article.A
        }
    }

    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u')
}
