package dev.cazh0.stately.core.net

/**
 * Percent-decoding that leaves `+` alone.
 *
 * Why this exists rather than `URLDecoder.decode`: that method implements *form* decoding,
 * where `+` means space. Autologin tokens are base64 and legitimately contain `+`, so the
 * legacy code decoded with URLDecoder and then replaced every space back with a `+` — which
 * also corrupts any token containing a genuine encoded space. Decoding only `%XX` is the
 * operation that was actually wanted.
 *
 * Bytes are mapped straight to chars because NationStates tokens are ASCII (base64 plus
 * `.` and `=`); multi-byte UTF-8 sequences cannot occur here.
 */
internal object PercentCodec {

    fun decode(input: String): String {
        if ('%' !in input) return input

        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '%' && i + 2 < input.length) {
                val high = Character.digit(input[i + 1], HEX_RADIX)
                val low = Character.digit(input[i + 2], HEX_RADIX)
                if (high >= 0 && low >= 0) {
                    out.append(((high shl 4) or low).toChar())
                    i += 3
                    continue
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    private const val HEX_RADIX = 16
}
