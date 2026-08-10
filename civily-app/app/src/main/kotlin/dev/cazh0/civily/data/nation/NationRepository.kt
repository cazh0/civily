package dev.cazh0.civily.data.nation

import dev.cazh0.civily.core.net.NsClient
import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.core.result.Outcome
import dev.cazh0.civily.core.result.flatMap
import dev.cazh0.civily.core.result.map
import dev.cazh0.civily.core.text.HtmlEntities
import dev.cazh0.civily.core.text.bbcode.BbBlock
import dev.cazh0.civily.core.text.bbcode.BbParser
import dev.cazh0.civily.core.text.bbcode.BbSpan
import dev.cazh0.civily.core.text.bbcode.BbTarget
import dev.cazh0.civily.data.decodeNsXml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches nations. Knows which shards the nation screen needs and nothing about the UI.
 */
class NationRepository(private val client: NsClient) {

    suspend fun load(nationId: String): Outcome<Nation> = withContext(Dispatchers.Default) {
        request(nationId, SHARDS, CENSUS_OPTIONS).map { it.toNation(nationId) }
    }

    /**
     * Just enough to show a nation exists and let the user recognise it.
     *
     * Why separate from [load]: search results are answered by this, and asking for the full
     * shard set to render one row spends the request budget on data nobody looks at. It stays
     * on the wire shape because a row shows two fields and needs nothing decoded.
     */
    suspend fun loadPreview(nationId: String): Outcome<NationDto> =
        withContext(Dispatchers.Default) { request(nationId, PREVIEW_SHARDS) }

    /**
     * Both callers wrap this in [Dispatchers.Default] because `NsClient` puts only the network
     * call on a background thread — decoding, and the mapping after it, would otherwise happen
     * on the caller's thread, which is the main one (spec §2 R2b).
     */
    private suspend fun request(
        nationId: String,
        shards: List<String>,
        options: Map<String, String> = emptyMap(),
    ): Outcome<NationDto> =
        client.get(NsUrl.api(NsUrl.Target.Nation(nationId), shards, options))
            .flatMap { body -> decodeNsXml<NationDto>(TAG, body) }

    private fun NationDto.toNation(id: String) = Nation(
        id = id,
        name = text(name),
        type = text(type),
        motto = text(motto),
        flagUrl = flagUrl,
        region = text(region),
        category = text(category),
        population = population,
        waStatus = text(waStatus),
        influence = text(influence),
        // A comma-separated list of ids, and empty when nobody endorses this nation — which
        // `split` would report as one endorsement rather than none.
        endorsementCount = endorsements.split(',').count { it.isNotBlank() },
        issuesAnswered = issuesAnswered,
        foundedEpochSeconds = foundedTime,
        lastActiveEpochSeconds = lastLogin,
        civilRights = text(freedom.civilRights),
        economyRating = text(freedom.economy),
        politicalFreedom = text(freedom.politicalFreedom),
        capital = text(capital),
        leader = text(leader),
        religion = text(religion),
        currency = text(currency),
        demonym = text(person),
        demonymPlural = text(people),
        animal = text(animal),
        animalTrait = text(animalTrait),
        notable = text(notable),
        admirable = text(admirable),
        sensibilities = text(sensibilities),
        people = People(
            // Parsed rather than printed even though the game writes these itself: the README's
            // rule is that anything rendering NationStates content goes through BbParser, and
            // that is what guarantees no reader sees a tag the day the game dresses these.
            crime = BbParser.parse(crime),
            causesOfDeath = deaths.causes
                .map { Cause(text(it.type), it.percent) }
                .sortedByDescending { it.percent },
        ),
        government = Government(
            priority = text(govtPriority),
            taxPercent = tax,
            description = BbParser.parse(governmentDescription),
            budget = govt.toBudget(),
        ),
        economy = Economy(
            gdp = gdp,
            averageIncome = income,
            poorestIncome = poorest,
            richestIncome = richest,
            majorIndustry = text(majorIndustry),
            description = BbParser.parse(industryDescription),
            sectors = sectors.toSectors(),
        ),
        policies = policies.policies.map {
            Policy(
                name = text(it.name),
                imageId = it.imageId,
                category = text(it.category),
                description = text(it.description),
            )
        },
        rankings = census.scales.map {
            Ranking(
                scaleId = it.id,
                score = it.score,
                worldRank = it.worldRank,
                percentile = it.percentile,
            )
        },
        happenings = happenings.events.map { it.toHappening(id) },
    )

    /**
     * Why zeroes are dropped rather than drawn: eleven of the twelve departments read 0.0 on a
     * single-issue nation, and a chart of empty bars says nothing while costing a screenful.
     * Why sorted: the question a budget answers is what this government cares about, and that
     * is the order that answers it.
     */
    private fun GovtDto.toBudget(): List<Spend> = listOf(
        Spend(Department.Administration, administration),
        Spend(Department.Defence, defence),
        Spend(Department.Education, education),
        Spend(Department.Environment, environment),
        Spend(Department.Healthcare, healthcare),
        Spend(Department.Industry, commerce),
        Spend(Department.InternationalAid, internationalAid),
        Spend(Department.LawAndOrder, lawAndOrder),
        Spend(Department.PublicTransport, publicTransport),
        Spend(Department.SocialPolicy, socialEquality),
        Spend(Department.Spirituality, spirituality),
        Spend(Department.Welfare, welfare),
    ).filter { it.percent > 0 }.sortedByDescending { it.percent }

    private fun SectorsDto.toSectors(): List<Sector> = listOf(
        Sector(Sector.Kind.Government, government),
        Sector(Sector.Kind.StateOwned, stateOwned),
        Sector(Sector.Kind.PrivateIndustry, privateIndustry),
        Sector(Sector.Kind.BlackMarket, blackMarket),
    ).filter { it.percent > 0 }.sortedByDescending { it.percent }

    private fun EventDto.toHappening(selfId: String) = Happening(
        atEpochSeconds = timestamp,
        text = BbParser.parseHappening(text).map { it.withoutLinkTo(selfId) },
    )

    /**
     * Why the nation's own name is not a link on its own screen: every line of its feed names
     * it, and a link that pushes the screen the reader is already on is a back press they now
     * owe. The words stay; only the link goes.
     */
    private fun BbBlock.withoutLinkTo(nationId: String): BbBlock {
        if (this !is BbBlock.Paragraph) return this
        val self = BbTarget.Nation(nationId)
        return copy(
            spans = spans.map { span ->
                if (span is BbSpan.Link && span.target == self) {
                    BbSpan.Plain(span.text, span.style)
                } else {
                    span
                }
            },
        )
    }

    /**
     * Why every field is decoded and not just the long ones: NationStates escapes what it
     * stores on the way out, so a nation's animal arrives as `&#9733;&#9733;&#9733; nautilus`
     * and a motto arrives with the author's own entities intact. The long fields get the same
     * treatment inside [BbParser]; these have no markup to parse, only text to read.
     */
    private fun text(raw: String): String = HtmlEntities.decode(raw)

    private companion object {
        const val TAG = "Nation"

        val PREVIEW_SHARDS = listOf("name", "flag", "region")

        /**
         * Every World Census scale, with the three numbers the Rankings tab shows.
         *
         * Why every scale rather than a handful: the tab is the game's own ranking page, and
         * asking per scale would be ninety requests against a fifty-per-thirty-seconds budget.
         * `mode` is a documented parameter of the `census` shard, and its `+` survives being
         * percent-encoded — checked against the live API.
         */
        val CENSUS_OPTIONS = mapOf("scale" to "all", "mode" to "score+rank+prank")

        /**
         * Documented shards only (spec §3). Each one maps to a field on [NationDto], and they
         * travel in one request because the API's own guidance is to combine them rather than
         * spend a rate-limit slot per field.
         */
        val SHARDS = listOf(
            "name",
            "type",
            "motto",
            "flag",
            "category",
            "region",
            "population",
            "wa",
            "influence",
            "demonym2plural",
            "currency",
            "animal",
            "animaltrait",
            "capital",
            "leader",
            "religion",
            "freedom",
            "notable",
            "admirable",
            "sensibilities",
            "govtdesc",
            "industrydesc",
            "crime",
            "happenings",
            "govtpriority",
            "tax",
            "govt",
            "gdp",
            "income",
            "poorest",
            "richest",
            "majorindustry",
            "sectors",
            "demonym2",
            "deaths",
            "answered",
            "endorsements",
            "foundedtime",
            "lastlogin",
            "policies",
            "census",
        )
    }
}
