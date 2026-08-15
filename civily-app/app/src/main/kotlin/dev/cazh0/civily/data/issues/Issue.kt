package dev.cazh0.civily.data.issues

import dev.cazh0.civily.core.text.bbcode.BbBlock
import dev.cazh0.civily.data.nation.Policy

/**
 * The nation's outstanding issues, plus what the newspaper masthead needs to name itself.
 */
data class IssuesPage(
    val capital: String,
    val nationName: String,
    val flagUrl: String,
    val currency: String,
    /** The demonym adjective, for the sentence a reclassification is written as. */
    val demonym: String,
    val issues: List<Issue>,
    /** Unix seconds, or null when the API named no next issue. */
    val nextIssueTime: Long?,
)

/**
 * What the Issues button knows without opening the Issues screen: how many decisions are
 * waiting, and when the next one lands.
 *
 * [nationId] is stamped on because the button sits beside the account switcher. Switching
 * nations costs no request, so without it the button would show the previous nation's count
 * against the new nation's name for as long as the reload takes.
 */
data class IssueBadge(
    val nationId: String,
    val dueCount: Int,
    /** Unix seconds, or null when the API named no next issue. */
    val nextIssueTime: Long?,
)

/** An issue in the shape the screen shows it, with its BBCode already parsed. */
data class Issue(
    val id: Int,
    val title: String,
    val text: List<BbBlock>,
    /** Full URLs of the newspaper artwork, in the API's PIC1/PIC2 order. */
    val imageUrls: List<String>,
    val options: List<IssueOption>,
)

/**
 * What enacting legislation did.
 *
 * This is the payoff of the whole feature: the game answers a decision with a consequence,
 * a set of moved statistics and tomorrow's headlines.
 */
data class IssueResult(
    val description: String,
    val rankings: List<CensusChange>,
    val headlines: List<IssueResultHeadline>,
    /** Ratings the answer moved far enough to rename. */
    val reclassifications: List<Reclassification>,
    /** Policies the answer put on the books. */
    val newPolicies: List<Policy>,
    /** Policies the answer took off them. */
    val canceledPolicies: List<Policy>,
    /**
     * Rift banner codes the answer unlocked — the game's postcards. `NsUrl.banner` addresses
     * the artwork; the code itself is never shown, because it is a filename, not a name.
     */
    val postcards: List<String>,
)

data class IssueResultHeadline(
    val text: String,
    val imageUrls: List<String>,
)

/**
 * One rating the game has renamed, from the word it used to the word it uses now.
 *
 * Both words are printed and neither is called an improvement. The three freedom ladders are
 * not monotonic — `FreedomRating` records the sweep that proved it, where the top rungs read
 * "Excessive", "Frightening" and "Corrupted" — so "went up" would be a claim the ordering
 * cannot support. What the new word is worth is left to [FreedomRating.standing], which judges
 * the word rather than its position.
 */
data class Reclassification(
    val rating: Rating,
    val from: String,
    val to: String,
) {
    /**
     * The four things the game reclassifies. `RECLASSIFY type` names one of these and no more.
     *
     * Declared in the order the aftermath prints them, which is why [Government] leads: it is
     * the nation's own title and the ratings are the reasons it moved. The API sends them in
     * whatever order it likes.
     */
    enum class Rating { Government, CivilRights, Economy, PoliticalFreedom }
}

data class CensusChange(
    /** Indexes `R.array.census_scales`; the screen turns it into a name. */
    val scaleId: Int,
    /** Signed percent movement from the issue result. */
    val percentChange: Double,
)

data class IssueOption(
    /** The API's own option number, counted from zero. Never the list index. */
    val id: Int,
    val text: List<BbBlock>,
)
