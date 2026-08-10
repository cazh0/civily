package dev.cazh0.stately.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Every colour value in the app.
 *
 * Why here and nowhere else: spec §3 bans colour literals outside the resource layer. In a
 * Compose UI this file is that layer — a `Color(0xFF…)` anywhere under `feature/` is the
 * violation the rule is aimed at.
 */
internal val Indigo50 = Color(0xFFE8EAF6)
internal val Indigo200 = Color(0xFF9FA8DA)
internal val Indigo500 = Color(0xFF3F51B5)
internal val Indigo700 = Color(0xFF303F9F)
internal val Indigo900 = Color(0xFF1A237E)

internal val Teal200 = Color(0xFF80CBC4)
internal val Teal500 = Color(0xFF009688)

internal val Red300 = Color(0xFFE57373)
internal val Red700 = Color(0xFFD32F2F)

internal val Grey50 = Color(0xFFFAFAFA)
internal val Grey900 = Color(0xFF121212)
internal val Grey850 = Color(0xFF1E1E1E)

internal val White = Color(0xFFFFFFFF)
internal val Black = Color(0xFF000000)

/**
 * The plate a flag sits on, in both themes.
 *
 * Why it does not follow the colour scheme: NationStates flags are authored against the white
 * page of the website, and many are transparent PNGs whose artwork is black. On a dark surface
 * those flags disappear entirely. A light plate is slightly loud at night and completely
 * legible, which is the right trade for the one element on the screen that is a picture.
 */
internal val FlagPlate = Color(0xFFECECEC)

/**
 * Newsprint. Fixed in both themes, like [FlagPlate] and for the same reason: a newspaper that
 * followed the wallpaper would stop reading as a newspaper, which is the entire point of it.
 */
internal val Newsprint = Color(0xFFEDE8DB)

/** Ink values taken from the supplied SVG, not eyeballed. */
internal val NewsprintInk = Color(0xFF333333)
internal val NewsprintHeadlineInk = Color(0xFF444444)
internal val NewsprintRule = Color(0xFF444444)
internal val NewsprintSubhead = Color(0xFF666666)

/**
 * Rising and falling, taken from Material 3's own tonal palettes.
 *
 * These are the only colours in the app that do not come from the user's wallpaper, because
 * they carry meaning rather than style: "up" and "down" have to be legible at a glance, and a
 * palette derived from a photograph cannot promise either. Everything else on a trend — the
 * heading, the unit, the badge — follows the theme like the rest of the app.
 *
 * Each has a light and a dark tone rather than one value for both. That is the whole point of
 * the M3 system: tone 40 carries 4.5:1 against a light surface and tone 80 carries it against
 * a dark one, so both directions stay readable on any device, at any wallpaper, in either
 * theme. A single fixed colour cannot do that — it is always failing one of the two.
 *
 * Down uses M3's baseline error tones. Up uses the green tonal palette at the same tones,
 * which is what Material's own guidance does for a "success" extended colour.
 */
internal val TrendUpLight = Color(0xFF146C2E)
internal val TrendUpDark = Color(0xFF6DD58C)
internal val TrendDownLight = Color(0xFFB3261E)
internal val TrendDownDark = Color(0xFFF2B8B5)

/**
 * Avatar backgrounds, picked deterministically from a nation's id.
 *
 * Why fixed rather than theme-derived: an avatar's job is to make the same author instantly
 * recognisable down a long board. A palette that shifts with the wallpaper cannot do that.
 * Each is dark enough for white initials to clear contrast requirements in either theme.
 */
internal val AvatarPalette = listOf(
    Color(0xFF5C6BC0),
    Color(0xFF00897B),
    Color(0xFF7B1FA2),
    Color(0xFFC62828),
    Color(0xFF1565C0),
    Color(0xFF4E342E),
    Color(0xFF2E7D32),
    Color(0xFFAD1457),
    Color(0xFF455A64),
    Color(0xFFEF6C00),
)
