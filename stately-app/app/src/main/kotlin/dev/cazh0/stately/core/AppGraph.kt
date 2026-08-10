package dev.cazh0.stately.core

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import dev.cazh0.stately.BuildConfig
import dev.cazh0.stately.core.net.NsClient
import dev.cazh0.stately.core.net.RateLimiter
import dev.cazh0.stately.core.session.SessionStore
import dev.cazh0.stately.data.auth.AuthRepository
import dev.cazh0.stately.data.nation.NationRepository
import dev.cazh0.stately.data.region.RegionRepository
import dev.cazh0.stately.data.issues.IssuesRepository
import dev.cazh0.stately.data.rmb.RmbRepository
import dev.cazh0.stately.data.wa.AssemblyRepository
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

    /** Why a shared loader: NationStates serves many flags as SVG, which Coil needs told about. */
    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(appContext)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 30L
        const val CALL_TIMEOUT_SECONDS = 60L
    }
}

/** The only way UI code reaches the graph. */
val Context.graph: AppGraph
    get() = (applicationContext as StatelyApp).graph
