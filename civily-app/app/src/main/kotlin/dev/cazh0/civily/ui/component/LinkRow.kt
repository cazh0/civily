package dev.cazh0.civily.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.ui.theme.Dimens

/**
 * A tappable row: optional image, title, one line of context, chevron.
 *
 * The chevron is not decoration — it is the difference between a card the user reads and a
 * card the user knows to press.
 */
@Composable
fun LinkRow(
    name: String,
    subtitle: String,
    flagUrl: String,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.TouchTarget + Dimens.CardPadding)
                .padding(horizontal = Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            if (flagUrl.isNotEmpty()) {
                AsyncImage(
                    model = flagUrl,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    // Why cropped and not fitted: a fitted flag leaves bars of empty plate down
                    // its sides, which is what made every row look like a picture floating in a
                    // box. Filled, the artwork *is* the tile. A chip's job is recognition, and
                    // the whole flag is one tap away on the nation's own screen.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(Dimens.FlagThumbnailWidth, Dimens.FlagThumbnailHeight)
                        .clip(RoundedCornerShape(Dimens.FlagCornerRadius)),
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(Dimens.ItemSpacing * 2),
            )
        }
    }
}
