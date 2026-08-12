package dev.cazh0.civily.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The chart palettes, reached through here so no call site indexes a list it could run off.
 *
 * Kept beside [TrendColors] for the same reason: these are the only colours in the app that are
 * not derived from the user's wallpaper, and both places that decide one should be readable in
 * one file.
 */
object ChartColors {

    /**
     * The [index]th slice colour, cycling.
     *
     * Cycles rather than clamps because a nation can die of more ways than there are colours —
     * the game's mortality list is longer than twenty-three — and two slices sharing a colour at
     * opposite ends of a legend is survivable where an index crash is not.
     */
    fun slice(index: Int): Color = ChartPalette[index.mod(ChartPalette.size)]

    /** An economic sector's own colour, by its position in `Sector.Kind`. */
    fun sector(index: Int): Color = SectorPalette[index.mod(SectorPalette.size)]
}
