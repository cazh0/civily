package dev.cazh0.civily.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.ImageLoader
import dev.cazh0.civily.ui.theme.Dimens

/**
 * A flag at the top of a detail screen, whole and uncropped.
 *
 * This is the one place a flag is looked *at* rather than recognised, so it is fitted rather
 * than cropped to fill: a chip may lose its edges, a flag on its own screen may not. Fitting
 * leaves plate showing beside it, which is exactly where [AmbientFlag] earns its keep — the
 * bars either side are the flag's own colours rather than a slab of grey.
 */
@Composable
fun FlagHero(
    flagUrl: String,
    contentDescription: String,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    if (flagUrl.isEmpty()) return

    AmbientFlag(
        flagUrl = flagUrl,
        contentDescription = contentDescription,
        imageLoader = imageLoader,
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        contentScale = ContentScale.Fit,
        contentPadding = Dimens.CardPadding,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.FlagHeight + Dimens.CardPadding * 2),
    )
}
