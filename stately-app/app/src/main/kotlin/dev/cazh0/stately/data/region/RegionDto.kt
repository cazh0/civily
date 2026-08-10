package dev.cazh0.stately.data.region

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * A region as returned by `api.cgi?region=…`.
 *
 * [delegate] and [founder] are nation ids, or the string `"0"` when the region has neither.
 * That sentinel is the API's, not ours — see [hasDelegate] and [hasFounder].
 */
@Serializable
@XmlSerialName("REGION", "", "")
data class RegionDto(
    @XmlElement(true) @XmlSerialName("NAME", "", "") val name: String = "",
    @XmlElement(true) @XmlSerialName("FLAG", "", "") val flagUrl: String = "",
    @XmlElement(true) @XmlSerialName("NUMNATIONS", "", "") val nationCount: Int = 0,
    @XmlElement(true) @XmlSerialName("DELEGATE", "", "") val delegate: String = "",
    @XmlElement(true) @XmlSerialName("DELEGATEVOTES", "", "") val delegateVotes: Int = 0,
    @XmlElement(true) @XmlSerialName("FOUNDER", "", "") val founder: String = "",
    /** The region's World Factbook Entry, as BBCode. */
    @XmlElement(true) @XmlSerialName("FACTBOOK", "", "") val factbook: String = "",
) {
    val hasDelegate: Boolean get() = delegate.isNotEmpty() && delegate != NONE

    val hasFounder: Boolean get() = founder.isNotEmpty() && founder != NONE
}

/**
 * Why file scope and not a companion: `@Serializable` generates a public `Companion` to hold
 * `serializer()`. Declaring a private one takes that name and makes the type unserialisable.
 */
private const val NONE = "0"
