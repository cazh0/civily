package dev.cazh0.stately.core.net

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * The single place any NationStates URL is built.
 *
 * Why centralised: spec §3 permits no endpoint other than nationstates.net. One builder
 * makes that a property of the codebase you can verify by reading one file, instead of a
 * rule you have to trust 40 scattered format strings to keep.
 */
object NsUrl {

    const val DOMAIN = "nationstates.net"

    private const val BASE = "https://www.$DOMAIN"
    private const val API_PATH = "$BASE/cgi-bin/api.cgi"

    /** Pinned so a server-side default change can never silently alter response shapes. */
    private const val API_VERSION = "12"

    /** What the request is about. Mirrors the API's mutually exclusive target parameters. */
    sealed interface Target {
        fun applyTo(builder: HttpUrl.Builder)

        data class Nation(val id: String) : Target {
            override fun applyTo(builder: HttpUrl.Builder) {
                builder.addQueryParameter("nation", id)
            }
        }

        data class Region(val id: String) : Target {
            override fun applyTo(builder: HttpUrl.Builder) {
                builder.addQueryParameter("region", id)
            }
        }

        /** [council] is 1 for the General Assembly, 2 for the Security Council. */
        data class Assembly(val council: Int) : Target {
            override fun applyTo(builder: HttpUrl.Builder) {
                builder.addQueryParameter("wa", council.toString())
            }
        }

        data object World : Target {
            override fun applyTo(builder: HttpUrl.Builder) = Unit
        }
    }

    /** Artwork served alongside issues and policies, addressed by the id the API returns. */
    fun banner(bannerId: String): String = "$BASE/images/banners/$bannerId.jpg"

    /**
     * The bare endpoint for a Private Command.
     *
     * Commands carry everything — the nation, the command, its arguments — in the POST body
     * rather than the query string, so there is nothing to build here beyond the address.
     */
    fun command(): HttpUrl = API_PATH.toHttpUrl()

    /**
     * @param shards documented shard names only (spec §3). Joined with `+` as the API expects.
     * @param options extra query parameters such as `scale` or `mode`.
     */
    fun api(
        target: Target,
        shards: List<String>,
        options: Map<String, String> = emptyMap(),
    ): HttpUrl {
        val builder = API_PATH.toHttpUrl().newBuilder()
        target.applyTo(builder)
        if (shards.isNotEmpty()) {
            // Why encoded: shard names are a fixed lowercase-ASCII vocabulary we control, and
            // the `+` between them is a separator the API requires literally, not an escape.
            builder.addEncodedQueryParameter("q", shards.joinToString("+"))
        }
        options.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        builder.addQueryParameter("v", API_VERSION)
        return builder.build()
    }
}
