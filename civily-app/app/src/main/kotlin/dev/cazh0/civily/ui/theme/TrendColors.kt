package dev.cazh0.civily.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * The two directional colours, resolved for the theme in force.
 *
 * Kept behind this object rather than read directly so no call site can reach for the light
 * tone on a dark screen — the mistake that makes a "success" green unreadable at night.
 *
 * These same two carry every good/bad judgement in the app — a moved statistic and a freedom
 * rating alike — so that green means one thing everywhere. A second pair for a second feature
 * would be a second vocabulary the reader has to learn.
 */
object TrendColors {

    val up: Color
        @Composable get() = if (isSystemInDarkTheme()) TrendUpDark else TrendUpLight

    val down: Color
        @Composable get() = if (isSystemInDarkTheme()) TrendDownDark else TrendDownLight

    /**
     * Either accent turned into a container to sit behind text.
     *
     * Why composited rather than a fifth and sixth colour value: Material 3 builds a tinted
     * container by laying the accent over the surface it sits on at a state-layer opacity, so
     * the wash follows the theme's own surface and the palette gains nothing to keep in step.
     * Compositing rather than drawing it translucent keeps the result opaque, which is what
     * stops a tinted tile reading a different lightness from the untinted tile beside it.
     *
     * @param surface the container colour the tile would have had with no judgement to carry.
     */
    fun wash(accent: Color, surface: Color): Color =
        accent.copy(alpha = STATE_LAYER).compositeOver(surface)

    /** M3's own emphasis level for a state layer. Lower reads as a smudge; higher shouts. */
    private const val STATE_LAYER = 0.12f
}
