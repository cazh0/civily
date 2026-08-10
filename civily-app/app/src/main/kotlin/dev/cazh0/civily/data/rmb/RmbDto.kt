package dev.cazh0.civily.data.rmb

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * A regional message board page as returned by `api.cgi?region=…&q=messages`.
 *
 * The API returns oldest first and defaults to ten posts.
 */
@Serializable
@XmlSerialName("REGION", "", "")
data class RmbPageDto(
    val messages: MessagesDto = MessagesDto(),
)

@Serializable
@XmlSerialName("MESSAGES", "", "")
data class MessagesDto(
    val posts: List<PostDto> = emptyList(),
)

@Serializable
@XmlSerialName("POST", "", "")
data class PostDto(
    /** An attribute, not an element — hence no `@XmlElement`. */
    val id: String = "",
    @XmlElement(true) @XmlSerialName("TIMESTAMP", "", "") val timestamp: Long = 0,
    /** The author's nation id. */
    @XmlElement(true) @XmlSerialName("NATION", "", "") val nation: String = "",
    /** 0 visible, 1 suppressed by a moderator or officer, 2 deleted by its author. */
    @XmlElement(true) @XmlSerialName("STATUS", "", "") val status: Int = 0,
    @XmlElement(true) @XmlSerialName("LIKES", "", "") val likes: Int = 0,
    /** Present only when the post was edited; the instant of the edit. */
    @XmlElement(true) @XmlSerialName("EDITED", "", "") val edited: Long = 0,
    /** Set when the post came from an embassy region rather than this one. */
    @XmlElement(true) @XmlSerialName("EMBASSY", "", "") val embassy: String = "",
    /** BBCode, delivered in a CDATA section. */
    @XmlElement(true) @XmlSerialName("MESSAGE", "", "") val message: String = "",
)
