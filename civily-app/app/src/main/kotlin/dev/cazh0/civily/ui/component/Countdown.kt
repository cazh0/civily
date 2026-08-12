package dev.cazh0.civily.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.Countdown
import kotlinx.coroutines.delay

/**
 * A live countdown to [targetEpochSeconds], ticking only while the screen is on.
 *
 * Why the lifecycle gate: a composition survives the app going to the background, and a
 * `delay` loop inside one goes on waking the process to write a value nobody can see. Reading
 * the lifecycle as state instead of catching a callback keeps the whole thing declarative —
 * the loop simply does not exist while the screen is stopped, and on the way back it reads the
 * clock rather than resuming from where it stopped, so a wait does not come back short by
 * however long the app was away.
 *
 * Why the caller picks a [Countdown.Resolution]: a row in a list is read at a glance and says
 * "1h 14m", so it has to change once a minute; a clock has to change once a second. Ticking
 * the row per second would be fifty-nine recompositions an hour spent redrawing the same
 * characters.
 */
@Composable
fun rememberCountdown(
    targetEpochSeconds: Long,
    resolution: Countdown.Resolution,
): Countdown.Remaining {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    val visible = lifecycleState.isAtLeast(Lifecycle.State.STARTED)

    // Why remembered: `produceState` reads its initial value on every recomposition and uses it
    // on the first, so an un-remembered `System.currentTimeMillis()` here would be a clock read
    // per frame for an answer only the first frame wants.
    val initial = remember(targetEpochSeconds) {
        Countdown.until(targetEpochSeconds, System.currentTimeMillis())
    }

    return produceState(initial, targetEpochSeconds, resolution, visible) {
        if (!visible) return@produceState

        while (true) {
            val now = System.currentTimeMillis()
            value = Countdown.until(targetEpochSeconds, now)
            if (value.isDue) return@produceState

            delay(Countdown.delayToNextStep(targetEpochSeconds, now, resolution))
        }
    }.value
}

/**
 * Calls [onReached] when the clock passes [targetEpochSeconds] with the screen in front of the
 * reader.
 *
 * This is what makes a wait end by itself: the screen counting down to an issue is the screen
 * that should notice it has arrived, rather than leaving the reader to pull a list they were
 * already told was about to change.
 *
 * It cannot become a poll. The effect is keyed on the instant being waited for, so a reload
 * that hands back the same instant — which is what a device clock a little ahead of the
 * server's looks like — does not restart it. A reload that hands back a later one is a new wait
 * and restarts it exactly once.
 *
 * Resumed rather than started: this fires a request, and the app must not spend one on behalf
 * of a reader who is looking at something else.
 */
@Composable
fun CountdownReachedEffect(targetEpochSeconds: Long, onReached: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    val resumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val reached by rememberUpdatedState(onReached)

    LaunchedEffect(targetEpochSeconds, resumed) {
        if (!resumed) return@LaunchedEffect

        // Zero or less on arrival means the moment passed while the reader was elsewhere, so
        // what is on screen is already out of date and the reload is owed immediately.
        val waitMs = Countdown.millisUntil(targetEpochSeconds, System.currentTimeMillis())
        if (waitMs > 0) delay(waitMs)

        reached()
    }
}

/**
 * "1h 14m" — the glanceable form, for a line that has one phrase of room.
 *
 * Coarse on purpose: the seconds are noise beside a wait measured in hours, and a value that
 * changes under the reader's eyes on a list row draws attention the row has not earned.
 */
@Composable
fun countdownCompact(remaining: Countdown.Remaining): String = when {
    remaining.days > 0 -> stringResource(
        R.string.countdown_days,
        remaining.days,
        remaining.hours,
    )

    remaining.hours > 0 -> stringResource(
        R.string.countdown_hours,
        remaining.hours,
        remaining.minutes,
    )

    remaining.minutes > 0 -> stringResource(R.string.countdown_minutes, remaining.minutes)

    // Under a minute, due included. "0m" is not a countdown, and a row at [Resolution.Minutes]
    // does not wake often enough to print a second that would still be true when it is read.
    else -> stringResource(R.string.countdown_moments)
}

/**
 * "1:14:32" — the clock form, for a screen whose whole subject is the wait.
 *
 * The hours field is always written even at zero, so the string keeps its shape as the hour
 * rolls over and the reader's eye keeps its place. See `NextIssueClock` for the tabular figures
 * that hold the *width* still as the digits change.
 */
@Composable
fun countdownClock(remaining: Countdown.Remaining): String = when {
    remaining.isDue -> stringResource(R.string.countdown_moments)
    remaining.days > 0 -> stringResource(
        R.string.countdown_clock_days,
        remaining.days,
        remaining.hours,
        remaining.minutes,
        remaining.seconds,
    )

    else -> stringResource(
        R.string.countdown_clock,
        remaining.hours,
        remaining.minutes,
        remaining.seconds,
    )
}
