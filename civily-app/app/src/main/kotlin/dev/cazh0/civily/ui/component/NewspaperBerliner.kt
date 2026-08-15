package dev.cazh0.civily.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.Newspaper
import dev.cazh0.civily.ui.theme.BerlinerInk
import dev.cazh0.civily.ui.theme.NewsprintHeadlineInk

/**
 * The game's third front page, transcribed from the supplied `npVariant2.svg` frame.
 *
 * Berliner because the three papers are named for the three formats a real one is printed in,
 * and this is the middle of them: the same four strips as [NewspaperFrontPage] and the same
 * bands, arranged as a quieter, narrower paper. It mirrors the broadsheet's furniture — the
 * cover price takes the left of the masthead where the broadsheet flies its flag, the flag
 * takes the right where the broadsheet prints its price — sets the paper's name in the middle
 * of that instead of over toward one end, and rules its edition line off top and bottom with
 * three hairlines rather than hanging it under one heavy rule. The headline is centred and
 * italic where the broadsheet's is ranged left and upright. All of it prints in one slate ink.
 *
 * Every position is a fraction of the component's own width, taken from the frame's 594-unit
 * sheet, so this is the same page at any size.
 *
 * Nothing is painted behind the page, for [NewspaperFrontPage]'s reason: the strips carry their
 * own paper and their own torn edges.
 */
@Composable
fun NewspaperBerliner(
    masthead: String,
    edition: Newspaper.Edition,
    headline: String,
    modifier: Modifier = Modifier,
    style: NewspaperStyle = NewspaperStyle.FrontPage,
    price: String? = null,
    flagUrl: String? = null,
    imageUrls: List<String> = emptyList(),
    imageLoader: ImageLoader? = null,
) {
    val bands = berlinerBands(style)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Captured because the nested Boxes shadow this scope's `maxWidth`.
        val w = maxWidth
        val px: (Float) -> Dp = { w * it }

        Column(Modifier.fillMaxWidth()) {
            NewspaperTopEdge(px(TOP_EDGE))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(px(bands.mastheadBand)),
            ) {
                Image(
                    painter = painterResource(R.drawable.newspaper_band_masthead),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )

                price?.let {
                    FittedText(
                        text = it,
                        maxWidth = px(PRICE_W),
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontSize = px(PRICE_SIZE).toSp(),
                        ),
                        color = BerlinerInk,
                        modifier = Modifier
                            .offset(x = px(PRICE_X), y = px(PRICE_Y))
                            .width(px(PRICE_W)),
                    )
                }

                FittedText(
                    text = masthead,
                    maxWidth = px(MASTHEAD_W),
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = px(MASTHEAD_SIZE).toSp(),
                        letterSpacing = MASTHEAD_TRACKING,
                        textAlign = TextAlign.Center,
                    ),
                    color = BerlinerInk,
                    modifier = Modifier
                        .offset(x = px(MASTHEAD_X), y = px(MASTHEAD_Y))
                        .width(px(MASTHEAD_W)),
                )

                if (flagUrl != null && imageLoader != null) {
                    AsyncImage(
                        model = flagUrl,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .offset(x = px(FLAG_X), y = px(FLAG_Y))
                            .size(width = px(FLAG_W), height = px(FLAG_H)),
                    )
                }

                EditionLine(edition, px)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(px(bands.headlineBand)),
            ) {
                Image(
                    painter = painterResource(R.drawable.newspaper_band_headline),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = headline,
                    style = berlinerHeadline(headline, w).style,
                    color = NewsprintHeadlineInk,
                    maxLines = HEADLINE_LINES,
                    softWrap = false,
                    modifier = Modifier
                        .offset(x = px(MARGIN), y = px(HEADLINE_Y))
                        .width(px(RULE_W)),
                )
            }

            NewspaperBody(imageUrls, imageLoader, w, px(bands.body))
        }
    }
}

/**
 * The bands, which this paper draws the same in both frames.
 *
 * Only the body differs between them, and it differs the same way it does on the broadsheet.
 * The pile's frame does draw the masthead band half a percent of the sheet's width deeper —
 * paper below the last rule, not room made for anything — and the rules and the edition line
 * ride down with it by less than that again. Carrying two sets of numbers to move type by a
 * pixel and a half on a phone would say those bands are different when they are one band.
 */
internal fun berlinerBands(style: NewspaperStyle): NewspaperBands = NewspaperBands(
    mastheadBand = 48.94f / FRAME,
    headlineBand = 29.2768f / FRAME,
    body = style.body,
    headlineLines = HEADLINE_LINES,
)

/** The proportions of this page as one sheet in a pile. */
@Composable
internal fun berlinerExtent(headline: String, width: Dp): SheetExtent {
    val bands = berlinerBands(NewspaperStyle.Stacked)
    return SheetExtent(
        height = bands.aspect,
        lines = berlinerHeadline(headline, width)
            .boxes(MARGIN, bands.headlineTop + HEADLINE_Y, width),
    )
}

/** The type this page's headline comes out at, which both the page and a pile ask for. */
@Composable
private fun berlinerHeadline(headline: String, width: Dp): FittedType =
    rememberFittedType(
        text = headline,
        maxWidth = width * RULE_W,
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSize = (width * HEADLINE_SIZE).toSp(),
            textAlign = TextAlign.Center,
        ),
        maxLines = HEADLINE_LINES,
    )

/**
 * Three hairlines, and the edition line ruled off between the second and the third.
 *
 * The cells are the thirds of the ruled width rather than the frame's own three boxes, which
 * measure 216.7, 77.45 and 216.7: that middle figure hugs one date in the frame's own face, and
 * a hug is a measurement rather than a margin — the same reason the tabloid's masthead is not
 * held to its frame's 219.88. Ranged out, centred and ranged in over equal thirds, the frame's
 * own strings land exactly where the frame puts them, and a longer one has somewhere to go.
 *
 * Volume, city, date — this paper's order, and the reverse of the broadsheet's.
 */
@Composable
private fun EditionLine(edition: Newspaper.Edition, px: (Float) -> Dp) {
    PaperBlock(px(MARGIN), px(RULE_1_Y), px(RULE_W), px(RULE_THIN), BerlinerInk)
    PaperBlock(px(MARGIN), px(RULE_2_Y), px(RULE_W), px(RULE_THIN), BerlinerInk)
    PaperBlock(px(MARGIN), px(RULE_3_Y), px(RULE_W), px(RULE_THIN), BerlinerInk)

    val cell = TextStyle(
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        fontSize = px(EDITION_SIZE).toSp(),
    )
    val y = px(EDITION_Y)
    val width = px(EDITION_W)
    EditionCell(edition.volume, px(MARGIN), y, width, cell, TextAlign.Start, BerlinerInk)
    // The middle of the line is the only thing on the page the frame sets upright.
    EditionCell(
        edition.city,
        px(EDITION_C_X),
        y,
        width,
        cell.copy(fontStyle = FontStyle.Normal, letterSpacing = CITY_TRACKING),
        TextAlign.Center,
        BerlinerInk,
    )
    EditionCell(
        edition.date,
        px(EDITION_R_X),
        y,
        width,
        cell.copy(letterSpacing = DATE_TRACKING),
        TextAlign.End,
        BerlinerInk,
    )
}

// Geometry as fractions of the page width, transcribed from `npVariant2.svg` on its own
// 594-unit sheet. A position inside the masthead band is written as the frame's absolute figure
// less the band's top, so every number below can be found in the frame. Nothing here is a guess.
private const val FRAME = 594f
private const val BAND_TOP = 47.03f
private const val HEADLINE_BAND_TOP = 95.97f

private const val MARGIN = 29.69f / FRAME
private const val RULE_W = 510.86f / FRAME

/**
 * The price is ranged from the frame's own x — the same fraction of the sheet in both frames,
 * which is what says it is ranged and not centred — and given the paper up to the masthead.
 */
private const val PRICE_X = 76.1538f / FRAME
private const val PRICE_Y = (57f - BAND_TOP) / FRAME
private const val PRICE_W = (125.61f - 76.1538f) / FRAME

private const val MASTHEAD_X = 125.61f / FRAME
private const val MASTHEAD_Y = (50f - BAND_TOP) / FRAME
private const val MASTHEAD_W = 281.86f / FRAME

private const val FLAG_X = 422.623f / FRAME
private const val FLAG_Y = (53.0625f - BAND_TOP) / FRAME
private const val FLAG_W = 30f / FRAME
private const val FLAG_H = 16.875f / FRAME

private const val RULE_THIN = 1f / FRAME
private const val RULE_1_Y = (75.97f - BAND_TOP) / FRAME
private const val RULE_2_Y = (80.97f - BAND_TOP) / FRAME
private const val RULE_3_Y = (94.97f - BAND_TOP) / FRAME

private const val EDITION_Y = (83.97f - BAND_TOP) / FRAME
private const val EDITION_W = RULE_W / 3f
private const val EDITION_C_X = MARGIN + EDITION_W
private const val EDITION_R_X = MARGIN + 2f * EDITION_W

private const val HEADLINE_Y = (98.35f - HEADLINE_BAND_TOP) / FRAME

/** One, in both frames: this paper's headline band is one line deep and its headline fills it. */
private const val HEADLINE_LINES = 1

/**
 * Type is stated as the cap height the frame drew, over the cap height of an em in Noto Serif —
 * 714 units of its 1000 — for [NewspaperTabloid]'s reason: two faces set at one point size do
 * not put capitals at one height, and what the reader sees is the height of the letters.
 *
 * The frame's face is a display serif the app does not ship, against the §4 row that says the
 * APK may not grow, so this page is set in the device's `serif` as the broadsheet is.
 */
private const val CAP_RATIO = 714f / 1000f

private const val MASTHEAD_SIZE = 13.996f / CAP_RATIO / FRAME
private const val HEADLINE_SIZE = 16.49f / CAP_RATIO / FRAME
private const val EDITION_SIZE = 5.474f / CAP_RATIO / FRAME

/** The frame prints the price a tenth smaller than the edition line under it. */
private const val PRICE_SIZE = 4.919f / CAP_RATIO / FRAME

/**
 * Tracking, measured off the frame as the gap it opens between two letters over the gap the
 * same face leaves shut — the headline, which it sets solid, is the ruler for the rest.
 *
 * In `em` so that it survives [FittedText] shrinking a long string: a tracking in absolute units
 * would stay the frame's width around type half the frame's size and blow the line apart.
 */
private val MASTHEAD_TRACKING = 0.11f.em
private val CITY_TRACKING = 0.13f.em
private val DATE_TRACKING = 0.3f.em
