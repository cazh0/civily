package dev.cazh0.civily.data.wa

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

/** Which chamber. The API addresses them by number, so the number is the identity. */
enum class Council(val id: Int) {
    // Why 3 rather than 1: NationStates moved active General Assembly resolutions to 3 in 2026;
    // 1 remains a readable archive of the Legacy General Assembly.
    GeneralAssembly(3),
    SecurityCouncil(2),
}

/**
 * A World Assembly chamber as returned by `api.cgi?wa=…`.
 *
 * Member and delegate counts are World Assembly-wide, so both chambers report the same
 * figures. Only [resolution] differs.
 */
@Serializable
@XmlSerialName("WA", "", "")
data class AssemblyDto(
    @XmlElement(true) @XmlSerialName("NUMNATIONS", "", "") val memberCount: Int = 0,
    @XmlElement(true) @XmlSerialName("NUMDELEGATES", "", "") val delegateCount: Int = 0,
    val resolution: ResolutionDto = ResolutionDto(),
)

/**
 * The resolution currently at vote, if there is one.
 *
 * Why every field defaults and nothing is nullable: between votes the API returns a literal
 * `<RESOLUTION></RESOLUTION>` rather than omitting the element or sending an error. That empty
 * element is the normal state for a good part of every day, so it decodes into an instance
 * whose [isAtVote] is false — not into a null the UI has to remember to check, and not into a
 * parse failure.
 */
@Serializable
@XmlSerialName("RESOLUTION", "", "")
data class ResolutionDto(
    @XmlElement(true) @XmlSerialName("NAME", "", "") val name: String = "",
    @XmlElement(true) @XmlSerialName("CATEGORY", "", "") val category: String = "",
    /** For Security Council resolutions, the nation or region the resolution acts on. */
    @XmlElement(true) @XmlSerialName("OPTION", "", "") val target: String = "",
    @XmlElement(true) @XmlSerialName("PROPOSED_BY", "", "") val proposedBy: String = "",
    /** The resolution text, as BBCode. */
    @XmlElement(true) @XmlSerialName("DESC", "", "") val body: String = "",
    @XmlElement(true) @XmlSerialName("TOTAL_VOTES_FOR", "", "") val votesFor: Int = 0,
    @XmlElement(true) @XmlSerialName("TOTAL_VOTES_AGAINST", "", "") val votesAgainst: Int = 0,
    @XmlElement(true) @XmlSerialName("VOTE_TRACK_FOR", "", "")
    val voteTrackFor: VoteTrackDto = VoteTrackDto(),
    @XmlElement(true) @XmlSerialName("VOTE_TRACK_AGAINST", "", "")
    val voteTrackAgainst: VoteTrackDto = VoteTrackDto(),
    @XmlElement(true) @XmlSerialName("DELVOTES_FOR", "", "")
    val delegateVotesFor: DelegateVotesDto = DelegateVotesDto(),
    @XmlElement(true) @XmlSerialName("DELVOTES_AGAINST", "", "")
    val delegateVotesAgainst: DelegateVotesDto = DelegateVotesDto(),
) {
    /**
     * The one thing the wire shape decides. Vote arithmetic lives on [Resolution], so it is
     * defined once rather than on both sides of the mapping.
     */
    val isAtVote: Boolean get() = name.isNotEmpty()
}

/** One side of the hourly voting history returned by `votetrack`. */
@Serializable
@XmlSerialName("VOTE_TRACK_FOR", "", "")
data class VoteTrackDto(
    val points: List<VotePointDto> = emptyList(),
)

/** A single hourly total. Both FOR and AGAINST tracks use this same XML shape. */
@Serializable
@XmlSerialName("N", "", "")
data class VotePointDto(
    @XmlValue(true) val votes: Int = 0,
)

/** Delegates on one side of a resolution, returned by `delvotes`. */
@Serializable
@XmlSerialName("DELVOTES_FOR", "", "")
data class DelegateVotesDto(
    val delegates: List<DelegateVoteDto> = emptyList(),
)

@Serializable
@XmlSerialName("DELEGATE", "", "")
data class DelegateVoteDto(
    @XmlElement(true) @XmlSerialName("NATION", "", "") val nationId: String = "",
    @XmlElement(true) @XmlSerialName("VOTES", "", "") val votes: Int = 0,
)
