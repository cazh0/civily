package dev.cazh0.civily.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Every duration in the app.
 *
 * Why here rather than at the call sites: the Compose counterpart of spec §3's ban on size and
 * colour literals. A `tween(700)` typed into a NavHost is a design decision nobody can find
 * again, and it is the difference between a screen that answers and a screen that thinks about
 * it.
 *
 * The numbers are short on purpose. A transition is not the app doing work — the destination is
 * already composed before it starts — so every millisecond of it is a millisecond the user
 * waits for something that has already happened. Material's own guidance puts a simple fade at
 * one to two frames past perception; the library default this replaces is 700ms, which on a
 * back press reads as the app hesitating.
 */
object Motion {

    /** Arriving screen. Long enough to be seen as motion, short enough not to be waited on. */
    const val ScreenEnterMillis = 110

    /**
     * Leaving screen, deliberately shorter than the arrival.
     *
     * Two full screens draw for the whole overlap, so the exit is also the only part of a
     * transition with a frame cost worth naming. It ends first; the arriving screen finishes
     * alone.
     */
    const val ScreenExitMillis = 80

    /**
     * A flag's plate settling onto the colour mixed from the flag.
     *
     * Slower than a screen change because nothing is waiting on it: the picture is already
     * there, and the plate catching up under it should not read as a flash.
     */
    const val PlateMillis = 220

    /**
     * A count badge arriving, and its digits rolling over.
     *
     * The one deliberately springy thing in the app. A badge appears because something is now
     * waiting for the reader, so it is allowed to arrive with a little weight — a linear fade
     * would put it on screen without ever saying it was not there a moment ago. The overshoot
     * is small: this is a notification count, not a toy.
     */
    val BadgeEnter: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /**
     * One digit replacing another, and the badge crossing over with the chevron it stands in for.
     *
     * Long enough to be read as the same object changing rather than as two objects swapped,
     * short enough that a reader who glanced at the row already has the new value.
     */
    const val BadgeRollMillis = 200

    /**
     * The standard Material easing, for anything that moves.
     *
     * A cross-fade uses `LinearEasing` instead: an eased alpha ramp leaves both layers sitting
     * near half opacity for longer than the curve suggests, which reads as a stall rather than
     * as a transition.
     */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
