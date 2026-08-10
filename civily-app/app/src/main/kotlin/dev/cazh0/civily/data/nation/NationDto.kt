package dev.cazh0.civily.data.nation

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * A nation as returned by `api.cgi?nation=…`.
 *
 * The wire shape only. [NationRepository] maps it to [Nation], which is what the screen reads:
 * the prose shards arrive as markup and as HTML entities, and decoding them is real work that
 * belongs on a background dispatcher rather than in a composable.
 *
 * Sign-in still reads this type directly — it wants one field off a `ping` response and has no
 * use for a screen model.
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
    /** What the national animal does, e.g. "frolics freely in the nation's sparkling oceans". */
    @XmlElement(true) @XmlSerialName("ANIMALTRAIT", "", "") val animalTrait: String = "",
    /** The capital's name. Player-set on nations that have bought the custom-capital option. */
    @XmlElement(true) @XmlSerialName("CAPITAL", "", "") val capital: String = "",
    @XmlElement(true) @XmlSerialName("LEADER", "", "") val leader: String = "",
    @XmlElement(true) @XmlSerialName("RELIGION", "", "") val religion: String = "",
    val freedom: FreedomDto = FreedomDto(),
    /**
     * A few of the things the nation is known for, as one comma-joined phrase:
     * "museums and concert halls, compulsory vegetarianism, and hatred of cheese".
     *
     * The API picks which few, and picks again on every request — the `notables` shard is the
     * full list. That is the game's own behaviour, so a refresh legitimately changes the wording
     * of the summary sentence built from it.
     */
    @XmlElement(true) @XmlSerialName("NOTABLE", "", "") val notable: String = "",
    /** One thing the nation is admired for, e.g. "environmentally stunning". Also rotates. */
    @XmlElement(true) @XmlSerialName("ADMIRABLE", "", "") val admirable: String = "",
    /** How its people are, e.g. "compassionate, democratic". */
    @XmlElement(true) @XmlSerialName("SENSIBILITIES", "", "") val sensibilities: String = "",
    /** A finished paragraph about the government. Delivered in a CDATA section. */
    @XmlElement(true) @XmlSerialName("GOVTDESC", "", "") val governmentDescription: String = "",
    /** A finished paragraph about the economy. */
    @XmlElement(true) @XmlSerialName("INDUSTRYDESC", "", "") val industryDescription: String = "",
    /** A finished paragraph about crime. */
    @XmlElement(true) @XmlSerialName("CRIME", "", "") val crime: String = "",
    val happenings: HappeningsDto = HappeningsDto(),

    // ------------------------------------------------------------- government
    /** The department taking the largest share of the budget, e.g. "the Environment". */
    @XmlElement(true) @XmlSerialName("GOVTPRIORITY", "", "") val govtPriority: String = "",
    /** Income tax rate, as a percentage. */
    @XmlElement(true) @XmlSerialName("TAX", "", "") val tax: Double = 0.0,
    val govt: GovtDto = GovtDto(),

    // ------------------------------------------------------------- economy
    /** Gross domestic product in the nation's own currency. Runs past `Int` on big nations. */
    @XmlElement(true) @XmlSerialName("GDP", "", "") val gdp: Long = 0,
    /** Average income, in the nation's own currency. */
    @XmlElement(true) @XmlSerialName("INCOME", "", "") val income: Long = 0,
    /** Average income of the poorest tenth. */
    @XmlElement(true) @XmlSerialName("POOREST", "", "") val poorest: Long = 0,
    /** Average income of the richest tenth. */
    @XmlElement(true) @XmlSerialName("RICHEST", "", "") val richest: Long = 0,
    @XmlElement(true) @XmlSerialName("MAJORINDUSTRY", "", "") val majorIndustry: String = "",
    val sectors: SectorsDto = SectorsDto(),

    // ------------------------------------------------------------- people
    /** The singular demonym noun, e.g. "Testlandian". */
    @XmlElement(true) @XmlSerialName("DEMONYM2", "", "") val person: String = "",
    val deaths: DeathsDto = DeathsDto(),

    // ------------------------------------------------------------- standing
    @XmlElement(true) @XmlSerialName("ISSUES_ANSWERED", "", "") val issuesAnswered: Int = 0,
    /**
     * When the nation was founded, as a Unix time — except `0`, which is the API's way of saying
     * the nation predates the record. The site prints those as "Antiquity".
     */
    @XmlElement(true) @XmlSerialName("FOUNDEDTIME", "", "") val foundedTime: Long = 0,
    /** When the nation last logged in, as a Unix time. */
    @XmlElement(true) @XmlSerialName("LASTLOGIN", "", "") val lastLogin: Long = 0,
    /** Comma-separated nation ids. Empty when nobody endorses this nation. */
    @XmlElement(true) @XmlSerialName("ENDORSEMENTS", "", "") val endorsements: String = "",
    val policies: PoliciesDto = PoliciesDto(),
    val census: CensusDto = CensusDto(),
) {
    /** Delegates are members too, which is why this is not a equality check against one string. */
    val isWaMember: Boolean get() = waStatus == WA_MEMBER || waStatus == WA_DELEGATE
}

/**
 * The three ratings the game prints as words rather than numbers.
 *
 * The `freedomscores` shard gives the same three as numbers; the words are what the site shows
 * and what a player recognises, so those are what Civily asks for.
 */
@Serializable
@XmlSerialName("FREEDOM", "", "")
data class FreedomDto(
    @XmlElement(true) @XmlSerialName("CIVILRIGHTS", "", "") val civilRights: String = "",
    @XmlElement(true) @XmlSerialName("ECONOMY", "", "") val economy: String = "",
    @XmlElement(true) @XmlSerialName("POLITICALFREEDOM", "", "") val politicalFreedom: String = "",
)

@Serializable
@XmlSerialName("HAPPENINGS", "", "")
data class HappeningsDto(
    val events: List<EventDto> = emptyList(),
)

/**
 * One line of the nation's feed.
 *
 * [text] is not plain prose: nations and regions in it are delimited with `@@` and `%%`, which
 * is what [dev.cazh0.civily.core.text.bbcode.BbParser.parseHappening] exists for.
 */
@Serializable
@XmlSerialName("EVENT", "", "")
data class EventDto(
    @XmlElement(true) @XmlSerialName("TIMESTAMP", "", "") val timestamp: Long = 0,
    @XmlElement(true) @XmlSerialName("TEXT", "", "") val text: String = "",
)

/**
 * Where the government spends, as a percentage of its budget per department.
 *
 * The element names are the game's own British spellings and its own vocabulary — `COMMERCE` is
 * the industry budget, `SOCIALEQUALITY` is social policy. They are translated once, in the
 * mapper, so no screen carries the API's names.
 */
@Serializable
@XmlSerialName("GOVT", "", "")
data class GovtDto(
    @XmlElement(true) @XmlSerialName("ADMINISTRATION", "", "") val administration: Double = 0.0,
    @XmlElement(true) @XmlSerialName("DEFENCE", "", "") val defence: Double = 0.0,
    @XmlElement(true) @XmlSerialName("EDUCATION", "", "") val education: Double = 0.0,
    @XmlElement(true) @XmlSerialName("ENVIRONMENT", "", "") val environment: Double = 0.0,
    @XmlElement(true) @XmlSerialName("HEALTHCARE", "", "") val healthcare: Double = 0.0,
    @XmlElement(true) @XmlSerialName("COMMERCE", "", "") val commerce: Double = 0.0,
    @XmlElement(true) @XmlSerialName("INTERNATIONALAID", "", "") val internationalAid: Double = 0.0,
    @XmlElement(true) @XmlSerialName("LAWANDORDER", "", "") val lawAndOrder: Double = 0.0,
    @XmlElement(true) @XmlSerialName("PUBLICTRANSPORT", "", "") val publicTransport: Double = 0.0,
    @XmlElement(true) @XmlSerialName("SOCIALEQUALITY", "", "") val socialEquality: Double = 0.0,
    @XmlElement(true) @XmlSerialName("SPIRITUALITY", "", "") val spirituality: Double = 0.0,
    @XmlElement(true) @XmlSerialName("WELFARE", "", "") val welfare: Double = 0.0,
)

/** What share of the economy each sector holds, as percentages. */
@Serializable
@XmlSerialName("SECTORS", "", "")
data class SectorsDto(
    @XmlElement(true) @XmlSerialName("GOVERNMENT", "", "") val government: Double = 0.0,
    @XmlElement(true) @XmlSerialName("INDUSTRY", "", "") val privateIndustry: Double = 0.0,
    @XmlElement(true) @XmlSerialName("PUBLIC", "", "") val stateOwned: Double = 0.0,
    @XmlElement(true) @XmlSerialName("BLACKMARKET", "", "") val blackMarket: Double = 0.0,
)

@Serializable
@XmlSerialName("DEATHS", "", "")
data class DeathsDto(
    val causes: List<CauseDto> = emptyList(),
)

/** `<CAUSE type="Old Age">92.7</CAUSE>` — the name is an attribute, the share is the text. */
@Serializable
@XmlSerialName("CAUSE", "", "")
data class CauseDto(
    val type: String = "",
    @nl.adaptivity.xmlutil.serialization.XmlValue(true) val percent: Double = 0.0,
)

@Serializable
@XmlSerialName("POLICIES", "", "")
data class PoliciesDto(
    val policies: List<PolicyDto> = emptyList(),
)

@Serializable
@XmlSerialName("POLICY", "", "")
data class PolicyDto(
    @XmlElement(true) @XmlSerialName("NAME", "", "") val name: String = "",
    /** A Rift banner code. `NsUrl.banner` turns it into an address. */
    @XmlElement(true) @XmlSerialName("PIC", "", "") val imageId: String = "",
    /** "Government", "Society", "Law & Order", "Economy", "International". */
    @XmlElement(true) @XmlSerialName("CAT", "", "") val category: String = "",
    @XmlElement(true) @XmlSerialName("DESC", "", "") val description: String = "",
)

@Serializable
@XmlSerialName("CENSUS", "", "")
data class CensusDto(
    val scales: List<ScaleDto> = emptyList(),
)

@Serializable
@XmlSerialName("SCALE", "", "")
data class ScaleDto(
    /** The census scale, by the id that indexes `R.array.census_scales`. */
    val id: Int = 0,
    @XmlElement(true) @XmlSerialName("SCORE", "", "") val score: Double = 0.0,
    @XmlElement(true) @XmlSerialName("RANK", "", "") val worldRank: Int = 0,
    /**
     * World rank as a percentage — "the top 15%". Fractional on the scales a nation leads:
     * Testlandia's population percentile comes back as `0.09`.
     */
    @XmlElement(true) @XmlSerialName("PRANK", "", "") val percentile: Double = 0.0,
)

// Why file scope and not a companion: `@Serializable` generates a public `Companion` to hold
// `serializer()`. Declaring a private one takes that name and makes the type unserialisable.
private const val WA_MEMBER = "WA Member"
private const val WA_DELEGATE = "WA Delegate"
