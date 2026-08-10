package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.cazh0.civily.R
import dev.cazh0.civily.data.nation.Ranking
import dev.cazh0.civily.ui.component.Pill
import dev.cazh0.civily.ui.theme.Dimens

/**
 * Where the nation places on every World Census scale.
 *
 * Ninety rows, in the game's own scale order rather than sorted by rank: the reader is usually
 * looking for one particular scale, and a list that reorders itself per nation is a list you
 * have to read rather than scan. The percentile is the pill because "top 3%" is the number that
 * means something without knowing how many nations exist.
 *
 * Lazy because it is ninety rows. Nothing else on this screen is long enough to need it.
 */
@Composable
fun NationRankingsPane(rankings: List<Ranking>, modifier: Modifier = Modifier) {
    if (rankings.isEmpty()) {
        Text(
            text = stringResource(R.string.rankings_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(Dimens.ScreenPadding),
        )
        return
    }

    val scaleNames = stringArrayResource(R.array.census_scales)
    val scaleUnits = stringArrayResource(R.array.census_units)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
    ) {
        items(items = rankings, key = { it.scaleId }) { ranking ->
            RankingRow(
                // A scale past the end of the list is named for its id rather than hidden. The
                // list came from the legacy app and has gaps of its own; the honest failure is
                // "Scale 91", not a missing row.
                name = scaleNames.getOrNull(ranking.scaleId)
                    ?: stringResource(R.string.census_unknown_scale, ranking.scaleId),
                unit = scaleUnits.getOrNull(ranking.scaleId).orEmpty(),
                ranking = ranking,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
        }
    }
}

@Composable
private fun RankingRow(name: String, unit: String, ranking: Ranking) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.ItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        // The unit sits under the name rather than under the score, because the game's units are
        // jokes at full length — "Krugman-Greenspan Business Outlook Index" — and the left column
        // is the only one with room for one. That leaves the right column purely numeric.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            Text(
                text = scoreWords(ranking.score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            Text(
                text = rankWords(ranking.worldRank),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }

        Pill(text = percentileWords(ranking.percentile))
    }
}
