package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
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
            modifier = modifier.padding(Dimens.ScreenPadding),
        )
        return
    }

    NationPane(modifier) {
        // Every line here is parsed markup rendered as an annotated string, which is the most
        // expensive thing on this screen per row. Lazily, the tab costs the handful of lines the
        // reader can see; eagerly it cost all fifteen before the first one appeared.
        //
        // The feed is fixed once loaded — nothing is inserted, removed or reordered — so the
        // index is a stable identity and no key is needed to keep the list honest.
        items(items = happenings, contentType = { PaneContent.Happening }) { happening ->
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
    // The gap under the line rather than an arrangement on the list, for the same reason the
    // panes' section gaps are carried by their sections: it belongs to the thing it separates.
    Column(
        modifier = Modifier.padding(bottom = Dimens.HappeningSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
    ) {
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
