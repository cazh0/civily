package dev.cazh0.civily.core.text

import java.util.Locale

/**
 * Turns a figure too long to read into one a person takes in at a glance.
 *
 * A nation's gross domestic product arrives as `3315350844221109`. Grouped, that is still
 * sixteen digits nobody counts; scaled, it is "3,315 trillion", which is how the game's own
 * prose writes it. [Population] does the same job for the one figure the API reports in
 * millions already — this is for the ones it reports in full.
 *
 * Why the unit comes back as an enum rather than a word: the words live in the resource layer,
 * and this object may not hold a `Context`. Why there is no unit below a million: nothing in
 * this app is scaled at thousands — an income of `66427` reads perfectly as "66,427", and a
 * "66.4 thousand" would be harder to read than the number it replaced.
 *
 * Formatting is fixed to [Locale.US] for the same reason as [Numbers].
 */
object Magnitude {

    enum class Unit { Ones, Million, Billion, Trillion }

    data class Scaled(val value: String, val unit: Unit)

    fun scale(amount: Long): Scaled {
        val unit = when {
            amount >= TRILLION -> Unit.Trillion
            amount >= BILLION -> Unit.Billion
            amount >= MILLION -> Unit.Million
            else -> Unit.Ones
        }
        if (unit == Unit.Ones) return Scaled(Numbers.grouped(amount), Unit.Ones)

        val divisor = when (unit) {
            Unit.Trillion -> TRILLION
            Unit.Billion -> BILLION
            Unit.Million -> MILLION
            Unit.Ones -> 1L
        }
        val scaled = amount.toDouble() / divisor
        // Why one decimal only under ten: "1 million" throws away a third of 1.5 million, while
        // "3,315.4 trillion" spends a digit on precision at a size where nobody is counting.
        val text = if (scaled < DECIMAL_BELOW) {
            String.format(Locale.US, "%.1f", scaled).removeSuffix(".0")
        } else {
            Numbers.grouped(scaled.toLong())
        }
        return Scaled(text, unit)
    }

    private const val MILLION = 1_000_000L
    private const val BILLION = 1_000_000_000L
    private const val TRILLION = 1_000_000_000_000L
    private const val DECIMAL_BELOW = 10.0
}
