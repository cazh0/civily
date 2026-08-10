package dev.cazh0.civily.data.region

import dev.cazh0.civily.core.net.NsClient
import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.core.result.Outcome
import dev.cazh0.civily.core.result.flatMap
import dev.cazh0.civily.core.result.map
import dev.cazh0.civily.core.text.bbcode.BbParser
import dev.cazh0.civily.data.decodeNsXml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fetches regions. Knows which shards the region screen needs and nothing about the UI. */
class RegionRepository(private val client: NsClient) {

    /** Runs on [Dispatchers.Default] so XML decoding and BBCode parsing stay off the main thread. */
    suspend fun load(regionId: String): Outcome<Region> = withContext(Dispatchers.Default) {
        client.get(NsUrl.api(NsUrl.Target.Region(regionId), SHARDS))
            .flatMap { body -> decodeNsXml<RegionDto>(TAG, body) }
            .map { dto -> dto.toRegion() }
    }

    private fun RegionDto.toRegion() = Region(
        name = name,
        flagUrl = flagUrl,
        nationCount = nationCount,
        delegate = delegate.takeIf { hasDelegate },
        delegateVotes = delegateVotes,
        founder = founder.takeIf { hasFounder },
        factbook = BbParser.parse(factbook),
    )

    private companion object {
        const val TAG = "Region"

        /** Documented shards only (spec §3). Each one maps to a field on [RegionDto]. */
        val SHARDS = listOf(
            "name",
            "flag",
            "numnations",
            "delegate",
            "delegatevotes",
            "founder",
            "factbook",
        )
    }
}
