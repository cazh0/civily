package dev.cazh0.civily.data.wa

import dev.cazh0.civily.core.net.NsClient
import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.core.result.Outcome
import dev.cazh0.civily.core.result.flatMap
import dev.cazh0.civily.core.result.map
import dev.cazh0.civily.core.text.bbcode.BbParser
import dev.cazh0.civily.data.decodeNsXml
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

    private companion object {
        const val TAG = "Assembly"

        /**
         * Documented shards only (spec §3). `votetrack` and `delvotes` feed the visible hourly
         * trend and the split between ordinary and delegate-weighted votes.
         */
        val SHARDS = listOf("resolution", "votetrack", "delvotes", "numnations", "numdelegates")
    }
}

internal fun AssemblyDto.toAssembly() = Assembly(
    memberCount = memberCount,
    delegateCount = delegateCount,
    resolution = resolution.takeIf { it.isAtVote }?.let { dto ->
        Resolution(
            name = dto.name,
            category = dto.category,
            proposedBy = dto.proposedBy,
            votesFor = dto.votesFor,
            votesAgainst = dto.votesAgainst,
            voteHistory = dto.voteHistory(),
            voteBreakdown = dto.voteBreakdown(),
            body = BbParser.parse(dto.body),
        )
    },
)

    // Why append the totals: `votetrack` is sampled on the hour, while `resolution` is live.
    private fun ResolutionDto.voteHistory() =
        voteTrackFor.points.zip(voteTrackAgainst.points) { votesFor, votesAgainst ->
            VoteTally(votesFor.votes, votesAgainst.votes)
        }.let { history ->
            val current = VoteTally(votesFor, votesAgainst)
            if (history.lastOrNull() == current) history else history + current
        }

    private fun ResolutionDto.voteBreakdown(): VoteBreakdown {
        val delegatesFor = delegateVotesFor.delegates.sumOf(DelegateVoteDto::votes)
        val delegatesAgainst = delegateVotesAgainst.delegates.sumOf(DelegateVoteDto::votes)
        // Why subtract: `TOTAL_VOTES_*` already includes the weighted ballots in `delvotes`.
        return VoteBreakdown(
            nationsFor = votesFor - delegatesFor,
            nationsAgainst = votesAgainst - delegatesAgainst,
            delegatesFor = delegatesFor,
            delegatesAgainst = delegatesAgainst,
        )
    }
