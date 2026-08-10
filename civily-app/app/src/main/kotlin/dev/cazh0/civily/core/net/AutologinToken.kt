package dev.cazh0.civily.core.net

/**
 * Converts a stored autologin token into the two shapes NationStates expects.
 *
 * Why both shapes exist: the `autologin` cookie carries `nation=token`, while the
 * `X-Autologin` header carries the bare token. NationStates has issued tokens in two
 * formats over time — modern ones start with `.` and omit the nation prefix, older ones
 * embed it as `nation%3Dtoken`. A device that logged in years ago still holds an old one,
 * so both must round-trip correctly.
 */
internal object AutologinToken {

    private const val NEW_FORMAT_PREFIX = "."
    private const val ENCODED_EQUALS = "%3D"

    /** The value for `Cookie: autologin=…`. */
    fun forCookie(nationId: String, stored: String): String {
        if (stored.isEmpty()) return stored
        val prefixed =
            if (stored.startsWith(NEW_FORMAT_PREFIX)) "$nationId$ENCODED_EQUALS$stored" else stored
        return PercentCodec.decode(prefixed)
    }

    /** The value for the `X-Autologin` header. */
    fun forHeader(nationId: String, stored: String): String =
        PercentCodec.decode(stored.replace("$nationId$ENCODED_EQUALS", ""))
}
