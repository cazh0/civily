package dev.cazh0.civily.data.issues

import android.util.Log
import dev.cazh0.civily.core.net.NsClient
import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.core.result.Outcome
import dev.cazh0.civily.core.result.CivilyError
import dev.cazh0.civily.core.result.flatMap
import dev.cazh0.civily.core.result.map
import dev.cazh0.civily.core.session.SessionStore
import dev.cazh0.civily.core.text.HtmlEntities
import dev.cazh0.civily.core.text.bbcode.BbParser
import dev.cazh0.civily.data.decodeNsXml
import dev.cazh0.civily.data.nation.toPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * The signed-in nation's issues, and the command that answers one.
 *
 * `issues` is a private shard and `c=issue` is a Private Command, so both require a session.
 * Without one this returns [CivilyError.Unauthorized] rather than sending an anonymous
 * request that NationStates would reject anyway.
 */
class IssuesRepository(
    private val client: NsClient,
    private val session: SessionStore,
) {

    suspend fun load(): Outcome<IssuesPage> = withContext(Dispatchers.Default) {
        val active = session.current
            ?: return@withContext Outcome.Failure(CivilyError.Unauthorized)

        client.get(NsUrl.api(NsUrl.Target.Nation(active.nationId), SHARDS))
            .flatMap { body -> decodeNsXml<IssuesPageDto>(TAG, body) }
            .map { page ->
                IssuesPage(
                    capital = page.capital,
                    nationName = active.nationName,
                    flagUrl = page.flagUrl,
                    currency = page.currency,
                    demonym = HtmlEntities.decode(page.demonym),
                    issues = page.issues.issues.map { it.toIssue() },
                    nextIssueTime = page.nextIssueTime.orNoNextIssue(),
                )
            }
    }

    /**
     * The two numbers the Issues button shows, without the payload behind it.
     *
     * Separate from [load] rather than derived from it because the button is on the front
     * screen and the issues themselves are not: asking for `issues` there would fetch every
     * issue's prose and options to render a digit.
     */
    suspend fun badge(): Outcome<IssueBadge> = withContext(Dispatchers.Default) {
        val active = session.current
            ?: return@withContext Outcome.Failure(CivilyError.Unauthorized)

        client.get(NsUrl.api(NsUrl.Target.Nation(active.nationId), BADGE_SHARDS))
            .flatMap { body -> decodeNsXml<IssueBadgeDto>(BADGE_TAG, body) }
            .map { dto ->
                IssueBadge(
                    nationId = active.nationId,
                    dueCount = dto.unread.issues,
                    nextIssueTime = dto.nextIssueTime.orNoNextIssue(),
                )
            }
    }

    /** `NEXTISSUETIME` is absent for a nation with none scheduled, which decodes to zero. */
    private fun Long.orNoNextIssue(): Long? = takeIf { it > 0 }

    /**
     * Enacts [optionId] on [issueId], or dismisses the issue when [optionId] is [DISMISS].
     *
     * Enacting uses the site's own tokenised form instead of the API command because the POST
     * response is the only response that carries the exact `legislation-papers` newspaper stack.
     * If that page fails before submission, the documented API command is the stable fallback.
     * Dismissal stays on the API command because it has no aftermath page to render.
     *
     * **This cannot be undone.** The caller is responsible for confirming with the user first.
     */
    suspend fun answer(issueId: Int, optionId: Int): Outcome<IssueResult> =
        withContext(Dispatchers.Default) {
            val nationId = session.current?.nationId
                ?: return@withContext Outcome.Failure(CivilyError.Unauthorized)

            if (optionId != DISMISS) {
                return@withContext answerViaSiteOrApi(nationId, issueId, optionId)
            }

            answerViaApi(nationId, issueId, optionId)
        }

    private suspend fun answerViaSiteOrApi(
        nationId: String,
        issueId: Int,
        optionId: Int,
    ): Outcome<IssueResult> {
        val dilemmaUrl = NsUrl.dilemma(issueId)
        val form = when (val page = client.getSitePage(dilemmaUrl)) {
            is Outcome.Success -> IssueHtmlParser.parseEnactForm(
                html = page.value,
                nationId = nationId,
                issueId = issueId,
                optionId = optionId,
            ).loggedIssueHtmlFailure("issue enact form")

            is Outcome.Failure -> {
                Log.w(TAG, "issue page fetch failed before submission: ${page.error.detailName()}")
                return answerViaApi(nationId, issueId, optionId)
            }
        }

        val enactForm = when (form) {
            is Outcome.Success -> form.value
            is Outcome.Failure -> {
                Log.w(TAG, "falling back to API issue command: ${form.error.detailName()}")
                return answerViaApi(nationId, issueId, optionId)
            }
        }

        val actionUrl = NsUrl.siteAction(enactForm.action)
            ?: run {
                Log.w(TAG, "falling back to API issue command: issue enact form action invalid")
                return answerViaApi(nationId, issueId, optionId)
            }

        return when (val posted = client.postSiteForm(
            url = actionUrl,
            form = enactForm.fields,
            referer = dilemmaUrl,
        )) {
            is Outcome.Success -> IssueHtmlParser.parseResult(posted.value)
                .loggedIssueHtmlFailure("issue result")
                .submittedResult()

            is Outcome.Failure -> submittedUnknown(posted.error)
        }
    }

    private suspend fun answerViaApi(
        nationId: String,
        issueId: Int,
        optionId: Int,
    ): Outcome<IssueResult> =
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

    private fun IssueResultDto.toResult() = IssueResult(
        description = description,
        rankings = rankings.ranks.map {
            CensusChange(
                scaleId = it.id,
                percentChange = it.percentChange,
            )
        },
        headlines = headlines.headlines.mapNotNull { it.toHeadline() },
        reclassifications = reclassifications.reclassifications.mapNotNull { it.toReclassification() },
        newPolicies = newPolicies.policies.map { it.toPolicy() },
        canceledPolicies = removedPolicies.policies.map { it.toPolicy() },
        postcards = unlocks.banners.map { it.bannerId }.filter { it.isNotEmpty() },
    )

    /**
     * Null for a `type` this app has no name for, which drops the row.
     *
     * Dropping rather than defaulting: the legacy client falls back to civil rights for an
     * unrecognised code, so a fifth code NationStates adds would be printed as a civil-rights
     * change that never happened. A reclassification the reader does not see is a gap; one
     * labelled with the wrong rating is a lie about their nation.
     */
    private fun ReclassifyDto.toReclassification(): Reclassification? {
        val rating = when (type.trim().lowercase(Locale.US)) {
            "0" -> Reclassification.Rating.CivilRights
            "1" -> Reclassification.Rating.Economy
            "2" -> Reclassification.Rating.PoliticalFreedom
            "govt" -> Reclassification.Rating.Government
            else -> {
                Log.w(TAG, "unknown reclassification type '$type'")
                return null
            }
        }
        val from = HtmlEntities.decode(from).trim()
        val to = HtmlEntities.decode(to).trim()
        if (from.isEmpty() || to.isEmpty()) return null
        return Reclassification(rating = rating, from = from, to = to)
    }

    /** No artwork: `c=issue` gives the headline as bare text. See [HeadlineDto]. */
    private fun HeadlineDto.toHeadline(): IssueResultHeadline? =
        displayText.takeIf { it.isNotBlank() }
            ?.let { IssueResultHeadline(text = it, imageUrls = emptyList()) }

    private fun IssueDto.toIssue() = Issue(
        id = id,
        title = title,
        text = BbParser.parse(text),
        imageUrls = listOfNotNull(
            primaryImageId.takeIf { it.isNotEmpty() }?.let { NsUrl.newspaperImage(it, 1) },
            secondaryImageId.takeIf { it.isNotEmpty() }?.let { NsUrl.newspaperImage(it, 2) },
        ),
        options = options.map { IssueOption(id = it.id, text = BbParser.parse(it.text)) },
    )

    private fun Outcome<IssueResult>.submittedResult(): Outcome<IssueResult> = when (this) {
        is Outcome.Success -> this
        is Outcome.Failure -> submittedUnknown(error)
    }

    private fun <T> Outcome<T>.loggedIssueHtmlFailure(section: String): Outcome<T> {
        if (this is Outcome.Failure) {
            Log.w(TAG, "$section did not parse: ${error.detailName()}")
        }
        return this
    }

    private fun submittedUnknown(error: CivilyError): Outcome.Failure {
        Log.w(TAG, "Submitted issue result unknown: ${error.detailName()}")
        return Outcome.Failure(CivilyError.IssueResultUnknown(error.detailName()))
    }

    private fun CivilyError.detailName(): String = when (this) {
        is CivilyError.Malformed -> detail
        is CivilyError.Server -> "server $code"
        is CivilyError.IssueResultUnknown -> detail
        CivilyError.InvalidLogin -> "invalid login"
        CivilyError.LoginConflict -> "login conflict"
        CivilyError.NoConnection -> "no connection"
        CivilyError.NotFound -> "not found"
        CivilyError.RateLimited -> "rate limited"
        CivilyError.Unauthorized -> "unauthorized"
    }

    companion object {
        /** The API's sentinel for "dismiss without acting". */
        const val DISMISS = -1

        private const val TAG = "Issues"
        private const val RESULT_TAG = "IssueResult"
        private const val BADGE_TAG = "IssueBadge"
        /**
         * The five extras ride along free — one request either way. Three of them dress the
         * masthead: the capital names the paper, the flag flies on it and the currency is the
         * cover price. `nextissuetime` is what the screen counts down to when there is nothing
         * on the desk, and `demonym` is the adjective the aftermath's reclassification sentence
         * is written in.
         */
        private val SHARDS =
            listOf("issues", "capital", "flag", "currency", "demonym", "nextissuetime")

        /** A count and an instant. See [badge]. */
        private val BADGE_SHARDS = listOf("unread", "nextissuetime")
    }
}
