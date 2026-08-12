package dev.cazh0.civily.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.ui.theme.Dimens
import dev.cazh0.civily.ui.theme.Motion

/**
 * A tappable row: an optional flag, a title, one line of context, and one mark at its end.
 *
 * The chevron is not decoration — it is the difference between a card the user reads and a
 * card the user knows to press.
 *
 * @param badgeCount things waiting behind this row. Zero draws nothing at all: a badge is a
 *   claim that there is something to deal with, and an empty one makes that claim every time
 *   the reader looks at the screen. Above zero it takes the chevron's place rather than sitting
 *   beside it — two marks at one end of a row is two things to read where the row has one thing
 *   to say, and a count already tells you there is somewhere to go.
 */
@Composable
fun LinkRow(
    name: String,
    subtitle: String,
    flagUrl: String,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
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

            RowMark(count = badgeCount)
        }
    }
}

/**
 * The one mark at the end of a row: a count if there is one, otherwise the chevron.
 *
 * They occupy the same slot and cross over inside it, so the badge arrives exactly where the
 * arrow was rather than shouldering it aside — no gap opens between them because they are never
 * both there, and nothing in the row moves as the count comes and goes.
 *
 * Why the slot is a touch target wide when neither of its occupants is one: the caret on the
 * account card above is an `IconButton`, which centres its icon 24dp inside its own edge. Given
 * the same slot both of these land on that same vertical line, so a stack of cards has one
 * trailing edge instead of two that nearly agree.
 */
@Composable
private fun RowMark(count: Int) {
    Box(
        modifier = Modifier.width(Dimens.TouchTarget),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = count == 0,
            enter = fadeIn(tween(Motion.BadgeRollMillis)),
            exit = fadeOut(tween(Motion.BadgeRollMillis)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        CountBadge(count = count)
    }
}

/**
 * The count itself.
 *
 * Primary rather than error, which is what Material's own `Badge` would have used: red in this
 * app means something is wrong — a failed request, a statistic falling — and an issue waiting
 * is the thing the player opened the app for. Colouring good news as damage is the mistake the
 * freedom ratings already caught the legacy client making.
 *
 * The digits roll in the direction the count moved: up when the game sends an issue, down when
 * the reader has just answered one and come back. It is a small thing and it is the difference
 * between a number that changed and a number you *watched* change.
 */
@Composable
private fun CountBadge(count: Int) {
    // Why the last real count is kept: reaching zero scales the badge away rather than cutting
    // it, and a digit that flips to "0" on its way out shows the reader a number that was never
    // true. Written before it is read in the same composition, so it costs no extra pass.
    val lastShown = remember { mutableIntStateOf(count) }
    if (count > 0) lastShown.intValue = count

    AnimatedVisibility(
        visible = count > 0,
        enter = scaleIn(Motion.BadgeEnter) + fadeIn(tween(Motion.BadgeRollMillis)),
        exit = scaleOut(Motion.BadgeEnter) + fadeOut(tween(Motion.BadgeRollMillis)),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .defaultMinSize(minWidth = Dimens.BadgeSize, minHeight = Dimens.BadgeSize)
                // Silent to a screen reader, because the subtitle beside it already says the
                // same thing in words: "Issues, three issues awaiting your answer, three" is
                // the badge read twice.
                .clearAndSetSemantics {},
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = lastShown.intValue,
                    transitionSpec = {
                        val rolledUp = targetState > initialState
                        val height = { full: Int -> if (rolledUp) full else -full }

                        (slideInVertically(tween(Motion.BadgeRollMillis), height) +
                            fadeIn(tween(Motion.BadgeRollMillis))) togetherWith
                            (slideOutVertically(tween(Motion.BadgeRollMillis)) { -height(it) } +
                                fadeOut(tween(Motion.BadgeRollMillis)))
                    },
                    label = "row count",
                ) { shown ->
                    Text(
                        text = shown.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(
                            horizontal = Dimens.BadgePaddingHorizontal,
                        ),
                    )
                }
            }
        }
    }
}
