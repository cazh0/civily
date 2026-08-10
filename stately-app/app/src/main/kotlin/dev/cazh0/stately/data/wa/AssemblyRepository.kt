package dev.cazh0.stately.data.wa

import dev.cazh0.stately.core.net.NsClient
import dev.cazh0.stately.core.net.NsUrl
import dev.cazh0.stately.core.result.Outcome
import dev.cazh0.stately.core.result.flatMap
import dev.cazh0.stately.core.result.map
import dev.cazh0.stately.core.text.bbcode.BbParser
import dev.cazh0.stately.data.decodeNsXml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fetches a World Assembly chamber. */
class AssemblyRepository(private val client: NsClient) {

    /**
     * Why the whole body runs on [Dispatchers.Default]: `NsClient` puts only the network call
     * on a background thread, so XML decoding and BBCode parsing would otherwise land on
     * whichever thread called this — the main one. Spec §2 R2b forbids that, and a long
     * factbook or resolution is exactly the payload that would make it visible.
     */
    suspend fun load(council: Council): Outcome<Assembly> = withContext(Dispatchers.Default) {
        client.get(NsUrl.api(NsUrl.Target.Assembly(council.id), SHARDS))
            .flatMap { body -> decodeNsXml<AssemblyDto>(TAG, body) }
            .map { dto -> dto.toAssembly() }
    }

    private fun AssemblyDto.toAssembly() = Assembly(
        memberCount = memberCount,
        delegateCount = delegateCount,
        resolution = resolution.takeIf { it.isAtVote }?.let { dto ->
            Resolution(
                name = dto.name,
                category = dto.category,
                proposedBy = dto.proposedBy,
                votesFor = dto.votesFor,
                votesAgainst = dto.votesAgainst,
                body = BbParser.parse(dto.body),
            )
        },
    )

    private companion object {
        const val TAG = "Assembly"

        /**
         * Documented shards only (spec §3). `votetrack` and `delvotes` are deliberately absent
         * — they return per-hour and per-delegate breakdowns this screen does not draw, and
         * asking for them would spend the request budget on data nobody sees.
         */
        val SHARDS = listOf("resolution", "numnations", "numdelegates")
    }
}
