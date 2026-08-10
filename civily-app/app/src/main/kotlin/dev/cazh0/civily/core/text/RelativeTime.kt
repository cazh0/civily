package dev.cazh0.civily.core.text

/**
 * How long ago something happened.
 *
 * Two renderings, one ladder. [compact] is what a message list wants — every post in a board
 * carries one, and "3 hours ago" pushes the author's name off the line on a narrow screen, so
 * "3h" is the convention readers already know from every other feed. [elapsed] is the same gap
 * left unformatted, for the places that have a whole line to spend on it: a happening reads as
 * prose, and "3h" beside a sentence reads as a stray token.
 *
 * Why [elapsed] returns a count and a unit rather than words: the words are plural-sensitive
 * ("1 hour ago", "3 hours ago") and plurals live in the resource layer. Returning the two
 * numbers a plural string needs keeps this object free of `Context` — which is what keeps it
 * testable without a framework.
 *
 * Why no timezone or locale: everything here is a difference between two instants, so there
 * is nothing to convert and nothing that changes with where the device is.
 */
object RelativeTime {

    /** The coarsest unit that still describes a gap. [Now] carries no count. */
    enum class Grain { Now, Minutes, Hours, Days, Weeks, Years }

    data class Elapsed(val count: Int, val grain: Grain)

    fun compact(epochSeconds: Long, nowMs: Long): String {
        val (count, grain) = elapsed(epochSeconds, nowMs)
        return when (grain) {
            Grain.Now -> NOW
            Grain.Minutes -> "${count}m"
            Grain.Hours -> "${count}h"
            Grain.Days -> "${count}d"
            Grain.Weeks -> "${count}w"
            Grain.Years -> "${count}y"
        }
    }

    fun elapsed(epochSeconds: Long, nowMs: Long): Elapsed {
        val seconds = nowMs / MILLIS_PER_SECOND - epochSeconds

        // A clock that is a little behind the server is normal, and "-2s ago" is not a thing.
        if (seconds < SECONDS_PER_MINUTE) return Elapsed(0, Grain.Now)

        val minutes = seconds / SECONDS_PER_MINUTE
        if (minutes < MINUTES_PER_HOUR) return Elapsed(minutes.toInt(), Grain.Minutes)

        val hours = minutes / MINUTES_PER_HOUR
        if (hours < HOURS_PER_DAY) return Elapsed(hours.toInt(), Grain.Hours)

        val days = hours / HOURS_PER_DAY
        if (days < DAYS_PER_WEEK) return Elapsed(days.toInt(), Grain.Days)

        val weeks = days / DAYS_PER_WEEK
        if (weeks < WEEKS_PER_YEAR) return Elapsed(weeks.toInt(), Grain.Weeks)

        return Elapsed((days / DAYS_PER_YEAR).toInt(), Grain.Years)
    }

    private const val NOW = "now"
    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val HOURS_PER_DAY = 24L
    private const val DAYS_PER_WEEK = 7L
    private const val WEEKS_PER_YEAR = 52L
    private const val DAYS_PER_YEAR = 365L
}
