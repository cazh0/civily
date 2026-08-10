package dev.cazh0.stately.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import dev.cazh0.stately.ui.theme.AvatarPalette
import dev.cazh0.stately.ui.theme.Dimens
import dev.cazh0.stately.core.text.Initials
import kotlin.math.absoluteValue

/**
 * A nation's initials on a colour derived from its id.
 *
 * Why not the nation's flag: the flag would cost one request per author, and a board of fifty
 * posts would spend the entire rate-limit budget drawing thumbnails. Initials on a stable
 * colour give the same thing an avatar is actually for — recognising the same person twice
 * without reading — for nothing.
 *
 * The colour comes from the id, not the display name, so it survives the name being rendered
 * differently anywhere.
 */
@Composable
fun NationAvatar(
    nationId: String,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.AvatarSize,
) {
    Surface(
        shape = CircleShape,
        color = colorFor(nationId),
        // The name is already beside it; announcing it twice is noise to a screen reader.
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = Initials.of(displayName),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

private fun colorFor(nationId: String): Color =
    AvatarPalette[(nationId.hashCode().absoluteValue) % AvatarPalette.size]
