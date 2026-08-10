package dev.cazh0.stately.core.net

import java.util.Locale

/**
 * Builds the User-Agent NationStates requires for API access.
 *
 * Why the exact wording is preserved from the legacy app: NationStates moderators identify
 * and whitelist clients by this string. Changing the format risks the app being treated as
 * an unidentified script.
 */
object UserAgent {

    private const val WITH_USER = "Stately/%s (User %s; Droid)"
    private const val NO_USER = "Stately/%s (No User; Droid)"

    fun of(appVersion: String, nationId: String?): String =
        if (nationId.isNullOrEmpty()) {
            String.format(Locale.US, NO_USER, appVersion)
        } else {
            String.format(Locale.US, WITH_USER, appVersion, nationId)
        }
}
