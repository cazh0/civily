package dev.cazh0.civily.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import coil.ImageLoader
import dev.cazh0.civily.core.text.Newspaper
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

/**
 * A pile of front pages, one per headline, transcribed from `newspaperStack.svg`.
 *
 * Everything is a fraction of the frame's 518.45-unit canvas: the sheet is 480 of those wide,
 * which is what leaves room for each one to sit at a different offset. The offsets and the
 * angles — +0.7°, −1.4°, +2.1°, −2.8° — are the frame's own, not a pattern invented to look
 * like it, and a sheet is turned about its top left corner because that is the corner the frame
 * turns it about.
 *
 * Sheets are drawn first to last so each lands on top of the one before, which is what makes
 * the pile read as a pile: every sheet shows its masthead and its headline, and the paper
 * below it is buried from there down. Where each one starts is [topsFor]'s answer rather than
 * the frame's, for the reason given there.
 *
 * The pile prints the game's papers in the game's own order, continuing from the paper this
 * issue was itself printed on, so its top sheet is the front page the reader has just come from
 * and the rest are the papers that would follow it. That is [Newspaper.design]'s rule applied
 * one step at a time — no second rule about which paper a headline lands on.
 */
@Composable
fun NewspaperStack(
    issueId: Int,
    headlines: List<String>,
    masthead: String,
    edition: Newspaper.Edition,
    modifier: Modifier = Modifier,
    price: String? = null,
    flagUrl: String? = null,
    imageUrlsByHeadline: List<List<String>> = emptyList(),
    imageLoader: ImageLoader? = null,
) {
    if (headlines.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val canvas = maxWidth
        val sheet = canvas * SHEET_WIDTH
        val extents = headlines.mapIndexed { index, headline ->
            stackedExtent(Newspaper.design(issueId + index), headline, sheet)
        }
        val tops = remember(extents) { topsFor(extents) }

        Box(
            Modifier
                .fillMaxWidth()
                // Down to the foot of the lowest sheet, its turned corner included. The frame
                // stops short of that, which cuts the bottom paper through its photographs —
                // and those are the only photographs the pile ever shows, every sheet above it
                // being covered from the headline down by the next one.
                .height(canvas * remember(extents, tops) { pileFoot(extents, tops) })
                .clipToBounds(),
        ) {
            headlines.forEachIndexed { index, headline ->
                NewspaperForIssue(
                    issueId = issueId + index,
                    masthead = masthead,
                    edition = edition,
                    headline = headline,
                    style = NewspaperStyle.Stacked,
                    price = price,
                    flagUrl = flagUrl,
                    imageUrls = imageUrlsByHeadline.getOrNull(index).orEmpty(),
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .offset(x = canvas * leftOf(index), y = canvas * tops[index])
                        .width(sheet)
                        .graphicsLayer {
                            rotationZ = angleOf(index)
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                )
            }
        }
    }
}

/**
 * Where each sheet starts, as a fraction of the canvas width.
 *
 * Why computed rather than transcribed: the frame places its four sheets by hand, and its own
 * spacing lands the next sheet across the foot of the one above — the last third of the
 * broadsheet's headline at one end of the pile, a whole line of the tabloid's at the other. A
 * pile whose point is that every sheet shows its words cannot be laid out by numbers that cover
 * them, so each sheet is set down where the sheet above stops printing.
 *
 * Two straight lines decide that, and nothing else: the foot of the headline box above, and the
 * torn top edge below. Both are turned, so both are sloped, and a sloped line clears a sloped
 * line everywhere along a span if it clears it at the two ends — which is why only the box's own
 * two bottom corners are asked. Taking the whole sheet's width instead of the box's would set
 * the next paper by a corner with no word anywhere near it and open a hand's width of blank.
 */
private fun topsFor(extents: List<SheetExtent>): List<Float> {
    val tops = ArrayList<Float>(extents.size)
    var top = FIRST_TOP
    extents.forEachIndexed { index, extent ->
        tops += top
        if (index < extents.lastIndex) top = clearing(top, extent, index)
    }
    return tops
}

/** Where the sheet after [index] has to start for its top edge to pass under these words. */
private fun clearing(top: Float, above: SheetExtent, index: Int): Float {
    val turn = radians(angleOf(index))
    val next = radians(angleOf(index + 1))
    val here = leftOf(index)
    val there = leftOf(index + 1)

    return above.lines.flatMap { line ->
        val foot = SHEET_WIDTH * line.foot
        listOf(line.left, line.right).map { side ->
            val edge = SHEET_WIDTH * side
            val x = here + edge * cos(turn) - foot * sin(turn)
            val y = top + edge * sin(turn) + foot * cos(turn)
            y - (x - there) * tan(next)
        }
    }.max()
}

/** The lowest point any sheet's turned foot reaches, which is how tall the pile has to be. */
private fun pileFoot(extents: List<SheetExtent>, tops: List<Float>): Float =
    extents.withIndex().maxOf { (index, extent) ->
        val turn = radians(angleOf(index))
        tops[index] + SHEET_WIDTH * (extent.height * cos(turn) + max(sin(turn), 0f))
    }

/**
 * The frame's four offsets and four angles, repeated for longer piles.
 *
 * The API has never returned more than four headlines, and a repeat is the one continuation
 * that cannot walk a fifth sheet off the side of the canvas the way a growing angle would.
 */
private fun leftOf(index: Int): Float = LEFTS[index % LEFTS.size]

private fun angleOf(index: Int): Float = ANGLES[index % ANGLES.size]

private fun radians(degrees: Float): Float = Math.toRadians(degrees.toDouble()).toFloat()

private const val CANVAS = 518.45f
private const val SHEET_WIDTH = 480f / CANVAS

/** The frame's top sheet starts at the top of the canvas, and its angle only takes it down. */
private const val FIRST_TOP = 0f

private val LEFTS = listOf(38.4883f / CANVAS, 8.87891f / CANVAS, 33.5f / CANVAS, 0f)
private val ANGLES = listOf(0.7f, -1.4f, 2.1f, -2.8f)
