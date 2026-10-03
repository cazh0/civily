package dev.cazh0.civily.feature.issues.newspaper

import androidx.annotation.StringRes
import dev.cazh0.civily.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The masthead and edition line for an issue presented as a newspaper.
 *
 * Everything except the date is derived from the issue's own id, so a given issue always
 * prints the same paper. A masthead that reshuffled on every scroll would read as noise;
 * one that is stable reads as a publication.
 *
 * Why string resources rather than words: the words are copy, and copy lives in `res/values`
 * (ARCHITECTURE A5). What is decided here is which of them an issue prints under, and that needs
 * no `Context` to test. [newspaperMasthead] and [newspaperEdition] turn the answer into words.
 */
object Newspaper {

    /** The edition line as printed: three cells, already in words. */
    data class Edition(val city: String, val date: String, val volume: String)

    /** Which front page an issue is printed on. All three are the game's own. */
    enum class Design { Broadsheet, Tabloid, Berliner }

    /**
     * The paper's design, and so the last thing about it that varies per issue.
     *
     * Why it belongs here rather than at the screen: it has to be the same page in the list and
     * in the detail, and it has to be the same page tomorrow, which is exactly what the masthead
     * and the edition line already promise. A design picked at the call site would change under
     * the reader on the way into the issue.
     *
     * Three designs over consecutive ids, against [EDITIONS]' four-cycle, come back around
     * together only every twelfth issue, so every design prints under every city. Two designs
     * did not: three and four share no factor, two and four share one, which had the broadsheet
     * taking the first and third cities for good and the tabloid the other two.
     */
    fun design(issueId: Int): Design = Design.entries[issueId.mod(Design.entries.size)]

    /**
     * What the paper is named after: the nation's capital, the same convention the desktop site
     * uses. The nation itself when the capital is unknown, because a paper with no name is worse
     * than one named after the country. Null when there is neither.
     */
    fun place(capital: String, nationName: String): String? =
        capital.ifBlank { nationName }.ifBlank { null }

    /** The paper's title — Herald, Times — under this issue. */
    @StringRes
    fun title(issueId: Int): Int = TITLES[issueId.mod(TITLES.size)]

    /** The edition this issue prints as — late, morning. */
    @StringRes
    fun city(issueId: Int): Int = EDITIONS[issueId.mod(EDITIONS.size)]

    /** Volume and number are flavour: fixed per issue, plausible, and checked by nobody. */
    fun volume(issueId: Int): Int = issueId.mod(VOLUME_RANGE) + 1

    fun date(now: Date = Date()): String = DATE_FORMAT.format(now).uppercase(Locale.US)

    private const val VOLUME_RANGE = 80

    private val TITLES = listOf(
        R.string.newspaper_title_leader,
        R.string.newspaper_title_times,
        R.string.newspaper_title_herald,
        R.string.newspaper_title_post,
        R.string.newspaper_title_chronicle,
        R.string.newspaper_title_gazette,
        R.string.newspaper_title_sentinel,
        R.string.newspaper_title_tribune,
        R.string.newspaper_title_observer,
        R.string.newspaper_title_register,
    )

    private val EDITIONS = listOf(
        R.string.newspaper_edition_city_final,
        R.string.newspaper_edition_late,
        R.string.newspaper_edition_morning,
        R.string.newspaper_edition_evening_final,
    )

    /** Fixed to US English: the app ships English-only by project rule (RULES §6). */
    private val DATE_FORMAT = SimpleDateFormat("EEEE d MMMM yyyy", Locale.US)
}
