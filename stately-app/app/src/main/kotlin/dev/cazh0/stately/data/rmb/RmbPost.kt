package dev.cazh0.stately.data.rmb

import dev.cazh0.stately.core.text.bbcode.BbBlock

/**
 * A post in the shape the board shows it.
 *
 * [Visibility] replaces the API's integer status because a screen should not carry a table of
 * magic numbers, and because the three cases render as three genuinely different things: a
 * post, a note that a moderator removed one, and a note that its author did.
 */
data class RmbPost(
    val id: String,
    val author: String,
    val postedAtEpochSeconds: Long,
    val wasEdited: Boolean,
    val likes: Int,
    val fromEmbassyRegion: String?,
    val body: List<BbBlock>,
    val visibility: Visibility,
) {
    enum class Visibility { Visible, Suppressed, Deleted }
}
