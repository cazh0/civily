package dev.cazh0.stately.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.cazh0.stately.ui.theme.Dimens

/**
 * A small label: a like count, an issue number, where a post came from.
 *
 * Why one component rather than a styled `Text` at each site: these are the only things on a
 * dense screen that are allowed to interrupt the reading line, so they have to look identical
 * everywhere or they read as several different kinds of thing.
 */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        shape = RoundedCornerShape(percent = ROUNDED_FULLY),
        color = container,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Dimens.PillPaddingHorizontal,
                vertical = Dimens.PillPaddingVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(Dimens.PillIconSize),
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

private const val ROUNDED_FULLY = 50
