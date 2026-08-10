package dev.cazh0.civily.core.session

/**
 * One signed-in nation and the credentials that prove it.
 *
 * Never logged, never placed in an Intent, never sent anywhere but nationstates.net (spec §3).
 *
 * NationStates issues a session per nation, so every stored account carries its own token and
 * its own PIN. Signing in as a second nation does not disturb the first one's session — which
 * is what makes switching a local change rather than a login.
 */
data class Session(
    val nationId: String,
    val nationName: String,
    /**
     * Why not nullable: NationStates returns `X-Autologin` only when it accepted the password,
     * so a [Session] without one was never signed in. Making it non-null means no code
     * downstream has to ask.
     */
    val autologin: String,
    val pin: String?,
    val regionId: String?,
    val isWaMember: Boolean,
    /** Empty when the sign-in response carried no flag, which the account tile falls back on. */
    val flagUrl: String,
) {
    /** NationStates returns this PIN when the session has expired. */
    val validPin: String? get() = pin?.takeIf { it != INVALID_PIN }

    companion object {
        const val INVALID_PIN = "-1"
    }
}
