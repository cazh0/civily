package dev.cazh0.civily.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.data.nation.Policy
import dev.cazh0.civily.ui.theme.Dimens

/**
 * One policy, as the nation's own list shows it and as an issue result reports it.
 *
 * @param banner false for a policy the nation no longer has. A cancelled policy keeps its name
 *   and its sentence but loses its artwork, because the banner is the thing the nation used to
 *   fly — printing it under "Canceled Policies" would make a repeal look like an enactment at
 *   the exact glance where the two must not be confused.
 */
@Composable
fun PolicyCard(
    policy: Policy,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    banner: Boolean = true,
) {
    Surface(
        shape = RoundedCornerShape(Dimens.TileCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            if (banner && policy.imageId.isNotEmpty()) {
                // Cropped to fill, like every other piece of artwork in the app: a Rift banner
                // is wide and decorative, and fitting one leaves plate above and below it. No
                // content description — the policy's name is directly beneath it.
                AsyncImage(
                    model = NsUrl.banner(policy.imageId),
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.PolicyBannerHeight),
                )
            }
            Column(
                modifier = Modifier.padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
            ) {
                Text(text = policy.name, style = MaterialTheme.typography.titleMedium)
                if (policy.description.isNotEmpty()) {
                    Text(
                        text = policy.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
