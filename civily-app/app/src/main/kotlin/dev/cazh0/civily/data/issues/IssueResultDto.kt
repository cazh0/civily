package dev.cazh0.civily.data.issues

import dev.cazh0.civily.data.nation.PolicyDto
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

/**
 * What `c=issue` returns once legislation is enacted.
 *
 * Captured from a live answer rather than guessed — the shape is
 * `<NATION><ISSUE id choice><OK/><DESC/><RANKINGS/><UNLOCKS/><RECLASSIFICATIONS/>
 * <NEW_POLICIES/><HEADLINES/></ISSUE></NATION>`. Not every section appears on every answer:
 * the sample this was read from carried no `REMOVED_POLICIES`, and the API documents that
 * element alongside the others.
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
    val unlocks: UnlocksDto = UnlocksDto(),
    val reclassifications: ReclassificationsDto = ReclassificationsDto(),
    val newPolicies: NewPoliciesDto = NewPoliciesDto(),
    val removedPolicies: RemovedPoliciesDto = RemovedPoliciesDto(),
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

/**
 * The Rift banners the legislation earned the nation — what the game calls postcards.
 *
 * `<UNLOCKS><BANNER>s1</BANNER></UNLOCKS>`, read off a live answer. `s1` is an artwork code,
 * not a title: `NsUrl.banner` is the only thing that turns it into something a reader sees.
 */
@Serializable
@XmlSerialName("UNLOCKS", "", "")
data class UnlocksDto(
    val banners: List<BannerDto> = emptyList(),
)

@Serializable
@XmlSerialName("BANNER", "", "")
data class BannerDto(
    @XmlValue(true) val id: String = "",
) {
    val bannerId: String get() = id.trim()
}

/**
 * A rating the answer moved far enough to change the word the game uses for it.
 *
 * `<RECLASSIFY type="0"><FROM>Some</FROM><TO>Few</TO></RECLASSIFY>`, read off a live answer.
 * The type is a code, not a name: `0`, `1` and `2` are the three freedom ratings and `govt` is
 * the nation's classification. That mapping comes from the legacy client, and the one live
 * sample agrees with it — `type="0"` moved a nation's civil rights the same turn its Political
 * Freedoms census fell. A code this app does not know names nothing, so it is dropped rather
 * than labelled with a guess.
 */
@Serializable
@XmlSerialName("RECLASSIFICATIONS", "", "")
data class ReclassificationsDto(
    val reclassifications: List<ReclassifyDto> = emptyList(),
)

@Serializable
@XmlSerialName("RECLASSIFY", "", "")
data class ReclassifyDto(
    val type: String = "",
    @XmlElement(true) @XmlSerialName("FROM", "", "") val from: String = "",
    @XmlElement(true) @XmlSerialName("TO", "", "") val to: String = "",
)

/**
 * Policies the answer put on the books, and policies it took off them.
 *
 * Both carry the same `<POLICY>` the `policies` shard does — name, artwork code, category and
 * a one-line description — so [PolicyDto] is shared rather than restated. Two containers and
 * not one flag on the element: the API sends two, and which list a policy came from is the
 * whole difference between enacting and cancelling it.
 */
@Serializable
@XmlSerialName("NEW_POLICIES", "", "")
data class NewPoliciesDto(
    val policies: List<PolicyDto> = emptyList(),
)

@Serializable
@XmlSerialName("REMOVED_POLICIES", "", "")
data class RemovedPoliciesDto(
    val policies: List<PolicyDto> = emptyList(),
)

@Serializable
@XmlSerialName("HEADLINES", "", "")
data class HeadlinesDto(
    val headlines: List<HeadlineDto> = emptyList(),
)

/**
 * A headline, and nothing else.
 *
 * No artwork: unlike `<ISSUE>` in the issues shard, which carries `PIC1` and `PIC2`, a
 * `<HEADLINE>` from `c=issue` is bare text — checked against a live enactment, not assumed.
 * The site's aftermath page is the only place the game names those cutouts, and Civily cannot
 * reach it; `README.md` records why.
 */
@Serializable
@XmlSerialName("HEADLINE", "", "")
data class HeadlineDto(
    @XmlValue(true) val text: String = "",
) {
    val displayText: String get() = text.trim()
}
