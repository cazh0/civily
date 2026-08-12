package dev.cazh0.civily.feature.issues

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import dev.cazh0.civily.R
import dev.cazh0.civily.core.graph
import dev.cazh0.civily.core.text.Newspaper
import dev.cazh0.civily.data.issues.Issue
import dev.cazh0.civily.data.issues.IssuesPage
import dev.cazh0.civily.ui.component.EmptyState
import dev.cazh0.civily.ui.component.LoadStateScaffold
import dev.cazh0.civily.ui.component.NewspaperFrontPage
import dev.cazh0.civily.ui.theme.Dimens
import java.util.Locale

/**
 * The nation's outstanding issues, one front page each.
 *
 * Why a list of headlines rather than every issue opened at once: an issue is a page of prose
 * and five advisors, so a screen holding four of them is forty screens of scrolling and no
 * sense of how many decisions are waiting. A headline answers "what is on my desk" in a glance.
 */
@Composable
fun IssuesScreen(
    viewModel: IssuesViewModel,
    onOpenIssue: (Int) -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader = LocalContext.current.graph.imageLoader

    LoadStateScaffold(
        title = stringResource(R.string.title_issues),
        state = state,
        onRetry = viewModel::refresh,
        onSignIn = onSignIn,
        onBack = onBack,
        modifier = modifier,
    ) { page ->
        if (page.issues.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.issues_empty),
                body = stringResource(R.string.issues_empty_hint),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                items(
                    items = page.issues,
                    key = { it.id },
                    // One shape per row, so the list can reuse a front page's layout nodes for
                    // the next front page rather than building them again.
                    contentType = { FRONT_PAGE },
                ) { issue ->
                    IssueHeadlineCard(
                        issue = issue,
                        page = page,
                        imageLoader = imageLoader,
                        onClick = { onOpenIssue(issue.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueHeadlineCard(
    issue: Issue,
    page: IssuesPage,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
) {
    // Why there is no card: a card is a rectangle, and the whole point of the torn strips is
    // that the page is not one. The newspaper meets the screen directly.
    NewspaperFrontPage(
        masthead = remember(page, issue.id) {
            Newspaper.masthead(page.capital, page.nationName, issue.id)
        },
        edition = remember(issue.id) { Newspaper.edition(issue.id) },
        headline = issue.title,
        price = page.currency.takeIf { it.isNotBlank() }
            ?.let { stringResource(R.string.newspaper_price, it.uppercase(Locale.US)) },
        flagUrl = page.flagUrl.takeIf { it.isNotBlank() },
        imageUrls = issue.imageUrls,
        imageLoader = imageLoader,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

/** The list holds one shape, and this names it for the reuse pool. */
private const val FRONT_PAGE = "front-page"
