package dev.cazh0.civily.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.Newspaper
import dev.cazh0.civily.ui.theme.NewsprintRule
import dev.cazh0.civily.ui.theme.TabloidFlagFrame
import dev.cazh0.civily.ui.theme.TabloidFlagShadow
import dev.cazh0.civily.ui.theme.TabloidHeadlineEdge
import dev.cazh0.civily.ui.theme.TabloidHeadlineInk
import dev.cazh0.civily.ui.theme.TabloidInk
import dev.cazh0.civily.ui.theme.TabloidPhotoEdge
import dev.cazh0.civily.ui.theme.TabloidPlate
import dev.cazh0.civily.ui.theme.TabloidPlateShadow
import dev.cazh0.civily.ui.theme.TabloidRule
import java.util.Locale

/**
 * The game's other front page, transcribed from the supplied `newNewspaper.svg` frame.
 *
 * Same paper, same three strips as [NewspaperFrontPage] — `dpaper1` torn top, `dpaper2` masthead
 * band, `dpaper5` body — and a different newspaper printed on them. Where the broadsheet is ink
 * on bare newsprint, this one is a printed page: the masthead sits on a slate plate between two
 * flags, the edition line runs white out of a slate bar under a heavy red rule, and the body is
 * given over to one photograph with the headline shouted across it. There is no headline band at
 * all, which is why this is its own component rather than a third [NewspaperStyle]: the enum
 * holds band heights, and this page does not have that band to give a height to.
 *
 * Every position is a fraction of the component's own width, taken from the frame's 594-unit
 * sheet, so this is the same page at any size. The masthead, the headline and the edition line
 * are set in the device's condensed face and shouted in capitals, because the frame is.
 *
 * Nothing is painted behind the page, for [NewspaperFrontPage]'s reason: the strips carry their
 * own paper and their own torn edges.
 */
@Composable
fun NewspaperTabloid(
    masthead: String,
    edition: Newspaper.Edition,
    headline: String,
    modifier: Modifier = Modifier,
    price: String? = null,
    flagUrl: String? = null,
    imageUrls: List<String> = emptyList(),
    imageLoader: ImageLoader? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Captured because the nested Boxes shadow this scope's `maxWidth`.
        val page = maxWidth
        val px: (Float) -> Dp = { page * it }

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
                    .height(px(MASTHEAD_BAND)),
            ) {
                // Why stretched rather than tiled as the frame tiles it: `dpaper2` is flat paper
                // with no torn edge of its own, so a sixth of extra height is invisible stretched
                // and prints a seam across the band repeated.
                Image(
                    painter = painterResource(R.drawable.newspaper_band_masthead),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )

                MountedFlag(flagUrl, imageLoader, page, px(FLAG_L_X), px(FLAG_L_Y), px(FLAG_L_W), px(FLAG_L_H))
                MountedFlag(flagUrl, imageLoader, page, px(FLAG_R_X), px(FLAG_R_Y), px(FLAG_R_W), px(FLAG_R_H))

                Masthead(masthead, page)

                // Last of the band's furniture, and the only thing that leaves it: the flash is
                // pinned across the tear at the top of the sheet.
                price?.let { PriceFlash(it, page) }

                EditionLine(edition, page)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(px(BODY)),
            ) {
                // Over the strip, not under it: this page's photograph is wider than the windows
                // cut in `dpaper5` and covers the paper between them as well, which is only
                // possible from on top.
                Image(
                    painter = painterResource(R.drawable.newspaper_edge_bottom),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )

                // Printed whether or not there is a photograph. It covers both of the strip's
                // windows, so leaving it out for a pictureless issue would open two holes in the
                // page; floored with newsprint and carrying the headline, a blank one reads as a
                // front page whose picture did not arrive.
                NewspaperPhoto(
                    url = imageUrls.getOrNull(0),
                    imageLoader = imageLoader,
                    x = px(PHOTO_X),
                    y = px(PHOTO_Y),
                    width = px(PHOTO_W),
                    height = px(PHOTO_H),
                )
                PhotoEdge(
                    x = px(PHOTO_X),
                    y = px(PHOTO_Y),
                    width = px(PHOTO_W),
                    height = px(PHOTO_H),
                    thickness = px(PHOTO_EDGE),
                    color = TabloidPhotoEdge,
                    // Open at the foot, as the frame draws it: the picture runs off the torn edge.
                    foot = false,
                )

                // The inset is an addition rather than the body, so an issue that offers only one
                // photograph prints one. An empty frame with a shadow under it is not a page.
                imageUrls.getOrNull(1)?.let { inset ->
                    PaperBlock(
                        px(INSET_X + INSET_SHADOW),
                        px(INSET_Y + INSET_SHADOW),
                        px(INSET_W),
                        px(INSET_H),
                        TabloidHeadlineEdge,
                    )
                    NewspaperPhoto(
                        url = inset,
                        imageLoader = imageLoader,
                        x = px(INSET_X),
                        y = px(INSET_Y),
                        width = px(INSET_W),
                        height = px(INSET_H),
                    )
                    PhotoEdge(
                        x = px(INSET_X),
                        y = px(INSET_Y),
                        width = px(INSET_W),
                        height = px(INSET_H),
                        thickness = px(INSET_EDGE),
                        color = NewsprintRule,
                        foot = true,
                    )
                }

                Headline(headline, page)
            }
        }
    }
}

/**
 * The nation's flag, mounted and shadowed, once at each end of the masthead.
 *
 * Fitted rather than cropped, for the reason the rest of the app fits a flag: the game's flags
 * run from 1:1 to 1:2 and a crop that filled this frame would cut the design out of half of them.
 * The mount is what the frame draws, so a flag narrower than it sits inside its border rather
 * than stretching to reach it.
 */
@Composable
private fun MountedFlag(
    url: String?,
    imageLoader: ImageLoader?,
    page: Dp,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
) {
    PaperBlock(x + page * FLAG_SHADOW_X, y + page * FLAG_SHADOW_Y, width, height, TabloidFlagShadow)

    Box(
        Modifier
            .offset(x = x, y = y)
            .size(width = width, height = height)
            .border(page * FLAG_FRAME, TabloidFlagFrame),
    ) {
        if (url != null && imageLoader != null) {
            AsyncImage(
                model = url,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** The plate, its two red bars, and the paper's name knocked out of it. */
@Composable
private fun Masthead(text: String, page: Dp) {
    val px: (Float) -> Dp = { page * it }
    val shout = remember(text) { text.uppercase(Locale.US) }
    val shadow = with(LocalDensity.current) {
        Shadow(TabloidRule, Offset(px(MASTHEAD_SHADOW_X).toPx(), px(MASTHEAD_SHADOW_Y).toPx()), NO_BLUR)
    }

    PaperBlock(px(PLAQUE_X), px(PLAQUE_Y + PLAQUE_SHADOW_Y), px(PLAQUE_W), px(PLAQUE_H), TabloidPlateShadow)

    Box(
        Modifier
            .offset(x = px(PLAQUE_X), y = px(PLAQUE_Y))
            .size(width = px(PLAQUE_W), height = px(PLAQUE_H))
            // The frame's own group opacity. A layer, not a colour, because it has to apply to
            // the plate, its bars and its lettering together — per-element it would let the plate
            // show through the letters.
            .alpha(PLAQUE_OPACITY)
            .background(TabloidPlate),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(px(PLAQUE_BAR))
                .background(TabloidRule),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(px(PLAQUE_BAR))
                .background(TabloidRule),
        )

        FittedText(
            text = shout,
            maxWidth = px(MASTHEAD_W),
            style = TextStyle(
                fontFamily = Condensed,
                fontWeight = FontWeight.Bold,
                fontSize = px(MASTHEAD_SIZE).toSp(),
                textAlign = TextAlign.Center,
                shadow = shadow,
            ),
            color = TabloidInk,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = px(MASTHEAD_Y))
                .width(px(MASTHEAD_W)),
        )
    }
}

/**
 * The cover price, on a tilted flash across the tear.
 *
 * Three rings rather than three boxes: each `background` paints the region the `padding` before
 * it left, which is the frame's red-cream-red edge in one modifier chain.
 */
@Composable
private fun PriceFlash(text: String, page: Dp) {
    val px: (Float) -> Dp = { page * it }
    val pill = RoundedCornerShape(percent = HALF)

    Box(
        Modifier
            .offset(x = px(PRICE_X), y = px(PRICE_Y))
            .size(width = px(PRICE_W), height = px(PRICE_H))
            .rotate(PRICE_TILT)
            .background(TabloidRule, pill)
            .padding(px(PRICE_RING))
            .background(TabloidInk, pill)
            .padding(px(PRICE_BORDER))
            .background(TabloidRule, pill),
        contentAlignment = Alignment.Center,
    ) {
        FittedText(
            text = text,
            maxWidth = px(PRICE_CORE_W),
            style = TextStyle(
                fontFamily = Condensed,
                fontWeight = FontWeight.Bold,
                fontSize = px(PRICE_SIZE).toSp(),
                textAlign = TextAlign.Center,
            ),
            color = TabloidInk,
        )
    }
}

/** The rules, the slate bar under them, and the three cells of the edition line inside it. */
@Composable
private fun EditionLine(edition: Newspaper.Edition, page: Dp) {
    val px: (Float) -> Dp = { page * it }

    PaperBlock(px(MARGIN), px(RULE_HEAVY_Y), px(RULE_W), px(RULE_HEAVY), TabloidRule)
    PaperBlock(px(MARGIN), px(RULE_HAIR_Y), px(RULE_W), px(RULE_HAIR), TabloidRule)
    PaperBlock(px(MARGIN), px(BAR_Y), px(RULE_W), px(BAR_H), TabloidPlate)
    PaperBlock(px(MARGIN), px(BAR_FOOT_Y), px(RULE_W), px(RULE_HAIR), TabloidRule)

    val cell = TextStyle(
        fontFamily = Condensed,
        fontWeight = FontWeight.Bold,
        fontSize = px(EDITION_SIZE).toSp(),
    )
    val y = px(EDITION_Y)
    EditionCell(edition.city, px(EDITION_L_X), y, px(EDITION_L_W), cell, TextAlign.Start, TabloidInk)
    // The frame tracks the date and nothing else, which is what makes it the middle of the line
    // rather than a third thing crowded into it.
    EditionCell(
        edition.date,
        px(EDITION_C_X),
        y,
        px(EDITION_C_W),
        cell.copy(letterSpacing = DATE_TRACKING),
        TextAlign.Center,
        TabloidInk,
    )
    EditionCell(edition.volume, px(EDITION_R_X), y, px(EDITION_R_W), cell, TextAlign.End, TabloidInk)
}

/**
 * The headline, shouted across the photograph.
 *
 * Drawn twice at one measurement: the outline pass carries the frame's dark edge and its offset
 * shadow, the fill pass the cream face. Both the edge and the offset are scaled by however far
 * the string had to shrink, because an outline that stayed the frame's width around type half
 * the frame's size closes the counters up and prints a blob.
 *
 * Two lines where the frame has one. The frame's headline is two words; the game's are sentences,
 * and holding a sentence to one line means shrinking it until it is no longer a headline. The
 * band below has the room — the photograph is what is under it either way.
 */
@Composable
private fun Headline(text: String, page: Dp) {
    val px: (Float) -> Dp = { page * it }
    val shout = remember(text) { text.uppercase(Locale.US) }

    val asDrawn = TextStyle(
        fontFamily = Condensed,
        fontWeight = FontWeight.Bold,
        fontSize = px(HEADLINE_SIZE).toSp(),
        lineHeight = px(HEADLINE_SIZE * HEADLINE_LEADING).toSp(),
    )
    val fitted = rememberFittedStyle(shout, px(HEADLINE_W), asDrawn, HEADLINE_LINES)
    val shrunk = fitted.fontSize.value / asDrawn.fontSize.value

    val edged = with(LocalDensity.current) {
        fitted.copy(
            drawStyle = Stroke(width = px(HEADLINE_OUTLINE * shrunk).toPx(), join = StrokeJoin.Round),
            shadow = Shadow(
                TabloidHeadlineEdge,
                Offset(px(HEADLINE_SHADOW * shrunk).toPx(), px(HEADLINE_SHADOW * shrunk).toPx()),
                NO_BLUR,
            ),
        )
    }
    val place = Modifier
        .offset(x = px(HEADLINE_X), y = px(HEADLINE_Y))
        .width(px(HEADLINE_W))

    Text(shout, style = edged, color = TabloidHeadlineEdge, maxLines = HEADLINE_LINES, modifier = place)
    Text(shout, style = fitted, color = TabloidHeadlineInk, maxLines = HEADLINE_LINES, modifier = place)
}

/** The dark edge printed around a photograph, inside it. */
@Composable
private fun PhotoEdge(
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    thickness: Dp,
    color: Color,
    foot: Boolean,
) {
    PaperBlock(x, y, width, thickness, color)
    PaperBlock(x, y, thickness, height, color)
    PaperBlock(x + width - thickness, y, thickness, height, color)
    if (foot) PaperBlock(x, y + height - thickness, width, thickness, color)
}

/**
 * The frame's face is a bold condensed grotesque this app does not ship and will not: a display
 * font is tens of kilobytes against a §4 row that says the APK may not grow. `sans-serif-condensed`
 * is on the device, it is the nearest thing to the frame's proportions that is, and every string
 * on this page is fitted anyway — a wider face costs a shrink step, not a broken layout.
 */
private val Condensed = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-condensed"), weight = FontWeight.Bold),
)

/**
 * Type is stated as the cap height the frame drew, over the cap height of an em in Roboto —
 * 1456 units of its 2048.
 *
 * Why not the frame's point sizes: they belong to the frame's own face, and two faces set at one
 * point size do not put capitals at one height. What the reader sees is the height of the
 * letters, so that is the figure carried across.
 */
private const val CAP_RATIO = 1456f / 2048f

/** A [Shadow] is a printed offset here, not an elevation, so it has no blur. */
private const val NO_BLUR = 0f

/** A [RoundedCornerShape] percentage that is a pill at whatever size the flash comes out. */
private const val HALF = 50

// Geometry as fractions of the page width, transcribed from `newNewspaper.svg` on its own
// 594-unit sheet. A position inside a band is written as the frame's absolute figure less the
// band's top, so every number below can be found in the frame. Nothing here is a guess.
private const val FRAME = 594f
private const val BAND_TOP = 47.03f
private const val BODY_TOP = 124.76f

private const val TOP_EDGE = BAND_TOP / FRAME
private const val MASTHEAD_BAND = 77.73f / FRAME
private const val BODY = 154.19f / FRAME
private const val MARGIN = 29.69f / FRAME
private const val RULE_W = 510.86f / FRAME

private const val FLAG_L_X = 32.55f / FRAME
private const val FLAG_L_Y = (52.36f - BAND_TOP) / FRAME
private const val FLAG_L_W = 71.48f / FRAME
private const val FLAG_L_H = 41.08f / FRAME
private const val FLAG_R_X = 470.72f / FRAME
private const val FLAG_R_Y = (53.56f - BAND_TOP) / FRAME
private const val FLAG_R_W = 67.2f / FRAME
private const val FLAG_R_H = 38.67f / FRAME
private const val FLAG_FRAME = 1f / FRAME
private const val FLAG_SHADOW_X = 3f / FRAME
private const val FLAG_SHADOW_Y = 4f / FRAME

private const val PLAQUE_X = 106.91f / FRAME
private const val PLAQUE_Y = (55.79f - BAND_TOP) / FRAME
private const val PLAQUE_W = 361.19f / FRAME
private const val PLAQUE_H = 34.22f / FRAME
private const val PLAQUE_BAR = 4f / FRAME
private const val PLAQUE_SHADOW_Y = 3f / FRAME
private const val PLAQUE_OPACITY = 0.9f

/** Inside the plate, not the band: the name is a child of the plate it is knocked out of. */
private const val MASTHEAD_Y = (60.4f - 55.79f) / FRAME

/**
 * The plate, inset by the same 4.61 units the frame insets the name from its top.
 *
 * Why not the frame's own 219.88: that box hugs one string set in the frame's own face, and a
 * hug is a measurement, not a margin. Held to it, a face a third wider than the frame's — which
 * is every face on the device — starts shrinking a masthead that has most of the plate free.
 */
private const val MASTHEAD_W = PLAQUE_W - 2f * MASTHEAD_Y
private const val MASTHEAD_SHADOW_X = 3f / FRAME
private const val MASTHEAD_SHADOW_Y = 2f / FRAME
private const val MASTHEAD_SIZE = 15.97f / CAP_RATIO / FRAME

// The flash is the frame's 29.55x17.53 pill plus the 1-unit ring its dilated shadow prints
// around it, and it is placed by that whole shape's centre because it is the shape that turns.
private const val PRICE_W = 31.56f / FRAME
private const val PRICE_H = 19.53f / FRAME
private const val PRICE_X = 426.73f / FRAME
private const val PRICE_Y = (42.59f - BAND_TOP) / FRAME
private const val PRICE_RING = 1f / FRAME
private const val PRICE_BORDER = 2f / FRAME
private const val PRICE_CORE_W = PRICE_W - 2f * (PRICE_RING + PRICE_BORDER)
private const val PRICE_TILT = 4f
private const val PRICE_SIZE = 5f / CAP_RATIO / FRAME

private const val RULE_HEAVY_Y = (102.76f - BAND_TOP) / FRAME
private const val RULE_HEAVY = 6f / FRAME
private const val RULE_HAIR_Y = (109.76f - BAND_TOP) / FRAME
private const val RULE_HAIR = 1f / FRAME
private const val BAR_Y = (110.76f - BAND_TOP) / FRAME
private const val BAR_H = 13f / FRAME
private const val BAR_FOOT_Y = (123.76f - BAND_TOP) / FRAME

private const val EDITION_Y = (112.76f - BAND_TOP) / FRAME
private const val EDITION_L_X = 32.43f / FRAME
private const val EDITION_L_W = 78.59f / FRAME
private const val EDITION_C_X = 118.92f / FRAME
private const val EDITION_C_W = 343.36f / FRAME
private const val EDITION_R_X = 470.18f / FRAME
private const val EDITION_R_W = 67.63f / FRAME
private const val EDITION_SIZE = 5.9f / CAP_RATIO / FRAME

/** Measured off the frame's own date, which is set 1.7 units of its 8.3-unit type wider. */
private val DATE_TRACKING = 0.2f.em

private const val PHOTO_X = 29.69f / FRAME
private const val PHOTO_Y = (125.95f - BODY_TOP) / FRAME
private const val PHOTO_W = 518.61f / FRAME
private const val PHOTO_H = 131.97f / FRAME
private const val PHOTO_EDGE = 3f / FRAME

private const val INSET_X = 434.61f / FRAME
private const val INSET_Y = (135.22f - BODY_TOP) / FRAME
private const val INSET_W = 97.03f / FRAME
private const val INSET_H = 104.39f / FRAME
private const val INSET_EDGE = 1f / FRAME
private const val INSET_SHADOW = 4f / FRAME

private const val HEADLINE_X = 41.58f / FRAME
private const val HEADLINE_Y = (133.76f - BODY_TOP) / FRAME
private const val HEADLINE_W = 386.09f / FRAME
private const val HEADLINE_OUTLINE = 2f / FRAME
private const val HEADLINE_SHADOW = 3.8016f / FRAME
private const val HEADLINE_SIZE = 37.55f / CAP_RATIO / FRAME
private const val HEADLINE_LINES = 2

/** The frame's line box is 50 units where its cap height asks for nearly 53: a tight headline. */
private const val HEADLINE_LEADING = 50f / (37.55f / CAP_RATIO)
