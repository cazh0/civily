package dev.cazh0.civily.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.ui.theme.Dimens

/**
 * A flag at the top of a detail screen, whole and uncropped.
 *
 * The plate behind it follows the colour scheme rather than being a fixed light grey. A pale
 * slab is the loudest thing on a dark screen, and it was showing on every flag in the app to
 * protect the minority that are transparent PNGs with dark artwork. Those now sit on a dark
 * plate — see the known gaps in the README; the rest of the app stops looking like it has a
 * hole punched in it.
 *
 * This is the one place the flag is looked *at* rather than recognised, so it is fitted whole
 * rather than cropped to fill: a chip may lose its edges, a flag on its own screen may not.
 */
@Composable
fun FlagHero(
    flagUrl: String,
    contentDescription: String,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    if (flagUrl.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth(),
    ) {
        AsyncImage(
            model = flagUrl,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.FlagHeight)
                .padding(Dimens.CardPadding),
        )
    }
}
