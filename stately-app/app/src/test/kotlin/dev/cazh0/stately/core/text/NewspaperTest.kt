package dev.cazh0.stately.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class NewspaperTest {

    private val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-08-09")!!

    @Test
    fun `the paper is named after the capital`() {
        assertEquals("The Itagüí Sentinel", Newspaper.masthead("Itagüí", "Bacata", issueId = 976))
    }

    @Test
    fun `the same issue always prints the same paper`() {
        // A masthead that reshuffled between the list and the detail would read as noise.
        val first = Newspaper.masthead("Itagüí", "Bacata", issueId = 976)
        val second = Newspaper.masthead("Itagüí", "Bacata", issueId = 976)

        assertEquals(first, second)
        assertEquals(Newspaper.edition(976, date), Newspaper.edition(976, date))
    }

    @Test
    fun `different issues get different papers`() {
        val titles = (1..10).map { Newspaper.masthead("Itagüí", "Bacata", it) }.toSet()

        assertTrue("expected variety across issues", titles.size > 1)
    }

    @Test
    fun `a nation with no capital falls back to its own name`() {
        assertEquals("The Bacata Times", Newspaper.masthead("", "Bacata", issueId = 1))
    }

    @Test
    fun `with neither a capital nor a name there is still a paper`() {
        assertEquals("The Daily Dispatch", Newspaper.masthead("", "", issueId = 1))
    }

    @Test
    fun `the edition line carries city date and volume`() {
        val edition = Newspaper.edition(976, date)

        assertEquals("SUNDAY 9 AUGUST 2026", edition.date)
        assertTrue(edition.volume.endsWith("NO. 976"))
        assertTrue(edition.city.isNotBlank())
    }

    @Test
    fun `a negative issue id does not crash the masthead`() {
        // mod, not rem: a negative id must still land inside the title list.
        assertNotEquals("", Newspaper.masthead("Itagüí", "Bacata", issueId = -3))
        assertNotEquals("", Newspaper.edition(-3, date).city)
    }
}
