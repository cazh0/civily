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
    fun grouped(value: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(value)
}
