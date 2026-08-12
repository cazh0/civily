package dev.cazh0.civily.feature.issues

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import dev.cazh0.civily.R
import dev.cazh0.civily.core.graph
import dev.cazh0.civily.core.text.Countdown
import dev.cazh0.civily.core.text.Newspaper
import dev.cazh0.civily.data.issues.Issue
import dev.cazh0.civily.data.issues.IssuesPage
import dev.cazh0.civily.ui.component.CountdownReachedEffect
import dev.cazh0.civily.ui.component.EmptyState
import dev.cazh0.civily.ui.component.LoadStateScaffold
import dev.cazh0.civily.ui.component.NewspaperFrontPage
import dev.cazh0.civily.ui.component.countdownClock
import dev.cazh0.civily.ui.component.rememberCountdown
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
                body = stringResource(R.string.issues_empty_hint, page.nationName),
            ) {
                val nextIssueTime = page.nextIssueTime
                if (nextIssueTime != null) {
                    NextIssueClock(
                        nextIssueTime = nextIssueTime,
                        onDue = viewModel::refresh,
                        modifier = Modifier.padding(top = Dimens.SectionSpacing),
                    )
                }
            }
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

/**
 * The wait, given the screen it has to itself.
 *
 * A nation with nothing on its desk is not an error and not a dead end — it is a nation between
 * issues, and the game says exactly when the next one lands. So the emptiness is dressed as what
 * it actually is: a clock. It runs to the second because this is the one screen with nothing
 * else on it, and a countdown that only moves once a minute reads as a screenshot.
 *
 * The figures are tabular, which is the whole reason the block never twitches: Roboto's
 * proportional digits are different widths, so a plain `Text` counting down re-measures itself
 * on nearly every tick and nudges everything around it. With `tnum` the string changes and the
 * layout does not, so a second costs one redraw of one node and no measure pass at all.
 *
 * The hours field is written even at zero — "0:04:11" — for the same reason: crossing an hour
 * must not move the digits the reader is looking at.
 */
@Composable
private fun NextIssueClock(
    nextIssueTime: Long,
    onDue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The wait ends by itself, so the screen counting it down is the screen that notices.
    CountdownReachedEffect(nextIssueTime, onDue)

    val remaining = rememberCountdown(nextIssueTime, Countdown.Resolution.Seconds)

    Surface(
        shape = RoundedCornerShape(Dimens.TileCornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = Dimens.SectionSpacing,
                vertical = Dimens.CardPadding,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            Text(
                text = stringResource(R.string.issues_next_label),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = countdownClock(remaining),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFeatureSettings = TABULAR_FIGURES,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** The list holds one shape, and this names it for the reuse pool. */
private const val FRONT_PAGE = "front-page"

/** OpenType tabular figures: every digit the same width, so a running clock cannot reflow. */
private const val TABULAR_FIGURES = "tnum"
