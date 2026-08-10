package dev.cazh0.stately.data.rmb

import dev.cazh0.stately.core.net.NsClient
import dev.cazh0.stately.core.net.NsUrl
import dev.cazh0.stately.core.result.Outcome
import dev.cazh0.stately.core.result.flatMap
import dev.cazh0.stately.core.result.map
import dev.cazh0.stately.core.text.bbcode.BbParser
import dev.cazh0.stately.data.decodeNsXml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a regional message board. */
class RmbRepository(private val client: NsClient) {

    /** Runs on [Dispatchers.Default]: every post body is BBCode that has to be parsed. */
    suspend fun load(regionId: String): Outcome<List<RmbPost>> =
        withContext(Dispatchers.Default) {
            val url = NsUrl.api(
                target = NsUrl.Target.Region(regionId),
                shards = listOf(SHARD),
                options = mapOf(LIMIT to PAGE_SIZE.toString()),
            )
            client.get(url)
                .flatMap { body -> decodeNsXml<RmbPageDto>(TAG, body) }
                .map { page -> page.messages.posts.map { it.toPost() }.asReversed() }
        }

    private fun PostDto.toPost() = RmbPost(
        id = id,
        author = nation,
        postedAtEpochSeconds = timestamp,
        wasEdited = edited > 0,
        likes = likes,
        fromEmbassyRegion = embassy.takeIf { it.isNotEmpty() },
        body = BbParser.parse(message),
        visibility = when (status) {
            SUPPRESSED -> RmbPost.Visibility.Suppressed
            DELETED -> RmbPost.Visibility.Deleted
            else -> RmbPost.Visibility.Visible
        },
    )

    private companion object {
        const val TAG = "RMB"
        const val SHARD = "messages"
        const val LIMIT = "limit"

        /**
         * The API allows 1–100 and defaults to 10. Fifty is one screenful of scrolling without
         * making the first load noticeably slower, and paging is not built yet.
         */
        const val PAGE_SIZE = 50

        const val SUPPRESSED = 1
        const val DELETED = 2
    }
}
