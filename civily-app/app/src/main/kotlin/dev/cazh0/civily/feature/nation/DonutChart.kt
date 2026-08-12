package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.Percent
import dev.cazh0.civily.ui.theme.Dimens

/** One wedge of something that adds up to a hundred per cent. */
data class Slice(val label: String, val percent: Double, val color: Color)

/**
 * Shares of a whole, drawn the way the game draws them.
 *
 * This is the desktop site's chart, which the legacy client already mirrors: a ring with a hole
 * six tenths of the radius, a fainter band of the same slices just inside it, no labels on the
 * wedges, and a wrapped legend of colour dots underneath. Those proportions are not invented —
 * they are the ones the legacy client sets (`holeRadius = 60`, `transparentCircleRadius = 65`),
 * and matching them is what makes a player who knows the game recognise this screen.
 *
 * Drawn on a `Canvas` rather than with a charting library: the whole shape is three arcs and two
 * circles, and a dependency for that would buy nothing while costing every build (spec §3).
 *
 * One departure, and it adds rather than changes: the legend carries each slice's percentage. The
 * game hides those behind tapping a wedge, which leaves a chart whose numbers cannot be read at
 * all — and the numbers are the reason the chart is here.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutChart(slices: List<Slice>, modifier: Modifier = Modifier) {
    val present = slices.filter { it.percent > 0 }
    if (present.isEmpty()) return

    // The hole is not a colour of its own — it is the surface showing through, which is what
    // makes the ring read as cut out of the page rather than drawn on a white plate.
    val hole = MaterialTheme.colorScheme.background
    val total = present.sumOf { it.percent }.toFloat()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .sizeIn(maxWidth = Dimens.DonutDiameter, maxHeight = Dimens.DonutDiameter)
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            val diameter = size.minDimension
            val radius = diameter / 2f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val square = Size(diameter, diameter)

            // Starting at twelve o'clock and going clockwise, as the game's own chart does.
            var start = START_ANGLE
            present.forEach { slice ->
                val sweep = (slice.percent.toFloat() / total) * FULL_TURN
                drawArc(
                    color = slice.color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = square,
                )
                start += sweep
            }

            // The faint band: the hole colour laid over the wedges at low opacity, so the slice
            // colours show through it. This is the ring visible just inside the solid one.
            drawCircle(
                color = hole.copy(alpha = FAINT_BAND_ALPHA),
                radius = radius * FAINT_BAND_FRACTION,
            )
            drawCircle(color = hole, radius = radius * HOLE_FRACTION)
        }

        // Wrapped and centred, like the game's: labels are as long as a cause of death, and a
        // fixed number of columns would either truncate them or waste half the row.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            present.forEach { slice -> LegendEntry(slice) }
        }
    }
}

@Composable
private fun LegendEntry(slice: Slice) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.LegendDotSize)
                .clip(CircleShape)
                .background(slice.color),
        )
        Text(
            text = slice.label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.value_percent, Percent.rounded(slice.percent)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Twelve o'clock. Compose measures arc angles from three o'clock, so this is a quarter back. */
private const val START_ANGLE = -90f
private const val FULL_TURN = 360f

/** The legacy client's `holeRadius = 60` and `transparentCircleRadius = 65`, as fractions. */
private const val HOLE_FRACTION = 0.60f
private const val FAINT_BAND_FRACTION = 0.65f

/** The legacy client's default transparent-circle alpha, 100 of 255. */
private const val FAINT_BAND_ALPHA = 0.392f
