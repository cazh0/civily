package dev.cazh0.civily.core.net

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

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

    /** Rift banner artwork, addressed by the id the API returns for banners and policies. */
    fun banner(bannerId: String): String = "$BASE/images/banners/$bannerId.jpg"

    /**
     * Issue newspaper artwork from the API, which names it by artwork id alone: `PIC1` of `i16`
     * in slot 1 is `/images/newspaper/i16-1.jpg`.
     *
     * This is for the API only. The site's own pages give the whole path and it is not a
     * pattern — see [siteImage].
     */
    fun newspaperImage(artworkId: String, slot: Int): String {
        val id = artworkId
            .trim()
            .substringAfterLast('/')
            .removeSuffix(".jpg")
            .removeSuffix("-$slot")
        return "$BASE/images/newspaper/$id-$slot.jpg"
    }

    /**
     * An image one of the site's own pages points at, kept exactly as the page wrote it.
     *
     * The aftermath page names every cutout for itself — `i16-1.jpg` on one paper, `y108-1.jpg`
     * on the next, different names on the next issue — so the `src` is the only address that is
     * certain to exist. Rebuilding one from a stem is how a working link becomes a 404 the first
     * time the game picks a name that does not fit the pattern.
     */
    fun siteImage(src: String): String? = onSite(src)

    /** The site's issue page; its enact form carries the CSRF token the site requires. */
    fun dilemma(issueId: Int): HttpUrl =
        "$BASE/page=show_dilemma/dilemma=$issueId".toHttpUrl()

    /** A NationStates-relative form action from one of the site's own pages. */
    fun siteAction(action: String): HttpUrl? = onSite(action)?.toHttpUrlOrNull()

    /**
     * A reference written by one of the site's own pages, resolved against the site.
     *
     * Null when it leads anywhere else. Page HTML is not ours: a reference that names another
     * host, or drops to plain HTTP, is not something the app follows because a page said so.
     */
    private fun onSite(reference: String): String? {
        val normalized = reference.trim()
        val lower = normalized.lowercase(Locale.US)
        return when {
            normalized.isBlank() -> null
            lower.startsWith("http://") -> null
            lower.startsWith("$BASE/") -> normalized
            lower.startsWith("https://$DOMAIN/") ->
                "$BASE/${normalized.substring("https://$DOMAIN/".length)}"
            lower.startsWith("https://") -> null
            // Protocol-relative: the host is whatever follows, which need not be this one.
            normalized.startsWith("//") -> null
            normalized.startsWith("/") -> "$BASE$normalized"
            else -> "$BASE/$normalized"
        }
    }

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
