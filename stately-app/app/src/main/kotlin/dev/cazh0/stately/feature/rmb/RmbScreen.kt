package dev.cazh0.stately.feature.rmb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cazh0.stately.R
import dev.cazh0.stately.core.graph
import dev.cazh0.stately.core.text.NsId
import dev.cazh0.stately.core.text.Numbers
import dev.cazh0.stately.core.text.RelativeTime
import dev.cazh0.stately.data.rmb.RmbPost
import dev.cazh0.stately.ui.component.EmptyState
import dev.cazh0.stately.ui.component.LoadStateScaffold
import dev.cazh0.stately.ui.component.NationAvatar
import dev.cazh0.stately.ui.component.Pill
import dev.cazh0.stately.ui.component.RichText
import dev.cazh0.stately.ui.theme.Dimens

@Composable
fun RmbScreen(
    regionId: String,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = LocalContext.current.graph
    val viewModel: RmbViewModel = viewModel(
        key = "rmb-$regionId",
        factory = RmbViewModel.factory(graph.rmbRepository, regionId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // One clock for the whole list. Reading it per post would make "9h" and "9h" disagree
    // halfway down the screen.
    val now = remember(state) { System.currentTimeMillis() }

    LoadStateScaffold(
        title = stringResource(R.string.title_rmb, NsId.toName(regionId)),
        state = state,
        onRetry = viewModel::refresh,
        onSignIn = onSignIn,
        onBack = onBack,
        modifier = modifier,
    ) { posts ->
        if (posts.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.rmb_empty),
                body = stringResource(R.string.rmb_empty_hint),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    top = Dimens.ItemSpacing,
                    bottom = Dimens.SectionSpacing,
                ),
            ) {
                itemsIndexed(items = posts, key = { _, post -> post.id }) { index, post ->
                    // Why consecutive posts by one author lose their header: a board is a
                    // conversation, and repeating a name and avatar every two lines turns a
                    // reply into a stranger.
                    val continuesThread = index > 0 &&
                        posts[index - 1].author == post.author &&
                        posts[index - 1].visibility == RmbPost.Visibility.Visible &&
                        post.visibility == RmbPost.Visibility.Visible

                    PostRow(
                        post = post,
                        now = now,
                        continuesThread = continuesThread,
                        onOpenNation = onOpenNation,
                        onOpenRegion = onOpenRegion,
                    )
                }
            }
        }
    }
}

@Composable
private fun PostRow(
    post: RmbPost,
    now: Long,
    continuesThread: Boolean,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
) {
    if (post.visibility != RmbPost.Visibility.Visible) {
        RemovedNote(post.visibility)
        return
    }

    val name = remember(post.author) { NsId.toName(post.author) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (continuesThread) Dimens.ItemSpacing else Dimens.PostSpacing),
    ) {
        // The gutter is held even when the avatar is hidden, so a thread stays a column
        // rather than stepping left every time the same person speaks twice.
        if (continuesThread) {
            Spacer(Modifier.width(Dimens.AvatarSize))
        } else {
            NationAvatar(
                nationId = post.author,
                displayName = name,
                modifier = Modifier.clickable { onOpenNation(post.author) },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            if (!continuesThread) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenNation(post.author) },
                    )
                    Text(
                        text = RelativeTime.compact(post.postedAtEpochSeconds, now) +
                            if (post.wasEdited) stringResource(R.string.rmb_edited_suffix) else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            post.fromEmbassyRegion?.let { region ->
                Pill(text = stringResource(R.string.rmb_from_embassy, region))
            }

            RichText(post.body, onOpenNation, onOpenRegion)

            if (post.likes > 0) {
                // Why this is not a pill any more: a filled, rounded, icon-and-count control
                // is the universal shape of a like *button*, and pressing it did nothing. The
                // documented API has no like command — only `rmbpost` — so until that changes
                // this is a readout, and it has to look like one. See the README's gap list.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.PillIconSize),
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.rmb_likes,
                            post.likes,
                            Numbers.grouped(post.likes),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Why a thin line and not a card: a removed post is a gap in the conversation, and giving it
 * the same weight as a real post makes the board look full of things worth reading.
 */
@Composable
private fun RemovedNote(visibility: RmbPost.Visibility) {
    Text(
        text = stringResource(
            if (visibility == RmbPost.Visibility.Suppressed) {
                R.string.rmb_suppressed
            } else {
                R.string.rmb_deleted
            },
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.PostSpacing),
    )
}
