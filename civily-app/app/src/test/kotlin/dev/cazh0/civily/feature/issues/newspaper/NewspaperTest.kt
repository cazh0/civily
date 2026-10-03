package dev.cazh0.civily.feature.issues.newspaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class NewspaperTest {

    private val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-08-09")!!

    @Test
    fun `the paper is named after the capital`() {
        assertEquals("Itagüí", Newspaper.place("Itagüí", "Bacata"))
    }

    @Test
    fun `the same issue always prints the same paper`() {
        // A masthead that reshuffled between the list and the detail would read as noise.
        assertEquals(Newspaper.title(976), Newspaper.title(976))
        assertEquals(Newspaper.city(976), Newspaper.city(976))
        assertEquals(Newspaper.volume(976), Newspaper.volume(976))
    }

    @Test
    fun `different issues get different papers`() {
        val titles = (1..10).map { Newspaper.title(it) }.toSet()

        assertTrue("expected variety across issues", titles.size > 1)
    }

    @Test
    fun `a nation with no capital falls back to its own name`() {
        assertEquals("Bacata", Newspaper.place("", "Bacata"))
    }

    @Test
    fun `with neither a capital nor a name there is no place to name the paper after`() {
        assertNull(Newspaper.place("", ""))
        assertNull(Newspaper.place("  ", " "))
    }

    @Test
    fun `the edition date is the day in capitals`() {
        assertEquals("SUNDAY 9 AUGUST 2026", Newspaper.date(date))
    }

    @Test
    fun `a volume is never zero or negative`() {
        // mod, not rem: a negative id must still print as a volume someone could own.
        assertTrue((-200..200).all { Newspaper.volume(it) > 0 })
    }

    @Test
    fun `an issue always prints on the same design`() {
        // The list and the detail ask separately. A design that disagreed between them would
        // redraw the paper under the reader on the way into the issue.
        assertEquals(Newspaper.design(976), Newspaper.design(976))
    }

    @Test
    fun `every design is printed`() {
        val designs = (1..10).map { Newspaper.design(it) }.toSet()

        assertEquals(Newspaper.Design.entries.toSet(), designs)
    }

    @Test
    fun `consecutive issues print on different designs`() {
        // The aftermath pile walks this sequence one step at a time, so a run that repeated
        // would print the same paper twice in a row down the stack.
        val run = (1..Newspaper.Design.entries.size).map { Newspaper.design(it) }

        assertEquals(run.size, run.toSet().size)
    }

    @Test
    fun `every design meets every edition city`() {
        // Three designs against a four-cycle of cities: the pair comes back around only every
        // twelfth issue, which is what stops a design being stuck with the same two cities.
        val pairs = (1..12).map { Newspaper.design(it) to Newspaper.city(it) }

        assertEquals(12, pairs.toSet().size)
    }

    @Test
    fun `a negative issue id does not crash the paper`() {
        // mod, not rem: a negative id must still land inside the title list.
        Newspaper.title(-3)
        Newspaper.city(-3)
        assertTrue(Newspaper.design(-3) in Newspaper.Design.entries)
    }
}
