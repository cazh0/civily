package dev.cazh0.civily.data.nation

import dev.cazh0.civily.core.text.bbcode.BbBlock

/**
 * A nation in the shape its screen shows it.
 *
 * Everything here is ready to draw: entities are decoded, the feed's markup is parsed, and the
 * game's prose is a tree rather than a string. That is the work [NationRepository] does on a
 * background dispatcher so a composable never does it — a composable runs again on every
 * recomposition, and parsing eleven paragraphs each time is exactly the main-thread cost
 * spec §2 R2b forbids.
 *
 * The grouping is the screen's seven tabs. Fields a reader wants at a glance sit at the top
 * level; the four subjects that need a page of their own get a type, so a pane takes the one
 * object it draws and cannot reach the rest.
 *
 * A field the API had no data for is the empty string, or zero. The screen drops the tile rather
 * than showing a label with nothing under it, so absence needs no sentinel.
 */
data class Nation(
    /** The id the nation was fetched by — the name in URL form. */
    val id: String,
    val name: String,
    /** The pretitle, e.g. "Hive Mind". Rendered as "The {type} of {name}". */
    val type: String,
    val motto: String,
    val flagUrl: String,

    // ------------------------------------------------------------------ overview
    val region: String,
    val category: String,
    /** Millions, as the API counts them. Goes through `Population` before anybody reads it. */
    val population: Int,
    val waStatus: String,
    val influence: String,
    val endorsementCount: Int,
    val issuesAnswered: Int,
    /**
     * Unix time, or zero for a nation older than the record — which the game itself renders as
     * "Antiquity" rather than a date. The sentinel is the API's; the screen reads it, it does not
     * repair it.
     */
    val foundedEpochSeconds: Long,
    /** Unix time of the nation's last login. Zero when the API sent none. */
    val lastActiveEpochSeconds: Long,

    // ------------------------------------------------------------------ freedoms
    val civilRights: String,
    val economyRating: String,
    val politicalFreedom: String,

    // ------------------------------------------------------------------ identity
    val capital: String,
    val leader: String,
    val religion: String,
    val currency: String,
    /** The singular demonym, e.g. "Testlandian". */
    val demonym: String,
    /** The plural demonym, e.g. "Testlandians". */
    val demonymPlural: String,
    val animal: String,
    val animalTrait: String,
    val notable: String,
    val admirable: String,
    val sensibilities: String,

    // ------------------------------------------------------------------ tabs
    val people: People,
    val government: Government,
    val economy: Economy,
    val policies: List<Policy>,
    val rankings: List<Ranking>,
    /** Newest first, as the API returns them. */
    val happenings: List<Happening>,
)

/** What the game says about a nation's population, and what it dies of. */
data class People(
    /** The game's finished paragraph about crime. */
    val crime: List<BbBlock>,
    /** Largest share first. */
    val causesOfDeath: List<Cause>,
)

data class Government(
    /** The department taking the largest share, e.g. "the Environment". */
    val priority: String,
    val taxPercent: Double,
    /** The game's finished paragraph about the government. */
    val description: List<BbBlock>,
    /** Largest share first. A department funded at zero is not listed. */
    val budget: List<Spend>,
)

data class Economy(
    val gdp: Long,
    val averageIncome: Long,
    val poorestIncome: Long,
    val richestIncome: Long,
    val majorIndustry: String,
    /** The game's finished paragraph about the economy. */
    val description: List<BbBlock>,
    /** Largest share first. A sector at zero is not listed. */
    val sectors: List<Sector>,
)

/** One enacted policy. [imageId] is a Rift banner code; `NsUrl.banner` addresses it. */
data class Policy(
    val name: String,
    val imageId: String,
    val category: String,
    val description: String,
)

/** One cause of death, as a percentage of all deaths. */
data class Cause(val name: String, val percent: Double)

/** One department's share of the government's budget, as a percentage. */
data class Spend(val department: Department, val percent: Double)

/**
 * The twelve departments a government funds.
 *
 * Why an enum and not the API's element names: two of those names are the game's own
 * vocabulary rather than plain English — `COMMERCE` is the industry budget and `SOCIALEQUALITY`
 * is social policy — and a screen printing either would be printing a wire detail at the user.
 */
enum class Department {
    Administration,
    Defence,
    Education,
    Environment,
    Healthcare,
    Industry,
    InternationalAid,
    LawAndOrder,
    PublicTransport,
    SocialPolicy,
    Spirituality,
    Welfare,
}

/** One sector's share of the economy, as a percentage. */
data class Sector(val kind: Kind, val percent: Double) {
    enum class Kind { Government, StateOwned, PrivateIndustry, BlackMarket }
}

/**
 * Where a nation places on one World Census scale.
 *
 * [scaleId] indexes `R.array.census_scales`, which is why it stays an id here: the name and the
 * unit are strings, and strings live in the resource layer.
 */
data class Ranking(
    val scaleId: Int,
    val score: Double,
    val worldRank: Int,
    /** World rank as a percentage — 0.09 means the top tenth of one per cent. */
    val percentile: Double,
)

/** One line of the nation's feed, with the nations and regions in it already linked. */
data class Happening(
    val atEpochSeconds: Long,
    val text: List<BbBlock>,
)
