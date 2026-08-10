package dev.cazh0.stately.core.text.bbcode

import java.util.Locale

/**
 * Resolves a `[color=…]` value written by a player into an ARGB integer.
 *
 * Why hand-rolled rather than `android.graphics.Color.parseColor`: that method throws on
 * anything it does not recognise, and this input is arbitrary text typed by strangers. An
 * unrecognised colour must quietly mean "no colour", not an exception on a parse path or a
 * `try`/`catch` standing in for a check (spec §2 R1).
 *
 * Returning null rather than a default is deliberate: the caller then leaves the text in the
 * theme's own colour, which stays legible in both light and dark. A guessed colour does not.
 */
object BbColor {

    /** @return ARGB, or null when the value is not a colour this understands. */
    fun parse(value: String): Int? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith('#')) parseHex(trimmed.substring(1)) else NAMED[
            trimmed.lowercase(Locale.US),
        ]
    }

    private fun parseHex(digits: String): Int? {
        val expanded = when (digits.length) {
            // #RGB is shorthand: each digit is doubled.
            SHORT_HEX -> digits.map { "$it$it" }.joinToString("")
            FULL_HEX -> digits
            else -> return null
        }
        var value = 0
        for (c in expanded) {
            val digit = Character.digit(c, HEX_RADIX)
            if (digit < 0) return null
            value = (value shl 4) or digit
        }
        return OPAQUE or value
    }

    private const val SHORT_HEX = 3
    private const val FULL_HEX = 6
    private const val HEX_RADIX = 16
    private const val OPAQUE = 0xFF000000.toInt()

    /**
     * The colour names players actually reach for. Not the full CSS list — an unknown name
     * degrades to unstyled text, which is a better outcome than carrying 140 constants.
     */
    private val NAMED = mapOf(
        "black" to 0xFF000000.toInt(),
        "white" to 0xFFFFFFFF.toInt(),
        "red" to 0xFFFF0000.toInt(),
        "green" to 0xFF008000.toInt(),
        "lime" to 0xFF00FF00.toInt(),
        "blue" to 0xFF0000FF.toInt(),
        "navy" to 0xFF000080.toInt(),
        "yellow" to 0xFFFFFF00.toInt(),
        "orange" to 0xFFFFA500.toInt(),
        "purple" to 0xFF800080.toInt(),
        "violet" to 0xFFEE82EE.toInt(),
        "pink" to 0xFFFFC0CB.toInt(),
        "brown" to 0xFFA52A2A.toInt(),
        "grey" to 0xFF808080.toInt(),
        "gray" to 0xFF808080.toInt(),
        "silver" to 0xFFC0C0C0.toInt(),
        "gold" to 0xFFFFD700.toInt(),
        "cyan" to 0xFF00FFFF.toInt(),
        "aqua" to 0xFF00FFFF.toInt(),
        "teal" to 0xFF008080.toInt(),
        "magenta" to 0xFFFF00FF.toInt(),
        "maroon" to 0xFF800000.toInt(),
        "olive" to 0xFF808000.toInt(),
    )
}
