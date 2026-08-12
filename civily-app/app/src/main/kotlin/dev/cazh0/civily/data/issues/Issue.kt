package dev.cazh0.civily.data.issues

import dev.cazh0.civily.core.text.bbcode.BbBlock

/**
 * The nation's outstanding issues, plus what the newspaper masthead needs to name itself.
 */
data class IssuesPage(
    val capital: String,
    val nationName: String,
    val flagUrl: String,
    val currency: String,
    val issues: List<Issue>,
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
)

data class IssueResultHeadline(
    val text: String,
    val imageUrls: List<String>,
)

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
