package dev.cazh0.civily.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

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
 * Newsprint, for the photo windows cut out of `dpaper5`. Fixed in both themes — the one surface
 * that does not follow the wallpaper, because a newspaper that did would stop reading as a
 * newspaper, which is the entire point of it.
 *
 * Sampled from the strip's own paper along the edges of those windows, so a blank one abuts the
 * printed page without a seam.
 */
internal val Newsprint = Color(0xFFD6D3C7)

/** Ink values taken from the supplied SVG, not eyeballed. */
internal val NewsprintInk = Color(0xFF333333)
internal val NewsprintHeadlineInk = Color(0xFF444444)
internal val NewsprintRule = Color(0xFF444444)
internal val NewsprintSubhead = Color(0xFF666666)

/**
 * The tabloid front page's second colour, from `newNewspaper.svg`.
 *
 * The broadsheet above is ink on paper and nothing else. This one is printed: a slate plate
 * behind the masthead and under the edition line, red rules, and the paper's own cream showing
 * through as the letters. Fixed in both themes for the reason [Newsprint] is.
 *
 * The three carrying an alpha are the frame's own drop shadows and borders, which are offsets
 * with no blur — a printed edge, not an elevation.
 */
internal val TabloidPlate = Color(0xFF263746)
internal val TabloidPlateShadow = Color(0x59263746)
internal val TabloidPhotoEdge = Color(0xD9263746)
internal val TabloidFlagShadow = Color(0x73263746)
internal val TabloidRule = Color(0xFF762A22)
internal val TabloidInk = Color(0xFFF4EFE3)
internal val TabloidHeadlineInk = Color(0xFFE7E2D8)
internal val TabloidHeadlineEdge = Color(0xFF555555)
internal val TabloidFlagFrame = Color(0xFFD0D0D0)

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
 * Slice colours for the three shares-of-a-whole charts, and the four economic sectors.
 *
 * Why fixed rather than theme-derived, the same argument as [AvatarPalette]: a slice is
 * identified by its colour and nothing else — the chart draws no labels on it — so twelve of
 * them have to be told apart at a glance, and a palette derived from a wallpaper cannot promise
 * twelve distinguishable hues, let alone the same twelve tomorrow.
 *
 * These are the values the desktop site and the legacy client both use, so a player who knows
 * the game's own charts recognises Civily's. [ChartPalette] is indexed and cycles; a government
 * department and an economic sector each take their colour from their position in their own enum,
 * so a department keeps its colour from one nation to the next.
 */
internal val ChartPalette = listOf(
    Color(0xFF2196F3),
    Color(0xFFF44336),
    Color(0xFF9C27B0),
    Color(0xFF4CAF50),
    Color(0xFF4DD0E1),
    Color(0xFF795548),
    Color(0xFF00695C),
    Color(0xFF283593),
    Color(0xFFFDD835),
    Color(0xFFFF9800),
    Color(0xFFEC407A),
    Color(0xFFAED581),
    Color(0xFF607D8B),
    Color(0xFF03A9F4),
    Color(0xFF6D4C41),
    Color(0xFF7E57C2),
    Color(0xFFFFEB3B),
    Color(0xFFAD1457),
    Color(0xFFE57373),
    Color(0xFF4DB6AC),
    Color(0xFF37474F),
    Color(0xFF512DA8),
    Color(0xFF1B5E20),
)

/** The economy's four sectors, which the game gives their own four colours. */
internal val SectorPalette = listOf(
    Color(0xFF2196F3),
    Color(0xFFFFC107),
    Color(0xFFF44336),
    Color(0xFF455A64),
)

/**
 * Avatar backgrounds, picked deterministically from a nation's id.
 *
 * Why fixed rather than theme-derived: an avatar's job is to make the same author instantly
 * recognisable down a long board. A palette that shifts with the wallpaper cannot do that.
 * Each is dark enough for white initials to clear contrast requirements in either theme.
 */
private val AvatarPalette = listOf(
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

/**
 * The avatar background a nation always gets.
 *
 * Derived from the id rather than the display name, so it survives the name being rendered
 * differently anywhere.
 *
 * Why the hash widens to a Long first: `Int.MIN_VALUE.absoluteValue` is still `Int.MIN_VALUE`,
 * so a name that happened to hash to it would index the list with a negative number and take
 * the app down. There is no Int the widened form cannot make positive.
 */
internal fun avatarColor(nationId: String): Color =
    AvatarPalette[(nationId.hashCode().toLong().absoluteValue % AvatarPalette.size).toInt()]
