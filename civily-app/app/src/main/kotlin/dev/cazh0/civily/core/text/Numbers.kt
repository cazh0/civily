package dev.cazh0.civily.core.text

import java.text.NumberFormat
import java.util.Locale

/**
 * Digit grouping for anything the user counts.
 *
 * Fixed to [Locale.US] for the same reason as [Population]: the app ships English-only by
 * project rule, and a number grouped one way beside a word chosen another way reads as a bug.
 */
object Numbers {
    fun grouped(value: Int): String = format.get().format(value)

    /** The same, for figures that run past `Int` — a nation's economy does. */
    fun grouped(value: Long): String = format.get().format(value)

    /**
     * One formatter per thread, built once.
     *
     * Why not a fresh one per call: `getIntegerInstance` builds an ICU-backed `DecimalFormat`
     * every time it is asked — a pattern parse and a set of locale symbols, for a job that is
     * putting commas in a number. The rankings tab formats two of these per row across ninety
     * rows, so the formatter was being built more often than the app draws frames.
     *
     * Why a [ThreadLocal] and not one shared instance: `NumberFormat` is documented as not
     * thread-safe, and this is called from composition on the main thread and from the parsing
     * dispatcher underneath it. One per thread is the cheap way to be right rather than the
     * fast way to be wrong.
     */
    private val format = object : ThreadLocal<NumberFormat>() {
        override fun initialValue(): NumberFormat = NumberFormat.getIntegerInstance(Locale.US)
    }
}
