package dev.cazh0.civily.feature.issues

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cazh0.civily.R
import dev.cazh0.civily.core.graph
import dev.cazh0.civily.core.result.CivilyError
import dev.cazh0.civily.core.result.LoadState
import dev.cazh0.civily.core.text.Newspaper
import dev.cazh0.civily.core.text.bbcode.BbBlock
import dev.cazh0.civily.data.issues.IssueOption
import dev.cazh0.civily.data.issues.IssuesPage
import dev.cazh0.civily.data.issues.IssuesRepository
import dev.cazh0.civily.ui.component.LoadingState
import dev.cazh0.civily.ui.component.NewspaperFrontPage
import dev.cazh0.civily.ui.component.RichText
import dev.cazh0.civily.ui.theme.Dimens
import java.util.Locale

/**
 * One issue, and the decision it is asking for.
 *
 * There is no confirmation dialog. Choosing an option and then pressing the commit inside it
 * is already two deliberate, separately-aimed actions, and the commit says exactly what it
 * does — a third "are you sure?" repeating the same sentence adds a tap without adding a
 * thought. Dismissing works the same way rather than being a single unguarded tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    issueId: Int,
    viewModel: IssuesViewModel,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val answer by viewModel.answer.collectAsStateWithLifecycle()
    val graph = LocalContext.current.graph
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var chosen by remember(issueId) { mutableStateOf<Int?>(null) }

    val page = (state as? LoadState.Ready)?.value
    val issue = page?.issues?.firstOrNull { it.id == issueId }

    // Why the screen does not simply close on success: the consequence is the point of
    // answering. It takes the screen over until the reader has finished with it.
    val outcome = answer as? AnswerState.Done

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            outcome != null -> stringResource(R.string.title_issue_result)
                            issue != null -> issue.title
                            else -> stringResource(R.string.title_issues)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { insets ->
        if (outcome != null) {
            IssueResultView(
                result = outcome.result,
                masthead = Newspaper.masthead(
                    capital = page?.capital.orEmpty(),
                    nationName = page?.nationName.orEmpty(),
                    issueId = issueId,
                ),
                edition = Newspaper.edition(issueId),
                price = page?.coverPrice(),
                flagUrl = page?.flagUrl?.takeIf { it.isNotBlank() },
                imageLoader = graph.imageLoader,
                onDone = {
                    viewModel.dismissAnswerState()
                    onBack()
                },
                modifier = Modifier.padding(insets),
            )
            return@Scaffold
        }

        // Page first: `issue` is looked up inside it, so testing the issue first would make
        // the page check unreachable.
        if (page == null || issue == null) {
            LoadingState(Modifier.padding(insets))
            return@Scaffold
        }

        val busy = answer == AnswerState.Sending

        LazyColumn(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimens.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            item {
                NewspaperFrontPage(
                    masthead = remember(page, issue.id) {
                        Newspaper.masthead(page.capital, page.nationName, issue.id)
                    },
                    edition = remember(issue.id) { Newspaper.edition(issue.id) },
                    headline = issue.title,
                    price = page.coverPrice(),
                    flagUrl = page.flagUrl.takeIf { it.isNotBlank() },
                    imageUrls = issue.imageUrls,
                    imageLoader = graph.imageLoader,
                )
            }

            item {
                RichText(
                    blocks = issue.text,
                    onOpenNation = onOpenNation,
                    onOpenRegion = onOpenRegion,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.issues_options_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = Dimens.ScreenPadding,
                        end = Dimens.ScreenPadding,
                        top = Dimens.ItemSpacing,
                    ),
                )
            }

            itemsIndexed(
                items = issue.options,
                key = { _, option -> "option-${option.id}" },
            ) { index, option ->
                OptionRow(
                    option = option,
                    ordinal = index + 1,
                    selected = option.id == chosen,
                    busy = busy,
                    onClick = { chosen = option.id },
                    onCommit = { viewModel.answer(issue.id, option.id) },
                    commitLabel = stringResource(R.string.action_enact_this),
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                )
            }

            item {
                DismissRow(
                    selected = chosen == IssuesRepository.DISMISS,
                    busy = busy,
                    onClick = { chosen = IssuesRepository.DISMISS },
                    onCommit = { viewModel.answer(issue.id, IssuesRepository.DISMISS) },
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                )
            }
        }
    }

    (answer as? AnswerState.Failed)?.let { failure ->
        val resultUnknown = failure.error is CivilyError.IssueResultUnknown
        val title = if (resultUnknown) {
            R.string.issue_answer_unknown
        } else {
            R.string.issue_answer_failed
        }
        val closeFailure = {
            viewModel.dismissAnswerState()
            if (resultUnknown || failure.error.needsSignIn) onBack()
        }
        AlertDialog(
            onDismissRequest = closeFailure,
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(failure.error.messageRes)) },
            confirmButton = {
                TextButton(onClick = closeFailure) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }
}

@Composable
private fun OptionRow(
    option: IssueOption,
    ordinal: Int,
    selected: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onCommit: () -> Unit,
    commitLabel: String,
    modifier: Modifier = Modifier,
) {
    SelectableAction(
        selected = selected,
        busy = busy,
        onClick = onClick,
        onCommit = onCommit,
        commitLabel = commitLabel,
        modifier = modifier,
    ) { contentColor ->
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
            OrdinalBadge(ordinal, selected, contentColor)
            Text(
                text = remember(option) { option.text.plainText() },
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DismissRow(
    selected: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableAction(
        selected = selected,
        busy = busy,
        onClick = onClick,
        onCommit = onCommit,
        commitLabel = stringResource(R.string.action_dismiss_confirm),
        modifier = modifier,
    ) { contentColor ->
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing)) {
            Text(
                text = stringResource(R.string.action_dismiss_issue),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
            Text(
                text = stringResource(R.string.issue_dismiss_explainer),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

/**
 * A choice that only becomes an action once it has been chosen.
 *
 * The selected state is marked by a filled container *and* a border: on a wallpaper-derived
 * palette two container colours can land within a few percent of each other, and "which one
 * did I pick" must never be a question on something that cannot be undone.
 */
@Composable
private fun SelectableAction(
    selected: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onCommit: () -> Unit,
    commitLabel: String,
    modifier: Modifier = Modifier,
    body: @Composable (contentColor: Color) -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        color = container,
        border = if (selected) {
            BorderStroke(Dimens.SelectionBorder, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !busy, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            body(contentColor)

            // The commit lives inside the chosen row so the second tap is where the first
            // one landed — two deliberate acts, no journey between them.
            AnimatedVisibility(visible = selected) {
                Button(
                    onClick = onCommit,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.PillIconSize),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.PillIconSize),
                        )
                        Text(
                            text = commitLabel,
                            modifier = Modifier.padding(start = Dimens.TextSpacing * 2),
                        )
                    }
                }
            }
        }
    }
}

/** The option's number, filled once chosen so the choice reads at a glance while scrolling. */
@Composable
private fun OrdinalBadge(ordinal: Int, selected: Boolean, contentColor: Color) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = if (selected) null else BorderStroke(Dimens.HairlineBorder, contentColor),
        modifier = Modifier.size(Dimens.OrdinalBadgeSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$ordinal",
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else contentColor,
            )
        }
    }
}

/** "1 DOLLAR" on the design's example page — the nation's own currency, as the site prints it. */
@Composable
private fun IssuesPage.coverPrice(): String? =
    currency.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.newspaper_price, it.uppercase(Locale.US)) }

/** Options are short prose; a row needs them as one run of text. */
private fun List<BbBlock>.plainText(): String = buildString {
    this@plainText.forEach { block ->
        if (block is BbBlock.Paragraph) block.spans.forEach { append(it.text) }
    }
}.trim()
