package dev.cazh0.civily.data.nation

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * A nation as returned by `api.cgi?nation=…`.
 *
 * Why this doubles as the domain model: the shape the API returns is the shape the screen
 * shows, so a separate domain type and mapper would be an abstraction with one caller —
 * which spec §2 R2a forbids. Introduce one when a second consumer needs a different shape,
 * not before.
 *
 * Every field is defaulted because the API omits elements for shards it has no data for.
 */
@Serializable
@XmlSerialName("NATION", "", "")
data class NationDto(
    @XmlElement(true) @XmlSerialName("NAME", "", "") val name: String = "",
    /** The nation's pretitle, e.g. "Republic". Rendered as "The {type} of {name}". */
    @XmlElement(true) @XmlSerialName("TYPE", "", "") val type: String = "",
    @XmlElement(true) @XmlSerialName("MOTTO", "", "") val motto: String = "",
    @XmlElement(true) @XmlSerialName("FLAG", "", "") val flagUrl: String = "",
    @XmlElement(true) @XmlSerialName("CATEGORY", "", "") val category: String = "",
    @XmlElement(true) @XmlSerialName("REGION", "", "") val region: String = "",
    /** Population in millions, as the API reports it. */
    @XmlElement(true) @XmlSerialName("POPULATION", "", "") val population: Int = 0,
    /** World Assembly membership, e.g. "WA Member", "Non-member". */
    @XmlElement(true) @XmlSerialName("UNSTATUS", "", "") val waStatus: String = "",
    /** Regional influence rank, e.g. "Nipper", "Powerbroker". */
    @XmlElement(true) @XmlSerialName("INFLUENCE", "", "") val influence: String = "",
    /** What the nation's people are called in the plural, e.g. "Testlandians". */
    @XmlElement(true) @XmlSerialName("DEMONYM2PLURAL", "", "") val people: String = "",
    @XmlElement(true) @XmlSerialName("CURRENCY", "", "") val currency: String = "",
    @XmlElement(true) @XmlSerialName("ANIMAL", "", "") val animal: String = "",
) {
    /** Delegates are members too, which is why this is not a equality check against one string. */
    val isWaMember: Boolean get() = waStatus == WA_MEMBER || waStatus == WA_DELEGATE
}

// Why file scope and not a companion: `@Serializable` generates a public `Companion` to hold
// `serializer()`. Declaring a private one takes that name and makes the type unserialisable.
private const val WA_MEMBER = "WA Member"
private const val WA_DELEGATE = "WA Delegate"
