package dev.cazh0.stately.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.stately.ui.theme.Dimens
import dev.cazh0.stately.ui.theme.FlagPlate

/**
 * A flag at the top of a detail screen.
 *
 * Why it sits on a filled surface rather than the page background: NationStates flags are
 * frequently transparent PNGs and SVGs, and a white-on-transparent flag is invisible against
 * a dark theme. A neutral plate behind it means every flag in the game renders legibly in
 * both themes without special-casing any of them.
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
        color = FlagPlate,
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
