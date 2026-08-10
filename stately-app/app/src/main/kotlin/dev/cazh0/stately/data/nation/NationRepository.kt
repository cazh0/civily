package dev.cazh0.stately.data.nation

import dev.cazh0.stately.core.net.NsClient
import dev.cazh0.stately.core.net.NsUrl
import dev.cazh0.stately.core.result.Outcome
import dev.cazh0.stately.core.result.flatMap
import dev.cazh0.stately.data.decodeNsXml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches nations. Knows which shards the nation screen needs and nothing about the UI.
 */
class NationRepository(private val client: NsClient) {

    suspend fun load(nationId: String): Outcome<NationDto> = fetch(nationId, SHARDS)

    /**
     * Just enough to show a nation exists and let the user recognise it.
     *
     * Why separate from [load]: search results are answered by this, and asking for the full
     * shard set to render one row spends the request budget on data nobody looks at.
     */
    suspend fun loadPreview(nationId: String): Outcome<NationDto> = fetch(nationId, PREVIEW_SHARDS)

    /**
     * Runs on [Dispatchers.Default] because `NsClient` puts only the network call on a
     * background thread — decoding would otherwise happen on the caller's thread, which is
     * the main one (spec §2 R2b).
     */
    private suspend fun fetch(nationId: String, shards: List<String>): Outcome<NationDto> =
        withContext(Dispatchers.Default) {
            client.get(NsUrl.api(NsUrl.Target.Nation(nationId), shards))
                .flatMap { body -> decodeNsXml<NationDto>(TAG, body) }
        }

    private companion object {
        const val TAG = "Nation"

        val PREVIEW_SHARDS = listOf("name", "flag", "region")

        /** Documented shards only (spec §3). Each one maps to a field on [NationDto]. */
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
        )
    }
}
