package dev.cazh0.civily.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.cazh0.civily.ui.theme.Dimens

/**
 * One labelled fact. The unit every detail screen is built from.
 *
 * Renders nothing when [value] is empty, because the API omits shards it has no data for and
 * a card reading "Delegate:" with nothing under it is worse than no card.
 *
 * Why `Modifier.clickable` rather than the `OutlinedCard(onClick = …)` overload: that overload
 * has moved in and out of experimental across Material 3 releases. This does not.
 */
@Composable
fun FactCard(
    @StringRes labelRes: Int,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (value.isEmpty()) return

    OutlinedCard(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
