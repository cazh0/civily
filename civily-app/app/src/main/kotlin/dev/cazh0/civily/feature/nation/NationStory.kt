package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import dev.cazh0.civily.R
import dev.cazh0.civily.data.nation.Happening
import dev.cazh0.civily.ui.component.RichText
import dev.cazh0.civily.ui.theme.Dimens

/**
 * The nation's own feed, newest first.
 *
 * Why no cards and no dividers: fifteen outlined boxes give fifteen lines identical weight and
 * turn a chronology into a spreadsheet — the same reason the message board dropped them. A
 * timestamp, a dot to anchor it, and space is the whole chrome.
 *
 * @param now the instant every "3 hours ago" is measured from, passed in so the whole list
 *   agrees and so nothing here reads the clock during composition.
 */
@Composable
fun NationHappeningsPane(
    happenings: List<Happening>,
    now: Long,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (happenings.isEmpty()) {
        // A nation founded minutes ago genuinely has no history. That is an answer, not a
        // failure, so it is a quiet line rather than anything dressed in red.
        Text(
            text = stringResource(R.string.nation_happenings_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.HappeningSpacing),
    ) {
        happenings.forEach { happening ->
            HappeningRow(happening, now, onOpenNation, onOpenRegion)
        }
    }
}

@Composable
private fun HappeningRow(
    happening: Happening,
    now: Long,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.TimelineDotSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = elapsedWords(happening.atEpochSeconds, now),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RichText(
            blocks = happening.text,
            onOpenNation = onOpenNation,
            onOpenRegion = onOpenRegion,
            // Lines up under the timestamp rather than under its dot, so the column of text
            // reads as one edge.
            modifier = Modifier.padding(start = Dimens.TimelineDotSize + Dimens.GridSpacing),
        )
    }
}
