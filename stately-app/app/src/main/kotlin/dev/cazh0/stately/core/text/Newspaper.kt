package dev.cazh0.stately.core.text

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The masthead and edition line for an issue presented as a newspaper.
 *
 * Everything except the date is derived from the issue's own id, so a given issue always
 * prints the same paper. A masthead that reshuffled on every scroll would read as noise;
 * one that is stable reads as a publication.
 */
object Newspaper {

    data class Edition(val city: String, val date: String, val volume: String)

    /**
     * @param capital the nation's capital, which is what the paper is named after — the same
     *   convention the desktop site uses. Falls back to the nation itself when the capital is
     *   unknown, because a paper with no name is worse than one named after the country.
     */
    fun masthead(capital: String, nationName: String, issueId: Int): String {
        val place = capital.ifBlank { nationName }.ifBlank { return FALLBACK_MASTHEAD }
        return "The $place ${TITLES[issueId.mod(TITLES.size)]}"
    }

    fun edition(issueId: Int, now: Date = Date()): Edition = Edition(
        city = EDITIONS[issueId.mod(EDITIONS.size)],
        date = DATE_FORMAT.format(now).uppercase(Locale.US),
        // Volume and number are flavour: fixed per issue, plausible, and checked by nobody.
        volume = "VOL. ${issueId.mod(VOLUME_RANGE) + 1} NO. $issueId",
    )

    private const val FALLBACK_MASTHEAD = "The Daily Dispatch"
    private const val VOLUME_RANGE = 80

    private val TITLES = listOf(
        "Leader", "Times", "Herald", "Post", "Chronicle",
        "Gazette", "Sentinel", "Tribune", "Observer", "Register",
    )

    private val EDITIONS = listOf("CITY FINAL", "LATE EDITION", "MORNING EDITION", "EVENING FINAL")

    /** Fixed to US English: the app ships English-only by project rule (README). */
    private val DATE_FORMAT = SimpleDateFormat("EEEE d MMMM yyyy", Locale.US)
}
