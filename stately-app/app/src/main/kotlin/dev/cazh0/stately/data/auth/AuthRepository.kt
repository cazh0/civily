package dev.cazh0.stately.data.auth

import dev.cazh0.stately.core.net.NsClient
import dev.cazh0.stately.core.net.NsUrl
import dev.cazh0.stately.core.result.Outcome
import dev.cazh0.stately.core.result.flatMap
import dev.cazh0.stately.core.result.map
import dev.cazh0.stately.core.session.Session
import dev.cazh0.stately.core.session.SessionStore
import dev.cazh0.stately.core.text.NsId
import dev.cazh0.stately.data.decodeNsXml
import dev.cazh0.stately.data.nation.NationDto

/**
 * Signs a nation in and out.
 *
 * The password never leaves this call chain: it goes to [NsClient.signIn], into the
 * `X-Password` header, and is discarded. What is persisted is the autologin token
 * NationStates issues in exchange (spec §3).
 */
class AuthRepository(
    private val client: NsClient,
    private val session: SessionStore,
) {

    suspend fun signIn(nationName: String, password: String): Outcome<Session> {
        val nationId = NsId.fromName(nationName)
        val url = NsUrl.api(NsUrl.Target.Nation(nationId), SHARDS)

        val outcome = client.signIn(url, password).flatMap { result ->
            decodeNsXml<NationDto>(TAG, result.body).map { nation ->
                Session(
                    nationId = nationId,
                    nationName = nation.name.ifEmpty { NsId.toName(nationId) },
                    autologin = result.autologin,
                    pin = result.pin,
                    regionId = nation.region.takeIf { it.isNotEmpty() }?.let(NsId::fromName),
                    isWaMember = nation.isWaMember,
                )
            }
        }

        // Why the store is written here and not inside the map: a mapping function that also
        // mutates global state is a mapping function nobody can reuse or test.
        if (outcome is Outcome.Success) session.signIn(outcome.value)
        return outcome
    }

    fun signOut() = session.signOut()

    private companion object {
        const val TAG = "SignIn"

        /**
         * `ping` is the point of this list. A request for public shards alone succeeds
         * whatever password is sent, so a wrong one would look like a right one; a private
         * shard forces NationStates to actually authenticate. The docs name `ping` as the
         * shard "for when you don't want to do anything except register a login", which is
         * exactly what signing in is. The other three fill in the [Session].
         */
        val SHARDS = listOf("name", "region", "wa", "ping")
    }
}
