package dev.cazh0.civily.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import coil.ImageLoader
import dev.cazh0.civily.core.text.Newspaper
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A pile of front pages, one per headline, transcribed from `recentHeadlines.svg`.
 *
 * Everything is a fraction of the frame's 517.86-unit canvas: the sheet is 480 of those wide,
 * which is what leaves room for each one to sit at a different offset. The placements and the
 * angles — −0.7°, +1.4°, −2.1°, +2.8° — are the frame's own, not a pattern invented to look
 * like it.
 *
 * Sheets are drawn first to last so each lands on top of the one before, which is what makes
 * the pile read as a pile: every sheet shows its masthead and its headline, and the paper
 * below it is buried from there down.
 */
@Composable
fun NewspaperStack(
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
        val last = headlines.lastIndex
        val sheet = SHEET_WIDTH * NewspaperStyle.Stacked.aspect

        Box(
            Modifier
                .fillMaxWidth()
                // Down to the foot of the last sheet, its rotated corner included. The frame
                // stops 65 units short of that, which cuts the bottom paper through its
                // photographs — and those are the only photographs the pile ever shows, every
                // sheet above it being covered from the headline down by the next one.
                .height(canvas * (topOf(last) + sheet + cornerDrop(last, sheet)))
                .clipToBounds(),
        ) {
            headlines.forEachIndexed { index, headline ->
                NewspaperFrontPage(
                    masthead = masthead,
                    edition = edition,
                    headline = headline,
                    style = NewspaperStyle.Stacked,
                    price = price,
                    flagUrl = flagUrl,
                    imageUrls = imageUrlsByHeadline.getOrNull(index).orEmpty(),
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .offset(x = canvas * leftOf(index), y = canvas * topOf(index))
                        .width(canvas * SHEET_WIDTH)
                        .rotate(angleOf(index)),
                )
            }
        }
    }
}

/**
 * The frame places its four sheets by hand, at uneven steps. Those exact positions are used
 * for the first four; past that the pile continues at the frame's average step, because the
 * API has never returned more than four headlines and a rhythm is better than a repeat.
 */
private fun topOf(index: Int): Float =
    TOPS.getOrNull(index) ?: (TOPS.last() + AVERAGE_STEP * (index - TOPS.lastIndex))

private fun leftOf(index: Int): Float = LEFTS[index % LEFTS.size]

/** −0.7°, +1.4°, −2.1°, +2.8°, … continued for longer lists. */
private fun angleOf(index: Int): Float =
    ANGLE_STEP * (index + 1) * if (index % 2 == 0) -1f else 1f

/**
 * How far a sheet's lowest corner falls past the foot of its own box once it is turned.
 *
 * `rotate` turns what is drawn, not what is laid out, so the box the pile is measured against
 * knows nothing about the angle. Without this the clip takes the corner off the last paper.
 */
private fun cornerDrop(index: Int, sheetHeight: Float): Float {
    val radians = Math.toRadians(abs(angleOf(index)).toDouble())
    return (SHEET_WIDTH / 2f) * sin(radians).toFloat() -
        (sheetHeight / 2f) * (1f - cos(radians).toFloat())
}

private const val CANVAS = 517.86f
private const val SHEET_WIDTH = 480f / CANVAS

private val TOPS = listOf(5.86f / CANVAS, 134.89f / CANVAS, 287.37f / CANVAS, 404.67f / CANVAS)
private val LEFTS = listOf(13.11f / CANVAS, 38f / CANVAS, 0f, 29.33f / CANVAS)
private const val AVERAGE_STEP = ((404.67f - 5.86f) / 3f) / CANVAS

private const val ANGLE_STEP = 0.7f
