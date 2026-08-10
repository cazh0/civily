package dev.cazh0.civily.data.issues

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * What `c=issue` returns once legislation is enacted.
 *
 * Captured from a live answer rather than guessed — the shape is
 * `<NATION><ISSUE id choice><OK/><DESC/><RANKINGS/><HEADLINES/></ISSUE></NATION>`.
 */
@Serializable
@XmlSerialName("NATION", "", "")
data class IssueResultPageDto(
    val issue: IssueResultDto = IssueResultDto(),
)

@Serializable
@XmlSerialName("ISSUE", "", "")
data class IssueResultDto(
    val id: Int = 0,
    /** The option the nation actually enacted, echoed back by the server. */
    val choice: Int = -1,
    @XmlElement(true) @XmlSerialName("OK", "", "") val ok: Int = 0,
    /**
     * The consequence, as a sentence fragment in the game's own voice — "ice-filled coffins
     * are ominously positioned in the corner of every hospital ward". It has no leading
     * capital and no full stop because the site prints it inside a longer sentence.
     */
    @XmlElement(true) @XmlSerialName("DESC", "", "") val description: String = "",
    val rankings: RankingsDto = RankingsDto(),
    val headlines: HeadlinesDto = HeadlinesDto(),
)

@Serializable
@XmlSerialName("RANKINGS", "", "")
data class RankingsDto(
    val ranks: List<RankDto> = emptyList(),
)

@Serializable
@XmlSerialName("RANK", "", "")
data class RankDto(
    /** The census scale, by the id that indexes `R.array.census_scales`. */
    val id: Int = 0,
    @XmlElement(true) @XmlSerialName("SCORE", "", "") val score: Double = 0.0,
    @XmlElement(true) @XmlSerialName("CHANGE", "", "") val change: Double = 0.0,
    @XmlElement(true) @XmlSerialName("PCHANGE", "", "") val percentChange: Double = 0.0,
)

@Serializable
@XmlSerialName("HEADLINES", "", "")
data class HeadlinesDto(
    val headlines: List<HeadlineDto> = emptyList(),
)

@Serializable
@XmlSerialName("HEADLINE", "", "")
data class HeadlineDto(
    @nl.adaptivity.xmlutil.serialization.XmlValue(true) val text: String = "",
)
