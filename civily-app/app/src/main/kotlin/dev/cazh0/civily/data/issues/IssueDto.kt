package dev.cazh0.civily.data.issues

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

/** The private `issues` shard: `api.cgi?nation=…&q=issues`. */
@Serializable
@XmlSerialName("NATION", "", "")
data class IssuesPageDto(
    /** The nation's capital. The newspaper presentation is named after it, as on the website. */
    @XmlElement(true) @XmlSerialName("CAPITAL", "", "") val capital: String = "",
    /** Flown on the masthead, as on the website. */
    @XmlElement(true) @XmlSerialName("FLAG", "", "") val flagUrl: String = "",
    /** Printed as the cover price — "1 DOLLAR" on the design's example page. */
    @XmlElement(true) @XmlSerialName("CURRENCY", "", "") val currency: String = "",
    /** Unix seconds. See [IssueBadgeDto.nextIssueTime] for the sentinel. */
    @XmlElement(true) @XmlSerialName("NEXTISSUETIME", "", "") val nextIssueTime: Long = 0,
    val issues: IssueListDto = IssueListDto(),
)

/**
 * The Issues button's counter: `api.cgi?nation=…&q=unread+nextissuetime`.
 *
 * Why not the `issues` shard the screen behind the button uses: a count and an instant are two
 * numbers, and the full shard is every issue's prose and five options each. The button asks for
 * what the button shows.
 */
@Serializable
@XmlSerialName("NATION", "", "")
data class IssueBadgeDto(
    val unread: UnreadDto = UnreadDto(),
    /**
     * Unix seconds. Absent — a nation with no scheduled issue — decodes to the zero the
     * repository turns into a null, the same sentinel handling the rest of `data/` uses.
     */
    @XmlElement(true) @XmlSerialName("NEXTISSUETIME", "", "") val nextIssueTime: Long = 0,
)

/**
 * The `unread` shard's counters. Only issues are read here.
 *
 * The other four counters — telegrams, notices, the RMB, the World Assembly — arrive in the
 * same element and are deliberately not modelled: nothing in the app has a place to show them,
 * and a field nobody reads is a field that goes stale without anyone noticing.
 */
@Serializable
@XmlSerialName("UNREAD", "", "")
data class UnreadDto(
    @XmlElement(true) @XmlSerialName("ISSUES", "", "") val issues: Int = 0,
)

@Serializable
@XmlSerialName("ISSUES", "", "")
data class IssueListDto(
    val issues: List<IssueDto> = emptyList(),
)

@Serializable
@XmlSerialName("ISSUE", "", "")
data class IssueDto(
    /** An attribute. This is the number the answer command takes. */
    val id: Int = 0,
    @XmlElement(true) @XmlSerialName("TITLE", "", "") val title: String = "",
    @XmlElement(true) @XmlSerialName("TEXT", "", "") val text: String = "",
    /** Newspaper image id. The artwork lives at `/images/newspaper/{PIC1}-1.jpg`. */
    @XmlElement(true) @XmlSerialName("PIC1", "", "") val primaryImageId: String = "",
    /** Newspaper image id. The artwork lives at `/images/newspaper/{PIC2}-2.jpg`. */
    @XmlElement(true) @XmlSerialName("PIC2", "", "") val secondaryImageId: String = "",
    val options: List<OptionDto> = emptyList(),
)

/**
 * One course of action.
 *
 * The option's own text is the element's content rather than a child element, hence
 * [XmlValue]. Its [id] is what `c=issue` expects, and the API counts these from zero — so the
 * id must be carried through rather than inferred from list position.
 */
@Serializable
@XmlSerialName("OPTION", "", "")
data class OptionDto(
    val id: Int = 0,
    @XmlValue(true) val text: String = "",
)
