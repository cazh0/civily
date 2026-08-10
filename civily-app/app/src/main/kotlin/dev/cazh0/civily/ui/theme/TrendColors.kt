package dev.cazh0.civily.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The two directional colours, resolved for the theme in force.
 *
 * Kept behind this object rather than read directly so no call site can reach for the light
 * tone on a dark screen — the mistake that makes a "success" green unreadable at night.
 */
object TrendColors {

    val up: Color
        @Composable get() = if (isSystemInDarkTheme()) TrendUpDark else TrendUpLight

    val down: Color
        @Composable get() = if (isSystemInDarkTheme()) TrendDownDark else TrendDownLight
}
