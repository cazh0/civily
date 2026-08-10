package dev.cazh0.civily.core.net

import java.util.Locale

/**
 * Builds the User-Agent NationStates requires for API access.
 *
 * The shape is the legacy app's, because NationStates moderators identify clients by this
 * string and an unfamiliar format risks being read as an unidentified script. The name is not:
 * this is a different client with a different application id, and sending `Stately/` would be
 * claiming to be somebody else's app — which is a worse standing with the moderators than
 * being new.
 */
object UserAgent {

    private const val WITH_USER = "Civily/%s (User %s; Droid)"
    private const val NO_USER = "Civily/%s (No User; Droid)"

    fun of(appVersion: String, nationId: String?): String =
        if (nationId.isNullOrEmpty()) {
            String.format(Locale.US, NO_USER, appVersion)
        } else {
            String.format(Locale.US, WITH_USER, appVersion, nationId)
        }
}
