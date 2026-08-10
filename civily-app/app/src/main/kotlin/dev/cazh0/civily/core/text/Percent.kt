package dev.cazh0.civily.core.text

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A signed percentage change, rounded before its sign is chosen.
 *
 * Why that order matters: `-0.04%` formatted naively prints "-0.0%", which claims a direction
 * for a movement that rounds to nothing. Deciding the sign from the value the reader will
 * actually see keeps the two consistent.
 */
object Percent {

    enum class Direction { Up, Down, Flat }

    data class Change(val text: String, val direction: Direction)

    fun change(percent: Double): Change {
        val rounded = (percent * SCALE).roundToLong() / SCALE
        val direction = when {
            rounded > 0 -> Direction.Up
            rounded < 0 -> Direction.Down
            else -> Direction.Flat
        }
        val sign = when (direction) {
            Direction.Up -> "+"
            Direction.Down -> "−"
            Direction.Flat -> ""
        }
        return Change(
            text = sign + String.format(Locale.US, "%.1f%%", abs(rounded)),
            direction = direction,
        )
    }

    /**
     * An unsigned percentage, with a decimal only when it carries information.
     *
     * The API reports shares to two places — a budget line of `20.9`, a black market of `0.42`,
     * an income tax of `87.0`. "87.0%" spends a character claiming precision the trailing zero
     * does not add, and "0%" would hide a black market entirely, so the decimals are kept where
     * they say something and dropped where they do not.
     */
    fun rounded(value: Double): String =
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

    private const val SCALE = 10.0
}
