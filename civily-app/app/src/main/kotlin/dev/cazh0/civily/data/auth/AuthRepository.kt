package dev.cazh0.civily.data.auth

import dev.cazh0.civily.core.net.NsClient
import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.core.result.Outcome
import dev.cazh0.civily.core.result.flatMap
import dev.cazh0.civily.core.result.map
import dev.cazh0.civily.core.session.Session
import dev.cazh0.civily.core.session.SessionStore
import dev.cazh0.civily.core.text.NsId
import dev.cazh0.civily.data.decodeNsXml
import dev.cazh0.civily.data.nation.NationDto

/**
 * Signs a nation in.
 *
 * The password never leaves this call chain: it goes to [NsClient.signIn], into the
 * `X-Password` header, and is discarded. What is persisted is the autologin token
 * NationStates issues in exchange (spec §3).
 *
 * Signing out has no counterpart here because it makes no request: it is
 * [SessionStore.forget], and the UI calls it directly rather than through a method that would
 * only pass it on.
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
                    flagUrl = nation.flagUrl,
                )
            }
        }

        // Why the store is written here and not inside the map: a mapping function that also
        // mutates global state is a mapping function nobody can reuse or test.
        if (outcome is Outcome.Success) session.signIn(outcome.value)
        return outcome
    }

    private companion object {
        const val TAG = "SignIn"

        /**
         * `ping` is the point of this list. A request for public shards alone succeeds
         * whatever password is sent, so a wrong one would look like a right one; a private
         * shard forces NationStates to actually authenticate. The docs name `ping` as the
         * shard "for when you don't want to do anything except register a login", which is
         * exactly what signing in is. The other four fill in the [Session] — `flag` among them
         * because the account switcher shows each nation by its flag, and asking for it here
         * costs nothing on a request the app was already making.
         */
        val SHARDS = listOf("name", "region", "wa", "flag", "ping")
    }
}
