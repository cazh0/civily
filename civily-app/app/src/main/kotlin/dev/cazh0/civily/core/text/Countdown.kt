package dev.cazh0.civily.core.text

/**
 * How long until something happens.
 *
 * The mirror of [RelativeTime], which measures a gap that has already opened. A countdown is
 * the other direction and it is *live*: the number on screen is wrong a second after it is
 * drawn, which is what makes [delayToNextStep] the important function here rather than the
 * arithmetic above it.
 *
 * Why a count of seconds and not words: the same instant is rendered two ways — a compact
 * "1h 14m" on a list row and a "1:14:32" clock on a screen with room for one — and the wording
 * of both belongs in the resource layer. This object holds no `Context` and therefore needs no
 * framework to test.
 *
 * Why no timezone: everything here is a difference between two instants.
 */
object Countdown {

    /** How finely a display of this countdown is written, and therefore how often it changes. */
    enum class Resolution(internal val stepMillis: Long) {
        /** A ticking clock. Changes every second. */
        Seconds(MILLIS_PER_SECOND),

        /** A glanceable "1h 14m". Changes on the minute. */
        Minutes(SECONDS_PER_MINUTE * MILLIS_PER_SECOND),
    }

    /**
     * Whole seconds left, never negative, broken out for display.
     *
     * The components are computed rather than stored so that the whole value is one `Long`:
     * this is written to Compose state on every tick, and equality on one number is what stops
     * an unchanged countdown recomposing anything.
     */
    @JvmInline
    value class Remaining(val totalSeconds: Long) {
        val days: Int get() = (totalSeconds / SECONDS_PER_DAY).toInt()
        val hours: Int get() = ((totalSeconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR).toInt()
        val minutes: Int get() = ((totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).toInt()
        val seconds: Int get() = (totalSeconds % SECONDS_PER_MINUTE).toInt()

        /** The moment has arrived, or the clock is behind the server's. Same thing to a reader. */
        val isDue: Boolean get() = totalSeconds <= 0L
    }

    fun until(targetEpochSeconds: Long, nowMs: Long): Remaining {
        val remainingMs = millisUntil(targetEpochSeconds, nowMs)
        if (remainingMs <= 0L) return Remaining(0L)
        return Remaining(remainingMs / MILLIS_PER_SECOND)
    }

    /**
     * The raw gap in milliseconds, negative once the moment has passed.
     *
     * Exists so that the one place a Unix second is turned into a device millisecond is here,
     * where it is tested, rather than in a composable holding a `* 1000`.
     */
    fun millisUntil(targetEpochSeconds: Long, nowMs: Long): Long =
        targetEpochSeconds * MILLIS_PER_SECOND - nowMs

    /**
     * Milliseconds until the displayed value would next change.
     *
     * Why this and not a fixed interval: a display at [Resolution.Seconds] is the remainder
     * floored to a second, so it changes when the gap crosses a whole second — not one second
     * after some arbitrary moment a composition happened to start. Waking on the boundary costs
     * exactly one state write per visible change; waking every 1000ms from a start point half
     * way through a second shows some values twice and skips others, which on a clock reads as
     * a stutter.
     *
     * Zero once the moment has passed: there is nothing further to wait for.
     */
    fun delayToNextStep(targetEpochSeconds: Long, nowMs: Long, resolution: Resolution): Long {
        val remainingMs = millisUntil(targetEpochSeconds, nowMs)
        if (remainingMs <= 0L) return 0L

        val step = resolution.stepMillis
        val pastTheStep = remainingMs % step
        return if (pastTheStep == 0L) step else pastTheStep
    }

    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60L
    private const val SECONDS_PER_DAY = SECONDS_PER_HOUR * 24L
}
