package dev.cazh0.stately.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.cazh0.stately.core.text.Percent
import dev.cazh0.stately.ui.theme.Dimens
import dev.cazh0.stately.ui.theme.TrendColors

/**
 * One moved statistic, in the shape the game's own Recent Trends strip uses.
 *
 * The site puts a rendered badge sprite beside each scale. There are ninety of those and no
 * documented way to address them, so the badge becomes a rounded tile carrying the scale's
 * initial — same silhouette, same anchor for the eye, nothing to download and nothing to go
 * stale when NationStates adds a scale.
 *
 * The name takes the direction's colour and the unit stays quiet beneath it, exactly as on
 * the dashboard: the colour is what you read at a glance, the unit is what you read when you
 * care.
 */
@Composable
fun TrendPill(
    name: String,
    unit: String,
    percentChange: Double,
    modifier: Modifier = Modifier,
) {
    val movement = Percent.change(percentChange)
    val tint = when (movement.direction) {
        Percent.Direction.Up -> TrendColors.up
        Percent.Direction.Down -> TrendColors.down
        // A movement of nothing is not an event; it takes the theme's quiet colour.
        Percent.Direction.Flat -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val arrow = when (movement.direction) {
        Percent.Direction.Up -> "↑"
        Percent.Direction.Down -> "↓"
        Percent.Direction.Flat -> "→"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Badge(initial = name.firstOrNull()?.uppercase().orEmpty(), tint = tint)

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
            ) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "$arrow${movement.text.removePrefix("+").removePrefix("−")}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The badge, standing in for the site's sprite: a rounded tile with the scale's initial. */
@Composable
private fun Badge(initial: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(Dimens.TrendBadgeRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(Dimens.HairlineBorder, tint),
        modifier = Modifier.size(Dimens.TrendBadgeSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tint,
                modifier = Modifier.padding(Dimens.TextSpacing / 2),
            )
        }
    }
}
