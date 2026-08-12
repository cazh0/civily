package dev.cazh0.civily.core

import android.app.ActivityManager
import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dev.cazh0.civily.BuildConfig
import dev.cazh0.civily.core.net.NsClient
import dev.cazh0.civily.core.net.RateLimiter
import dev.cazh0.civily.core.session.SessionStore
import dev.cazh0.civily.data.auth.AuthRepository
import dev.cazh0.civily.data.nation.NationRepository
import dev.cazh0.civily.data.region.RegionRepository
import dev.cazh0.civily.data.issues.IssuesRepository
import dev.cazh0.civily.data.rmb.RmbRepository
import dev.cazh0.civily.data.wa.AssemblyRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Every long-lived object in the app and the edges between them.
 *
 * Why hand-written instead of a DI framework: the whole graph fits on one screen and reads
 * top to bottom. Hilt would add an annotation processor to every build and move these same
 * five edges into generated code you cannot read — cost against spec §1.4 and §1.5 for no
 * gain at this size. Revisit only when the graph stops fitting here.
 *
 * Everything is lazy so [Application.onCreate] stays empty and cold start stays inside the
 * §4 budget.
 */
class AppGraph(context: Context) {

    private val appContext: Context = context.applicationContext

    val sessionStore: SessionStore by lazy { SessionStore(appContext) }

    private val rateLimiter: RateLimiter by lazy { RateLimiter() }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Why these and not the legacy 120s: the rate limiter now paces requests, so a
            // stalled call is a real failure rather than queue backpressure. Two minutes of
            // spinner is not a state worth waiting for.
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    val nsClient: NsClient by lazy {
        NsClient(httpClient, rateLimiter, sessionStore, BuildConfig.VERSION_NAME)
    }

    val nationRepository: NationRepository by lazy { NationRepository(nsClient) }

    val regionRepository: RegionRepository by lazy { RegionRepository(nsClient) }

    val assemblyRepository: AssemblyRepository by lazy { AssemblyRepository(nsClient) }

    val rmbRepository: RmbRepository by lazy { RmbRepository(nsClient) }

    val issuesRepository: IssuesRepository by lazy { IssuesRepository(nsClient, sessionStore) }

    val authRepository: AuthRepository by lazy { AuthRepository(nsClient, sessionStore) }

    /**
     * Why a shared loader: NationStates serves many flags as SVG, which Coil needs told about.
     *
     * Why it is handed [httpClient] instead of building its own: left alone, Coil opens a second
     * connection pool to the same host, so the first flag on a screen pays for a fresh TCP and
     * TLS handshake that the API call a moment earlier has already done. Sharing the pool means
     * artwork rides the warm connection — which is most of the wait on a tab of twenty policy
     * banners. The client carries no `Cache`, which is what Coil requires of a shared one: its
     * own disk cache is the one that must own the image files.
     *
     * Every cache size here is stated rather than defaulted. Coil's own defaults are written for
     * a photo gallery — a quarter of the app's heap in memory and up to 250MB of disk — and this
     * app shows flags. Sizing them to what it actually holds is what lets it run on a device
     * with half a gigabyte of RAM without the system killing it for holding artwork nobody is
     * looking at.
     */
    val imageLoader: ImageLoader by lazy {
        val lowRam = (appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .isLowRamDevice

        ImageLoader.Builder(appContext)
            .okHttpClient { httpClient }
            .components { add(SvgDecoder.Factory()) }
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(if (lowRam) LOW_RAM_MEMORY_FRACTION else MEMORY_FRACTION)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve(IMAGE_CACHE_DIRECTORY))
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            // Why the headers are ignored: they are the site's answer for a browser tab, and
            // they expire flags in hours. A flag changes when a player redraws one — rarely,
            // and never urgently — so honouring them spends a request and a round trip
            // re-fetching a file the device already has, every time a list scrolls past it.
            .respectCacheHeaders(false)
            // Half the bytes per pixel, and Coil applies it only to JPEGs, which have no alpha
            // to lose. It is on only where the alternative is the system killing the app for
            // holding pictures: on a device that reports itself low on RAM the difference is
            // whether a tab of policy banners survives being scrolled.
            .allowRgb565(lowRam)
            .build()
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 30L
        const val CALL_TIMEOUT_SECONDS = 60L

        /**
         * A screen holds a hero flag and at most a screenful of thumbnails, and a decoded flag
         * is under half a megabyte. A tenth of the heap holds every image the app has shown in
         * a session and still leaves the app's own data the room it needs.
         */
        const val MEMORY_FRACTION = 0.10

        /** Enough for the screen in front of the user and the one behind it. Nothing more. */
        const val LOW_RAM_MEMORY_FRACTION = 0.05

        /**
         * Sixteen megabytes is hundreds of flags. Coil's default is two per cent of the free
         * disk on the device, which on a full phone is nothing and on an empty one is a quarter
         * of a gigabyte of somebody's storage spent on pictures of flags.
         */
        const val DISK_CACHE_BYTES = 16L * 1024 * 1024

        const val IMAGE_CACHE_DIRECTORY = "image_cache"
    }
}

/** The only way UI code reaches the graph. */
val Context.graph: AppGraph
    get() = (applicationContext as CivilyApp).graph
