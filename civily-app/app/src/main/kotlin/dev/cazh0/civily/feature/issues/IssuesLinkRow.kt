package dev.cazh0.civily.feature.issues

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import coil.ImageLoader
import dev.cazh0.civily.R
import dev.cazh0.civily.core.result.LoadState
import dev.cazh0.civily.core.text.Countdown
import dev.cazh0.civily.data.issues.IssueBadge
import dev.cazh0.civily.ui.component.CountdownReachedEffect
import dev.cazh0.civily.ui.component.LinkRow
import dev.cazh0.civily.ui.component.countdownCompact
import dev.cazh0.civily.ui.component.rememberCountdown

/**
 * The Issues button, carrying its own answer.
 *
 * A button labelled "Issues" and nothing else asks the reader to open a screen to find out
 * whether it was worth opening. This one already knows: a count when decisions are waiting,
 * and when none are, how long until the next one. Either way the press is a decision the
 * reader has already made rather than a check they have to perform.
 *
 * Nothing is invented while the answer is unknown. A row mid-load, or one still holding the
 * previous nation's numbers a moment after a switch, wears the plain subtitle it always had —
 * a badge is a claim, and a claim made from no data is worse than no claim.
 *
 * @param badge the count and the next issue's instant. See [IssueBadge] for why it is stamped
 *   with the nation it belongs to.
 * @param activeNationId the nation whose name is on the card above this row.
 * @param onNextIssueDue called when the wait runs out with the reader watching, so the count
 *   under their eyes becomes right without them doing anything about it.
 */
@Composable
fun IssuesLinkRow(
    badge: LoadState<IssueBadge>,
    activeNationId: String,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onNextIssueDue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = (badge as? LoadState.Ready)?.value?.takeIf { it.nationId == activeNationId }
    val nextIssueTime = current?.nextIssueTime

    if (nextIssueTime != null) {
        CountdownReachedEffect(nextIssueTime, onNextIssueDue)
    }

    LinkRow(
        name = stringResource(R.string.title_issues),
        subtitle = issuesSubtitle(badge, current, nextIssueTime),
        flagUrl = "",
        imageLoader = imageLoader,
        onClick = onClick,
        modifier = modifier,
        badgeCount = current?.dueCount ?: 0,
    )
}

/**
 * One line, and it is always the most useful true thing available.
 *
 * The failure branch is deliberate. Spec §5 has no silent failures in it, and a count that
 * quietly stops updating when the device drops off the network is exactly that — the reader
 * would go on trusting a digit from twenty minutes ago. Saying so in the row's own quiet grey
 * costs nothing and keeps the press behind it, which reaches a screen that can retry properly.
 */
@Composable
private fun issuesSubtitle(
    badge: LoadState<IssueBadge>,
    current: IssueBadge?,
    nextIssueTime: Long?,
): String = when {
    current != null && current.dueCount > 0 -> pluralStringResource(
        R.plurals.issues_due,
        current.dueCount,
        current.dueCount,
    )

    nextIssueTime != null -> {
        // A row is glanced at, so it counts in minutes. The clock that counts in seconds is on
        // the screen this row opens, where the wait is the whole subject.
        val remaining = rememberCountdown(nextIssueTime, Countdown.Resolution.Minutes)
        stringResource(R.string.issues_next_in, countdownCompact(remaining))
    }

    badge is LoadState.Failed -> stringResource(badge.error.messageRes)

    else -> stringResource(R.string.subtitle_issues)
}
