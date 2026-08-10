package dev.cazh0.stately.data.region

import dev.cazh0.stately.core.text.bbcode.BbBlock

/**
 * A region in the shape the screen needs.
 *
 * Two things happen on the way here that must not happen in a composable: the factbook is
 * BBCode and has to be parsed (spec §2 R2b keeps that off the main thread), and the API's
 * `"0"` sentinel for "no delegate" becomes a real null. A screen should never have to know
 * that zero means nobody.
 */
data class Region(
    val name: String,
    val flagUrl: String,
    val nationCount: Int,
    val delegate: String?,
    val delegateVotes: Int,
    val founder: String?,
    val factbook: List<BbBlock>,
)
