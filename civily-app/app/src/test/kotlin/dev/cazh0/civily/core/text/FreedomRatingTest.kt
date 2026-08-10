package dev.cazh0.civily.core.text

import dev.cazh0.civily.core.text.FreedomRating.Standing
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Every word here was read off a live response while sweeping the full rank range of census
 * scales 0, 1 and 2 — not taken from the legacy app's tables, which have gaps of their own.
 */
class FreedomRatingTest {

    @Test
    fun `the bottom of a ladder is bad news`() {
        // Political freedom ~8.71, ~10.86, ~21.43, ~32.57; civil rights ~28.44, ~37.22, ~48.48.
        assertEquals(Standing.Bad, FreedomRating.standing("Outlawed"))
        assertEquals(Standing.Bad, FreedomRating.standing("Unheard Of"))
        assertEquals(Standing.Bad, FreedomRating.standing("Rare"))
        assertEquals(Standing.Bad, FreedomRating.standing("Few"))
        assertEquals(Standing.Bad, FreedomRating.standing("Some"))
        assertEquals(Standing.Bad, FreedomRating.standing("Below Average"))
    }

    @Test
    fun `a failing economy is bad news`() {
        // Economy ~1.80, ~6.67, ~15.75.
        assertEquals(Standing.Bad, FreedomRating.standing("Basket Case"))
        assertEquals(Standing.Bad, FreedomRating.standing("Struggling"))
        assertEquals(Standing.Bad, FreedomRating.standing("Fragile"))
    }

    @Test
    fun `the top of a ladder is bad news too, which is the whole point of this table`() {
        // The highest scores in the game: civil rights 90–100, political freedom 89.6–100,
        // economy 97+. The legacy client's score ramp paints every one of these bright green.
        assertEquals(Standing.Bad, FreedomRating.standing("Excessive"))
        assertEquals(Standing.Bad, FreedomRating.standing("Widely Abused"))
        assertEquals(Standing.Bad, FreedomRating.standing("Frightening"))
        assertEquals(Standing.Bad, FreedomRating.standing("Corrupted"))
    }

    @Test
    fun `the good rungs are good news`() {
        assertEquals(Standing.Good, FreedomRating.standing("Good"))
        assertEquals(Standing.Good, FreedomRating.standing("Very Good"))
        assertEquals(Standing.Good, FreedomRating.standing("Excellent"))
        assertEquals(Standing.Good, FreedomRating.standing("Superb"))
        assertEquals(Standing.Good, FreedomRating.standing("World Benchmark"))
        assertEquals(Standing.Good, FreedomRating.standing("Strong"))
        assertEquals(Standing.Good, FreedomRating.standing("Very Strong"))
        assertEquals(Standing.Good, FreedomRating.standing("Thriving"))
        assertEquals(Standing.Good, FreedomRating.standing("Powerhouse"))
    }

    @Test
    fun `a word that judges nothing carries no colour`() {
        // Economy ~22–44, and the middle of both other ladders. "Fair" and "Developing" sit low
        // on the scale and are still not an accusation, so nothing is asserted about them.
        assertEquals(Standing.Middling, FreedomRating.standing("Average"))
        assertEquals(Standing.Middling, FreedomRating.standing("Reasonable"))
        assertEquals(Standing.Middling, FreedomRating.standing("Fair"))
        assertEquals(Standing.Middling, FreedomRating.standing("Developing"))
    }

    @Test
    fun `an unrecognised rung is middling rather than guessed at`() {
        // NationStates can add a rung without notice. The cost must be a tile with no colour,
        // never a tile coloured the wrong way.
        assertEquals(Standing.Middling, FreedomRating.standing("Utterly Unprecedented"))
        assertEquals(Standing.Middling, FreedomRating.standing(""))
    }

    @Test
    fun `case and surrounding space do not change the answer`() {
        assertEquals(Standing.Bad, FreedomRating.standing("  frightening "))
        assertEquals(Standing.Good, FreedomRating.standing("WORLD BENCHMARK"))
    }
}
