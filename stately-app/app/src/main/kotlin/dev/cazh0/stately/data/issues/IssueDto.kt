package dev.cazh0.stately.data.issues

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
    val issues: IssueListDto = IssueListDto(),
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
    /** Banner image id. The artwork lives at `/images/banners/{PIC1}.jpg`. */
    @XmlElement(true) @XmlSerialName("PIC1", "", "") val bannerId: String = "",
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
