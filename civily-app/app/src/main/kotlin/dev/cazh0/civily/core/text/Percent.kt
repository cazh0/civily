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

    private const val SCALE = 10.0
}
