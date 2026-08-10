package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.Percent
import dev.cazh0.civily.ui.theme.Dimens

/** One slice of something that adds up to a hundred per cent. */
data class Share(val label: String, val percent: Double)

/**
 * Shares of a whole, as a ranked list of bars.
 *
 * Why bars and not a pie: three of these appear on this screen — where a government spends, what
 * the economy is made of, what people die of — and every one of them is really a question about
 * *order*. A pie makes the reader compare angles and hides the labels in a legend; a sorted bar
 * list answers "what does this government care about" in the first line.
 *
 * Why the fill is the share of a hundred rather than of the largest bar: these are shares of one
 * whole, so a sector holding 92% has to look like it holds nearly all of it. Normalising to the
 * biggest bar would draw every chart the same shape and say nothing.
 *
 * Why not `LinearProgressIndicator`: nothing here is progress, and TalkBack would announce it as
 * such. The percentage beside the label is what a screen reader reads.
 */
@Composable
fun ShareBars(shares: List<Share>, modifier: Modifier = Modifier) {
    if (shares.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        shares.forEach { share ->
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    Text(
                        text = share.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            R.string.value_percent,
                            Percent.rounded(share.percent),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Bar(share.percent)
            }
        }
    }
}

@Composable
private fun Bar(percent: Double) {
    val shape = RoundedCornerShape(Dimens.BarHeight)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.BarHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        // Why coerced: the API's own percentages sum to a hair over a hundred on some nations,
        // and a fraction above 1 makes `fillMaxWidth` throw rather than simply fill.
        val fraction = (percent / WHOLE).coerceIn(0.0, 1.0).toFloat()
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private const val WHOLE = 100.0
