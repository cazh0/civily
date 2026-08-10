package dev.cazh0.stately.core.session

/**
 * The signed-in nation and the credentials that prove it.
 *
 * Never logged, never placed in an Intent, never sent anywhere but nationstates.net (spec §3).
 */
data class Session(
    val nationId: String,
    val nationName: String,
    val autologin: String?,
    val pin: String?,
    val regionId: String?,
    val isWaMember: Boolean,
) {
    /** NationStates returns this PIN when the session has expired. */
    val validPin: String? get() = pin?.takeIf { it != INVALID_PIN }

    companion object {
        const val INVALID_PIN = "-1"
    }
}
