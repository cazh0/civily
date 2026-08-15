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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
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
 * Where a page is printed: on its own, or as one sheet in a pile.
 *
 * The two supplied frames of a design differ only in vertical proportion, and the body band is
 * the one part of that every banded design agrees on — a pile is drawn on deeper paper. What is
 * left over, the two bands above it, each design states for itself.
 */
enum class NewspaperStyle(val body: Float) {
    /** `Newspaper.svg` and `npVariant2.svg`, 594-wide frames: one issue, alone on the screen. */
    FrontPage(body = 111.61f / 594f),

    /** `recentHeadlines.svg` and `newspaperStack.svg`, 480-wide sheets: one paper in a pile. */
    Stacked(body = 128f / 480f),
}

/**
 * The bands a banded design divides its sheet into, below the torn top edge every design shares,
 * as fractions of the sheet's own width.
 */
internal class NewspaperBands(
    val mastheadBand: Float,
    val headlineBand: Float,
    val body: Float,
    val headlineLines: Int,
) {
    /** How tall the whole sheet is, as a fraction of its width. */
    val aspect: Float get() = TOP_EDGE + mastheadBand + headlineBand + body

    /** The top of the headline band, which is where every banded design's headline box sits. */
    val headlineTop: Float get() = TOP_EDGE + mastheadBand
}

/** One line of print, as a fraction of the sheet's width: where it runs, and where it ends. */
internal data class TextBox(val left: Float, val right: Float, val foot: Float)

/**
 * How tall a design's sheet is, and the lines its headline came out on — all as fractions of the
 * sheet's own width.
 *
 * A line and not the paragraph. The paragraph's box is as wide as the measure it was given, and
 * the sheet below is turned, so clearing that box means clearing a corner of empty paper past
 * the end of the shortest line — which on a headline that breaks to a short last line holds the
 * next paper a finger below where it could sit. Every line's own box is asked instead, and each
 * one only has to be clear of the paper that lands on top of it.
 *
 * Only the headline is stated. Everything a design prints above it — mastheads, flags, price,
 * edition line — is both higher and no wider, so a sheet clearing these clears those.
 */
internal data class SheetExtent(val height: Float, val lines: List<TextBox>)

/** The broadsheet's bands. `Stacked` gives the headline twice the room because it wraps. */
internal fun broadsheetBands(style: NewspaperStyle): NewspaperBands = when (style) {
    NewspaperStyle.FrontPage -> NewspaperBands(
        mastheadBand = 56.94f / 594f,
        headlineBand = 32f / 594f,
        body = style.body,
        headlineLines = 1,
    )

    NewspaperStyle.Stacked -> NewspaperBands(
        mastheadBand = 47.78f / 480f,
        headlineBand = 52f / 480f,
        body = style.body,
        headlineLines = 2,
    )
}

/**
 * The proportions of a design as one sheet [width] wide in a pile, with this headline on it.
 *
 * The headline is an argument because it is the string that decides the answer: it is the
 * lowest thing on every one of the three papers, and how far down it reaches is how far it had
 * to shrink to fit.
 */
@Composable
internal fun stackedExtent(design: Newspaper.Design, headline: String, width: Dp): SheetExtent =
    when (design) {
        Newspaper.Design.Broadsheet -> broadsheetExtent(headline, width)
        Newspaper.Design.Tabloid -> tabloidExtent(headline, width)
        Newspaper.Design.Berliner -> berlinerExtent(headline, width)
    }

/** The broadsheet's, whose headline is ranged left at the top of its own band. */
@Composable
private fun broadsheetExtent(headline: String, width: Dp): SheetExtent {
    val bands = broadsheetBands(NewspaperStyle.Stacked)
    return SheetExtent(
        height = bands.aspect,
        lines = broadsheetHeadline(headline, width, bands)
            .boxes(MARGIN, bands.headlineTop + HEADLINE_Y, width),
    )
}

/** The type the broadsheet's headline comes out at, which both the page and a pile ask for. */
@Composable
private fun broadsheetHeadline(headline: String, width: Dp, bands: NewspaperBands): FittedType =
    rememberFittedType(
        text = headline,
        maxWidth = width * HEADLINE_W,
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = (width * HEADLINE_SIZE).toSp(),
            lineHeight = (width * HEADLINE_SIZE * HEADLINE_LEADING).toSp(),
        ),
        maxLines = bands.headlineLines,
    )

/**
 * An issue's front page, on whichever of the game's three papers that issue prints on.
 *
 * The choice is [Newspaper.design]'s, not this function's, so it is the same paper in the list
 * and in the detail. Every screen showing a single issue goes through here, and so does every
 * sheet of the aftermath pile.
 */
@Composable
fun NewspaperForIssue(
    issueId: Int,
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
    when (Newspaper.design(issueId)) {
        Newspaper.Design.Broadsheet -> NewspaperFrontPage(
            masthead = masthead,
            edition = edition,
            headline = headline,
            modifier = modifier,
            style = style,
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
            style = style,
            price = price,
            flagUrl = flagUrl,
            imageUrls = imageUrls,
            imageLoader = imageLoader,
        )

        Newspaper.Design.Berliner -> NewspaperBerliner(
            masthead = masthead,
            edition = edition,
            headline = headline,
            modifier = modifier,
            style = style,
            price = price,
            flagUrl = flagUrl,
            imageUrls = imageUrls,
            imageLoader = imageLoader,
        )
    }
}

/**
 * The broadsheet front page, transcribed from the supplied Figma frames.
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
    val bands = broadsheetBands(style)

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
                val ruleBase = bands.mastheadBand - RULES_FROM_BAND_FOOT
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
                    style = broadsheetHeadline(headline, w, bands).style,
                    color = NewsprintHeadlineInk,
                    maxLines = bands.headlineLines,
                    softWrap = bands.headlineLines > 1,
                    // Why the top and not the middle: the frame sets the headline against the
                    // top of its band, and the sheet below covers the band's foot. Centring it
                    // would push a wrapped headline under the next paper.
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = px(MARGIN), y = px(HEADLINE_Y))
                        .width(px(HEADLINE_W)),
                )
            }

            NewspaperBody(imageUrls, imageLoader, w, px(bands.body))
        }
    }
}

/** The torn top of the sheet, which is `dpaper1` at its own aspect on every design. */
@Composable
internal fun NewspaperTopEdge(height: Dp) {
    Image(
        painter = painterResource(R.drawable.newspaper_edge_top),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
    )
}

/**
 * The foot of the sheet: two photographs, and `dpaper5` torn over them.
 *
 * Under the strip, not over it: the strip's windows are holes, and its torn edges are what gives
 * a photo printed on newsprint its border. Both banded designs cut the same two windows in the
 * same strip, so both print their pictures through this.
 */
@Composable
internal fun NewspaperBody(
    imageUrls: List<String>,
    imageLoader: ImageLoader?,
    width: Dp,
    band: Dp,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(band),
    ) {
        NewspaperPhoto(
            url = imageUrls.getOrNull(0),
            imageLoader = imageLoader,
            x = width * PHOTO1_X,
            y = band * PHOTO_Y,
            width = width * PHOTO1_W,
            height = band * PHOTO_H,
        )
        NewspaperPhoto(
            url = imageUrls.getOrNull(1),
            imageLoader = imageLoader,
            x = width * PHOTO2_X,
            y = band * PHOTO_Y,
            width = width * PHOTO2_W,
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
        style = rememberFittedType(text, maxWidth, style, maxLines).style,
        color = color,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        modifier = modifier,
    )
}

/** The size a string comes out at once it has been fitted, and the lines it then takes. */
internal class FittedType(val style: TextStyle, val height: Dp, val lines: List<LineBox>) {

    /**
     * Those lines as boxes on a sheet [width] wide, with the text box's own corner at [x], [y].
     *
     * `getLineLeft` and `getLineRight` already carry the alignment, so a centred headline
     * reports the box its ink is centred in rather than the measure it was centred within.
     */
    fun boxes(x: Float, y: Float, width: Dp): List<TextBox> = boxes(x, y, width) { it.foot }

    /**
     * The same, down to each baseline instead of each line's foot.
     *
     * For type set in capitals: the room a line box keeps for descenders is empty paper on such
     * a page, and a pile spaced by it holds the next sheet a descender's depth lower than the
     * words need. What can still reach under the baseline in an alphabet of capitals is a comma,
     * and only where a line ends in one — a tenth of an em, at the single point along the line
     * where the sheet below is tangent and under its own torn edge, which is transparent there.
     */
    fun capBoxes(x: Float, y: Float, width: Dp): List<TextBox> = boxes(x, y, width) { it.baseline }

    private fun boxes(x: Float, y: Float, width: Dp, foot: (LineBox) -> Dp): List<TextBox> =
        lines.map {
            TextBox(
                left = x + it.left / width,
                right = x + it.right / width,
                foot = y + foot(it) / width,
            )
        }
}

/** One line of a fitted string, measured from the corner of the box it was laid out in. */
internal class LineBox(val left: Dp, val right: Dp, val foot: Dp, val baseline: Dp)

/**
 * The lines of a laid-out string, in the units the caller placed its text box in.
 *
 * Compose gives no per-line baseline, but every line here is set in one style, so the drop from
 * a line's foot to its baseline is the same on all of them and the last one reports it.
 */
internal fun TextLayoutResult.lineBoxes(density: Density): List<LineBox> = with(density) {
    val descent = getLineBottom(lineCount - 1) - lastBaseline
    List(lineCount) { line ->
        LineBox(
            left = getLineLeft(line).toDp(),
            right = getLineRight(line).toDp(),
            foot = getLineBottom(line).toDp(),
            baseline = (getLineBottom(line) - descent).toDp(),
        )
    }
}

/**
 * What [FittedText] would print this string as, without printing it.
 *
 * Why it is separable: the tabloid's headline is drawn twice, outline under fill, and the two
 * passes have to be the same measurement — fitting each on its own would let a string that
 * lands on a shrink boundary come out at two sizes and print as a smear. The pile asks for the
 * same reason and one more: [height] is the box the words ended up in rather than the box they
 * were offered, which is the only figure that says where a sheet may be laid over this one.
 */
@Composable
internal fun rememberFittedType(
    text: String,
    maxWidth: Dp,
    style: TextStyle,
    maxLines: Int = 1,
): FittedType {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, style, maxWidth, maxLines, density) {
        fun measure(candidate: TextStyle) = measurer.measure(
            text = AnnotatedString(text),
            style = candidate,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            constraints = with(density) { Constraints(maxWidth = maxWidth.roundToPx()) },
        )

        var candidate = style
        var measured = measure(candidate)
        var steps = 0
        while (measured.hasVisualOverflow && steps < MAX_SHRINK_STEPS) {
            candidate = candidate.copy(
                fontSize = candidate.fontSize * SHRINK_STEP,
                // Only a wrapping headline sets a line height. TextUnit arithmetic throws on
                // Unspecified, so the single-line styles must be left alone.
                lineHeight = if (candidate.lineHeight.isSpecified) {
                    candidate.lineHeight * SHRINK_STEP
                } else {
                    candidate.lineHeight
                },
            )
            measured = measure(candidate)
            steps++
        }
        FittedType(
            style = candidate,
            height = with(density) { measured.size.height.toDp() },
            lines = measured.lineBoxes(density),
        )
    }
}

@Composable
internal fun Dp.toSp() = with(LocalDensity.current) { this@toSp.toSp() }

private const val MAX_SHRINK_STEPS = 14
private const val SHRINK_STEP = 0.93f

// Geometry as fractions of the page width, from the Figma frames. Nothing here is a guess.

/** `dpaper1` at its own aspect, which is the one figure all three designs' frames agree on. */
internal const val TOP_EDGE = 47.03f / 594f

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
