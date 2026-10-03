package dev.cazh0.civily.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.RelativeTime

/**
 * "3 hours ago", in words, never "3h" (DESIGN_RULES §4.4). The plural lives in the resource
 * layer, which is why [RelativeTime] hands back a count and a unit instead of a string.
 */
@Composable
fun elapsedWords(epochSeconds: Long, now: Long): String {
    val (count, grain) = RelativeTime.elapsed(epochSeconds, now)
    return when (grain) {
        RelativeTime.Grain.Now -> stringResource(R.string.elapsed_now)
        RelativeTime.Grain.Minutes -> pluralStringResource(R.plurals.elapsed_minutes, count, count)
        RelativeTime.Grain.Hours -> pluralStringResource(R.plurals.elapsed_hours, count, count)
        RelativeTime.Grain.Days -> pluralStringResource(R.plurals.elapsed_days, count, count)
        RelativeTime.Grain.Weeks -> pluralStringResource(R.plurals.elapsed_weeks, count, count)
        RelativeTime.Grain.Years -> pluralStringResource(R.plurals.elapsed_years, count, count)
    }
}
