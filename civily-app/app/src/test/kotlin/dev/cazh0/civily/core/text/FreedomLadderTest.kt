package dev.cazh0.civily.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class FreedomLadderTest {

    @Test
    fun `reads the direction of the live sample`() {
        // The captured c=issue answer: civil rights, Some to Few.
        assertEquals(FreedomLadder.Move.Fell, FreedomLadder.move("Some", "Few"))
    }

    @Test
    fun `reads the economy ladder in both directions`() {
        assertEquals(FreedomLadder.Move.Fell, FreedomLadder.move("Reasonable", "Developing"))
        assertEquals(FreedomLadder.Move.Rose, FreedomLadder.move("Developing", "Reasonable"))
        assertEquals(FreedomLadder.Move.Rose, FreedomLadder.move("Imploded", "All-Consuming"))
    }

    @Test
    fun `a rise into a word that reads badly is still a rise`() {
        // The whole reason this is separate from FreedomRating: by score this is the top of the
        // scale, and by word it is the nation being warned about.
        assertEquals(FreedomLadder.Move.Rose, FreedomLadder.move("World Benchmark", "Excessive"))
        assertEquals(FreedomRating.Standing.Bad, FreedomRating.standing("Excessive"))
    }

    @Test
    fun `political freedom runs past excessive to corrupted`() {
        assertEquals(FreedomLadder.Move.Rose, FreedomLadder.move("Excessive", "Widely Abused"))
        assertEquals(FreedomLadder.Move.Rose, FreedomLadder.move("Widely Abused", "Corrupted"))
    }

    @Test
    fun `case and surrounding space do not decide the direction`() {
        assertEquals(FreedomLadder.Move.Fell, FreedomLadder.move("  SOME  ", "few"))
    }

    @Test
    fun `a rung this list does not know changes rather than moves`() {
        assertEquals(FreedomLadder.Move.Changed, FreedomLadder.move("Some", "Astonishing"))
        assertEquals(FreedomLadder.Move.Changed, FreedomLadder.move("Astonishing", "Some"))
        assertEquals(FreedomLadder.Move.Changed, FreedomLadder.move("", ""))
    }

    @Test
    fun `a word that did not move is not called a rise or a fall`() {
        assertEquals(FreedomLadder.Move.Changed, FreedomLadder.move("Good", "Good"))
    }
}
