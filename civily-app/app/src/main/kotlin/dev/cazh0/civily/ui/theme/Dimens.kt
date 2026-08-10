package dev.cazh0.civily.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Every size and radius in the app.
 *
 * Why here and nowhere else: the Compose counterpart of spec §3's ban on size and radius
 * literals outside the resource layer. A screen that needs a spacing this file does not have
 * should add a named token here, not an inline `16.dp`.
 *
 * The spacing tokens are a 4dp scale. Anything off that scale reads as a mistake even when
 * nobody can say why.
 */
object Dimens {
    /** Outer margin of a screen's content. */
    val ScreenPadding = 16.dp

    /** Gap between unrelated blocks — a search field and the result below it. */
    val SectionSpacing = 24.dp

    /** Gap between stacked cards or list rows. */
    val ItemSpacing = 12.dp

    /** Gap between lines within one card. */
    val TextSpacing = 4.dp

    val CardCornerRadius = 12.dp
    val CardPadding = 16.dp

    /** The flag at the top of a detail screen. */
    val FlagHeight = 120.dp

    /**
     * The flag beside a name in a row.
     *
     * Why 3:2 and not a square: flags are wide. Fitting one into a square leaves a third of the
     * box as empty plate above and below it, which is what makes a flag look like it does not
     * fit its frame. 3:2 is the shape most of the game's flags actually are, so the artwork
     * fills the box instead of floating in it.
     */
    val FlagThumbnailWidth = 72.dp
    val FlagThumbnailHeight = 48.dp

    val FlagCornerRadius = 6.dp

    val ProgressSize = 40.dp

    /** Illustrative icon in an empty or failed state. */
    val StateIconSize = 48.dp

    /** Minimum touch target. Anything tappable is at least this tall. */
    val TouchTarget = 48.dp

    /** The accent stripe down the side of a quoted passage. */
    val QuoteBarWidth = 3.dp

    /** The circle carrying a nation's initials beside a post. */
    val AvatarSize = 36.dp

    val PillPaddingHorizontal = 10.dp
    val PillPaddingVertical = 4.dp
    val PillIconSize = 14.dp

    /** Gap between one post and the next. Bigger than a card gap, on purpose. */
    val PostSpacing = 20.dp

    /** The hero image at the top of an issue. */
    val IssueImageHeight = 160.dp

    /** The ring around a chosen option. Thick enough to survive a low-contrast palette. */
    val SelectionBorder = 2.dp

    val HairlineBorder = 1.dp

    val OrdinalBadgeSize = 28.dp

    /** The torn paper strip along the top of a newspaper. Its own aspect is 745x59. */
    val NewspaperEdgeHeight = 18.dp

    /** The tile standing in for the dashboard's census badge sprite. */
    val TrendBadgeSize = 34.dp
    val TrendBadgeRadius = 8.dp

    /** Space reserved for a list marker, wide enough that "10." does not wrap. */
    val BulletGutter = 28.dp

    /**
     * One fact in a grid.
     *
     * Why a larger radius than a card at a smaller size: a tile is read as one of a set, and a
     * radius that stays 12dp as the box shrinks makes the set read as table cells. Rounder and
     * smaller keeps them a row of objects.
     */
    val TileCornerRadius = 20.dp
    val TilePadding = 14.dp

    /** Gap between tiles, across and down. Tighter than [ItemSpacing]: a grid is one object. */
    val GridSpacing = 8.dp

    /** Gap between one happening and the next. The feed's only separator is space. */
    val HappeningSpacing = 18.dp

    /** The dot that anchors a happening's timestamp. */
    val TimelineDotSize = 6.dp

    /** One share of a whole, drawn as a bar. Thin on purpose: the label is the content. */
    val BarHeight = 8.dp

    /** The artwork strip at the head of an enacted policy. Rift banners are wide. */
    val PolicyBannerHeight = 88.dp
}
