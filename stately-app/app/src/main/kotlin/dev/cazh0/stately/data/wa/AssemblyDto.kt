package dev.cazh0.stately.data.wa

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/** Which chamber. The API addresses them by number, so the number is the identity. */
enum class Council(val id: Int) {
    GeneralAssembly(1),
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
) {
    /**
     * The one thing the wire shape decides. Vote arithmetic lives on [Resolution], so it is
     * defined once rather than on both sides of the mapping.
     */
    val isAtVote: Boolean get() = name.isNotEmpty()
}
