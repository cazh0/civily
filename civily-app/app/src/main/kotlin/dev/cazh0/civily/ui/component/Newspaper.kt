package dev.cazh0.civily.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.Newspaper
import dev.cazh0.civily.ui.theme.Newsprint
import dev.cazh0.civily.ui.theme.NewsprintHeadlineInk
import dev.cazh0.civily.ui.theme.NewsprintInk
import dev.cazh0.civily.ui.theme.NewsprintRule
import dev.cazh0.civily.ui.theme.NewsprintSubhead

/**
 * How tall the bands are. The two supplied frames differ only in this.
 *
 * Their horizontal geometry — margins, rule width, flag, masthead and price positions — agrees
 * to within a third of a percent, so it is expressed once. The vertical proportions do not: the
 * stacked frame gives the headline twice the room because its headlines wrap, and a deeper body.
 */
enum class NewspaperStyle(
    val mastheadBand: Float,
    val headlineBand: Float,
    val body: Float,
    val headlineLines: Int,
) {
    /** `Newspaper.svg`, a 594-wide frame: one issue, one line of headline. */
    FrontPage(
        mastheadBand = 56.94f / 594f,
        headlineBand = 32f / 594f,
        body = 111.61f / 594f,
        headlineLines = 1,
    ),

    /** `recentHeadlines.svg`, a 480-wide paper: stacked, headline wraps to two lines. */
    Stacked(
        mastheadBand = 47.78f / 480f,
        headlineBand = 52f / 480f,
        body = 128f / 480f,
        headlineLines = 2,
    ),
    ;

    /** Total height as a fraction of width — what a caller needs to lay a stack out. */
    val aspect: Float get() = TOP_EDGE + mastheadBand + headlineBand + body
}

/**
 * An issue's front page, on whichever of the game's two papers that issue prints on.
 *
 * The choice is [Newspaper.design]'s, not this function's, so it is the same paper in the list
 * and in the detail. Every screen showing a single issue goes through here; only the aftermath
 * pile addresses [NewspaperFrontPage] directly, because a pile is a shape the tabloid has no
 * frame for.
 */
@Composable
fun NewspaperForIssue(
    issueId: Int,
    masthead: String,
    edition: Newspaper.Edition,
    headline: String,
    modifier: Modifier = Modifier,
    price: String? = null,
    flagUrl: String? = null,
    imageUrls: List<String> = emptyList(),
    imageLoader: ImageLoader? = null,
) {
    when (Newspaper.design(issueId)) {
        Newspaper.Design.Broadsheet -> NewspaperFrontPage(
            masthead = masthead,
            edition = edition,
            headline = headline,
            modifier = modifier,
            price = price,
            flagUrl = flagUrl,
            imageUrls = imageUrls,
            imageLoader = imageLoader,
        )

        Newspaper.Design.Tabloid -> NewspaperTabloid(
            masthead = masthead,
            edition = edition,
            headline = headline,
            modifier = modifier,
            price = price,
            flagUrl = flagUrl,
            imageUrls = imageUrls,
            imageLoader = imageLoader,
        )
    }
}

/**
 * The front page, transcribed from the supplied Figma frames.
 *
 * Every position is a fraction of the component's own width, so this is the same page on a
 * 380dp phone and a 900dp tablet rather than a picture that happens to fit one of them.
 *
 * The strips are NationStates' own, bundled in `drawable-nodpi`: `dpaper1` is the torn top,
 * `dpaper2` the masthead band — which runs on past the title to carry the rules and edition
 * line — `dpaper4` the headline band and `dpaper5` the body. (`dpaper3` is in neither frame
 * nor on their server.)
 *
 * Nothing is painted behind the page. The strips carry their own paper and their own torn
 * edges, and anything solid behind them turns the page back into a rectangle. The body's gap
 * is left open: that is where the artwork goes when the API supplies it.
 *
 * Type is sized from the width rather than in `sp` — a composition, not a paragraph. That
 * trade is recorded in the README's gap list.
 */
@Composable
fun NewspaperFrontPage(
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
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Captured because the nested Boxes shadow this scope's `maxWidth`.
        val w = maxWidth
        val px: (Float) -> Dp = { w * it }

        Column(Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.newspaper_edge_top),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(px(TOP_EDGE)),
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(px(style.mastheadBand)),
            ) {
                Image(
                    painter = painterResource(R.drawable.newspaper_band_masthead),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
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

                FittedText(
                    text = masthead,
                    maxWidth = px(MASTHEAD_W),
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = px(MASTHEAD_SIZE).toSp(),
                        textAlign = TextAlign.Center,
                    ),
                    color = NewsprintInk,
                    modifier = Modifier
                        .offset(x = px(MASTHEAD_X), y = px(MASTHEAD_Y))
                        .width(px(MASTHEAD_W)),
                )

                price?.let {
                    FittedText(
                        text = it,
                        maxWidth = px(PRICE_W),
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = px(SMALL_SIZE).toSp(),
                            textAlign = TextAlign.End,
                        ),
                        color = NewsprintInk,
                        modifier = Modifier
                            .offset(x = px(PRICE_X), y = px(PRICE_Y))
                            .width(px(PRICE_W)),
                    )
                }

                // Rules sit against the foot of the band, so they follow its height.
                val ruleBase = style.mastheadBand - RULES_FROM_BAND_FOOT
                PaperBlock(px(MARGIN), px(ruleBase), px(RULE_W), px(RULE_THICK), NewsprintRule)
                PaperBlock(px(MARGIN), px(ruleBase + RULE_2_OFFSET), px(RULE_W), px(RULE_THIN), NewsprintRule)
                PaperBlock(px(MARGIN), px(ruleBase + RULE_3_OFFSET), px(RULE_W), px(RULE_THIN), NewsprintRule)

                val editionStyle = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = px(SMALL_SIZE).toSp(),
                )
                val editionY = px(ruleBase + EDITION_OFFSET)
                EditionCell(edition.city, px(MARGIN), editionY, px(EDITION_L_W), editionStyle, TextAlign.Start, NewsprintSubhead)
                EditionCell(edition.date, px(EDITION_C_X), editionY, px(EDITION_C_W), editionStyle, TextAlign.Center, NewsprintSubhead)
                EditionCell(edition.volume, px(EDITION_R_X), editionY, px(EDITION_R_W), editionStyle, TextAlign.End, NewsprintSubhead)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(px(style.headlineBand)),
            ) {
                Image(
                    painter = painterResource(R.drawable.newspaper_band_headline),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
                FittedText(
                    text = headline,
                    maxWidth = px(HEADLINE_W),
                    maxLines = style.headlineLines,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = px(HEADLINE_SIZE).toSp(),
                        lineHeight = px(HEADLINE_SIZE * HEADLINE_LEADING).toSp(),
                    ),
                    color = NewsprintHeadlineInk,
                    // Why the top and not the middle: the frame sets the headline against the
                    // top of its band, and the sheet below covers the band's foot. Centring it
                    // would push a wrapped headline under the next paper.
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = px(MARGIN), y = px(HEADLINE_Y))
                        .width(px(HEADLINE_W)),
                )
            }

            val band = px(style.body)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(band),
            ) {
                // Under the strip, not over it: the strip's windows are holes, and its torn
                // edges are what gives a photo printed on newsprint its border.
                NewspaperPhoto(
                    url = imageUrls.getOrNull(0),
                    imageLoader = imageLoader,
                    x = w * PHOTO1_X,
                    y = band * PHOTO_Y,
                    width = w * PHOTO1_W,
                    height = band * PHOTO_H,
                )
                NewspaperPhoto(
                    url = imageUrls.getOrNull(1),
                    imageLoader = imageLoader,
                    x = w * PHOTO2_X,
                    y = band * PHOTO_Y,
                    width = w * PHOTO2_W,
                    height = band * PHOTO_H,
                )
                Image(
                    painter = painterResource(R.drawable.newspaper_edge_bottom),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * One photograph, in the space the page leaves for it.
 *
 * Why paper is laid down first: on this page that space is a window cut out of the body strip,
 * so an empty one shows the screen straight through it. Newsprint is what is behind a
 * photograph on a real front page, and it is what the window should show while one loads — or
 * when the source has none to give. The tabloid prints its photographs over the strip instead
 * of through it, and it lays the same floor for the same reason: the windows are still there.
 */
@Composable
internal fun NewspaperPhoto(
    url: String?,
    imageLoader: ImageLoader?,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(width = width, height = height)
            .background(Newsprint),
    ) {
        if (url != null && imageLoader != null) {
            AsyncImage(
                model = url,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** A solid rectangle at a place on the page: a rule, a plate, a border, a printed shadow. */
@Composable
internal fun PaperBlock(x: Dp, y: Dp, width: Dp, height: Dp, color: Color) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .width(width)
            .height(height)
            .background(color),
    )
}

/** One of the three cells — city, date, volume — the edition line is divided into. */
@Composable
internal fun EditionCell(
    text: String,
    x: Dp,
    y: Dp,
    width: Dp,
    style: TextStyle,
    align: TextAlign,
    color: Color,
) {
    FittedText(
        text = text,
        maxWidth = width,
        style = style.copy(textAlign = align),
        color = color,
        modifier = Modifier
            .offset(x = x, y = y)
            .width(width),
    )
}

/**
 * Type that shrinks to fit its box instead of wrapping past it or clipping.
 *
 * Nation names run from "Bacata" to forty characters and headlines are whatever the game
 * wrote, but the bands are fixed. Shrinking keeps both the layout and the words.
 */
@Composable
internal fun FittedText(
    text: String,
    maxWidth: Dp,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        style = rememberFittedStyle(text, maxWidth, style, maxLines),
        color = color,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        modifier = modifier,
    )
}

/**
 * The size [FittedText] would print this string at, without printing it.
 *
 * Why it is separable: the tabloid's headline is drawn twice, outline under fill, and the two
 * passes have to be the same measurement. Fitting each of them on its own would let a string
 * that lands on a shrink boundary come out at two sizes and print as a smear.
 */
@Composable
internal fun rememberFittedStyle(
    text: String,
    maxWidth: Dp,
    style: TextStyle,
    maxLines: Int = 1,
): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, style, maxWidth, maxLines, density) {
        var candidate = style
        repeat(MAX_SHRINK_STEPS) {
            val measured = measurer.measure(
                text = AnnotatedString(text),
                style = candidate,
                maxLines = maxLines,
                softWrap = maxLines > 1,
                constraints = with(density) {
                    androidx.compose.ui.unit.Constraints(maxWidth = maxWidth.roundToPx())
                },
            )
            if (!measured.hasVisualOverflow) return@remember candidate
            candidate = candidate.copy(
                fontSize = candidate.fontSize * SHRINK_STEP,
                // Only the wrapping headline sets a line height. TextUnit arithmetic throws
                // on Unspecified, so the single-line styles must be left alone.
                lineHeight = if (candidate.lineHeight.isSpecified) {
                    candidate.lineHeight * SHRINK_STEP
                } else {
                    candidate.lineHeight
                },
            )
        }
        candidate
    }
}

@Composable
internal fun Dp.toSp() = with(LocalDensity.current) { this@toSp.toSp() }

private const val MAX_SHRINK_STEPS = 14
private const val SHRINK_STEP = 0.93f

// Geometry as fractions of the page width, from the Figma frames. Nothing here is a guess.
private const val TOP_EDGE = 47.03f / 594f
private const val MARGIN = 29.69f / 594f
private const val RULE_W = 510.86f / 594f
private const val FLAG_X = 76.15f / 594f
private const val FLAG_Y = (51.5f - 47.03f) / 594f
private const val FLAG_W = 40f / 594f
private const val FLAG_H = 24f / 594f
private const val MASTHEAD_X = 154.04f / 594f
private const val MASTHEAD_Y = (50.5f - 47.03f) / 594f
private const val MASTHEAD_W = 225f / 594f
private const val PRICE_X = 417.53f / 594f
private const val PRICE_Y = (58.5f - 47.03f) / 594f
private const val PRICE_W = 39.39f / 594f

// The rule block hangs off the foot of the masthead band: the first rule starts 24 design
// units above it, and the rest follow at the frame's own spacing.
private const val RULES_FROM_BAND_FOOT = 24f / 594f
private const val RULE_THICK = 4f / 594f
private const val RULE_THIN = 2f / 594f
private const val RULE_2_OFFSET = 6f / 594f
private const val RULE_3_OFFSET = 22f / 594f
private const val EDITION_OFFSET = 10f / 594f
private const val EDITION_C_X = 193.13f / 594f
private const val EDITION_C_W = 153.78f / 594f
private const val EDITION_R_X = 465.42f / 594f
private const val EDITION_R_W = 75.13f / 594f
private const val EDITION_L_W = 44.92f / 594f
private const val HEADLINE_W = 522.73f / 594f

/** The frame sets the headline 0.62 units below the top of its band, on a 480-unit sheet. */
private const val HEADLINE_Y = 0.62f / 480f

/**
 * The two windows cut out of `newspaper_edge_bottom`, measured off the strip's own 745×140.
 *
 * Why the strip and not the frames: the strip is what decides where the paper is missing, and
 * it is stretched to whatever height the style's body band is — 111.61/594 on the front page,
 * 128/480 stacked, a fifth taller. Sizing a photo from the page width, as the frames' numbers
 * do, registers it with the windows at one of those heights and misses at the other.
 *
 * So x and width follow the width; y and height follow the band. Each rect is bled a unit past
 * its window on every side, so rounding to whole pixels cannot leave a seam of bare screen;
 * the overhang lands on opaque newsprint.
 */
private const val STRIP_W = 745f
private const val STRIP_H = 140f
private const val PHOTO1_X = 37f / STRIP_W
private const val PHOTO1_W = 413f / STRIP_W
private const val PHOTO2_X = 578f / STRIP_W
private const val PHOTO2_W = 106f / STRIP_W
private const val PHOTO_Y = 2f / STRIP_H
private const val PHOTO_H = 117f / STRIP_H

private const val MASTHEAD_SIZE = 21f / 594f
private const val HEADLINE_SIZE = 26f / 594f
private const val HEADLINE_LEADING = 1.18f
private const val SMALL_SIZE = 8.5f / 594f
