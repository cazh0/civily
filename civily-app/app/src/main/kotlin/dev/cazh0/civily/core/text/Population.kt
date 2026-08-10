package dev.cazh0.civily.core.text

import java.util.Locale

/**
 * Turns the API's population figure into something a person reads without counting digits.
 *
 * NationStates reports population as a count of millions, so a large nation arrives as
 * `49902` and renders as "49902 million" if nothing intervenes — technically true, unreadable,
 * and not what the game itself shows. Past a billion the game switches unit, so this does too.
 *
 * Formatting is fixed to [Locale.US] because the app ships English-only by project rule
 * (README), and a number grouped one way beside a word chosen another way reads as a bug.
 */
object Population {

    /**
     * @param value the formatted number alone, with no unit — the caller pairs it with the
     *   right string resource so the unit stays in the resource layer.
     */
    data class Scaled(val value: String, val inBillions: Boolean)

    fun scale(millions: Int): Scaled = if (millions < MILLIONS_PER_BILLION) {
        Scaled(Numbers.grouped(millions), inBillions = false)
    } else {
        val billions = millions.toDouble() / MILLIONS_PER_BILLION
        Scaled(trimTrailingZeros(String.format(Locale.US, "%.3f", billions)), inBillions = true)
    }

    /** 50.000 reads as false precision; 49.902 does not. Strip only what adds nothing. */
    private fun trimTrailingZeros(formatted: String): String =
        if ('.' in formatted) formatted.trimEnd('0').trimEnd('.') else formatted

    private const val MILLIONS_PER_BILLION = 1000
}
