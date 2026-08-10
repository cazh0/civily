package dev.cazh0.stately.data.wa

import dev.cazh0.stately.core.text.bbcode.BbBlock

/**
 * A chamber in the shape the screen needs.
 *
 * Why this exists when [AssemblyDto] already holds the same numbers: the resolution's body
 * arrives as BBCode and has to be parsed before it can be drawn. Parsing is CPU work, and
 * spec §2 R2b keeps it off the main thread — so it happens here, in the repository, on a
 * background dispatcher, rather than inside a composable that runs on every recomposition.
 *
 * A null [resolution] is the chamber's ordinary between-votes state, not an error.
 */
data class Assembly(
    val memberCount: Int,
    val delegateCount: Int,
    val resolution: Resolution?,
)

data class Resolution(
    val name: String,
    val category: String,
    val proposedBy: String,
    val votesFor: Int,
    val votesAgainst: Int,
    val body: List<BbBlock>,
) {
    val totalVotes: Int get() = votesFor + votesAgainst

    /** Share of cast votes in favour, 0 when nobody has voted yet. */
    val supportFraction: Float
        get() = if (totalVotes == 0) 0f else votesFor.toFloat() / totalVotes
}
