package dev.cazh0.stately.core.text

/**
 * How long ago something happened, in the compact form a message list wants.
 *
 * Why compact rather than "3 hours ago": every post in a board carries one of these, and the
 * long form pushes the author's name off the line on a narrow screen. "3h" is the convention
 * readers already know from every other feed.
 *
 * Why no timezone or locale: everything here is a difference between two instants, so there
 * is nothing to convert and nothing that changes with where the device is. That also makes it
 * testable without a framework.
 */
object RelativeTime {

    fun compact(epochSeconds: Long, nowMs: Long): String {
        val elapsed = nowMs / MILLIS_PER_SECOND - epochSeconds

        // A clock that is a little behind the server is normal, and "-2s ago" is not a thing.
        if (elapsed < SECONDS_PER_MINUTE) return NOW

        val minutes = elapsed / SECONDS_PER_MINUTE
        if (minutes < MINUTES_PER_HOUR) return "${minutes}m"

        val hours = minutes / MINUTES_PER_HOUR
        if (hours < HOURS_PER_DAY) return "${hours}h"

        val days = hours / HOURS_PER_DAY
        if (days < DAYS_PER_WEEK) return "${days}d"

        val weeks = days / DAYS_PER_WEEK
        if (weeks < WEEKS_PER_YEAR) return "${weeks}w"

        return "${days / DAYS_PER_YEAR}y"
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
