package dev.cazh0.stately.data.issues

import dev.cazh0.stately.core.net.NsClient
import dev.cazh0.stately.core.net.NsUrl
import dev.cazh0.stately.core.result.Outcome
import dev.cazh0.stately.core.result.StatelyError
import dev.cazh0.stately.core.result.flatMap
import dev.cazh0.stately.core.result.map
import dev.cazh0.stately.core.session.SessionStore
import dev.cazh0.stately.core.text.bbcode.BbParser
import dev.cazh0.stately.data.decodeNsXml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The signed-in nation's issues, and the command that answers one.
 *
 * `issues` is a private shard and `c=issue` is a Private Command, so both require a session.
 * Without one this returns [StatelyError.Unauthorized] rather than sending an anonymous
 * request that NationStates would reject anyway.
 */
class IssuesRepository(
    private val client: NsClient,
    private val session: SessionStore,
) {

    suspend fun load(): Outcome<IssuesPage> = withContext(Dispatchers.Default) {
        val active = session.current
            ?: return@withContext Outcome.Failure(StatelyError.Unauthorized)

        client.get(NsUrl.api(NsUrl.Target.Nation(active.nationId), SHARDS))
            .flatMap { body -> decodeNsXml<IssuesPageDto>(TAG, body) }
            .map { page ->
                IssuesPage(
                    capital = page.capital,
                    nationName = active.nationName,
                    flagUrl = page.flagUrl,
                    currency = page.currency,
                    issues = page.issues.issues.map { it.toIssue() },
                )
            }
    }

    /**
     * Enacts [optionId] on [issueId], or dismisses the issue when [optionId] is [DISMISS].
     *
     * Why this is one request and not the two-step prepare/execute every other Private Command
     * needs: the API documents `issue` as the sole exception. Sending a `mode=prepare` here
     * would be wrong, not merely redundant.
     *
     * **This cannot be undone.** The caller is responsible for confirming with the user first.
     */
    suspend fun answer(issueId: Int, optionId: Int): Outcome<IssueResult> =
        withContext(Dispatchers.Default) {
            val nationId = session.current?.nationId
                ?: return@withContext Outcome.Failure(StatelyError.Unauthorized)

            client.post(
                url = NsUrl.command(),
                form = mapOf(
                    "nation" to nationId,
                    "c" to "issue",
                    "issue" to issueId.toString(),
                    "option" to optionId.toString(),
                ),
            )
                .flatMap { body -> decodeNsXml<IssueResultPageDto>(RESULT_TAG, body) }
                .map { page -> page.issue.toResult() }
        }

    private fun IssueResultDto.toResult() = IssueResult(
        description = description,
        rankings = rankings.ranks.map {
            CensusChange(
                scaleId = it.id,
                score = it.score,
                change = it.change,
                percentChange = it.percentChange,
            )
        },
        headlines = headlines.headlines.map { it.text }.filter { it.isNotBlank() },
    )

    private fun IssueDto.toIssue() = Issue(
        id = id,
        title = title,
        text = BbParser.parse(text),
        bannerUrl = bannerId.takeIf { it.isNotEmpty() }?.let(NsUrl::banner),
        options = options.map { IssueOption(id = it.id, text = BbParser.parse(it.text)) },
    )

    companion object {
        /** The API's sentinel for "dismiss without acting". */
        const val DISMISS = -1

        private const val TAG = "Issues"
        private const val RESULT_TAG = "IssueResult"
        /**
         * The three extras ride along free — one request either way — and between them they
         * dress the masthead: the capital names the paper, the flag flies on it and the
         * currency is the cover price.
         */
        private val SHARDS = listOf("issues", "capital", "flag", "currency")
    }
}
