package dev.cazh0.civily.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import dev.cazh0.civily.R
import dev.cazh0.civily.core.result.CivilyError
import dev.cazh0.civily.ui.theme.Dimens

/** The one loading state. Every screen uses it so "loading" looks the same everywhere. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(Dimens.ProgressSize))
    }
}

/**
 * A screen that loaded fine and has nothing to show.
 *
 * Deliberately not [ErrorState]: an empty message board is not a failure, and dressing it as
 * one tells the user something is broken when nothing is.
 */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The one failure state.
 *
 * Why it takes a [CivilyError] rather than a string: the error already carries the message
 * resource, so no screen can show a failure it invented or forget to offer a way out of it
 * (spec §1.2). The retry button is part of the state for the same reason — a dead end is not
 * a state this app is allowed to render.
 */
@Composable
fun ErrorState(
    error: CivilyError,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Dimens.StateIconSize),
        )
        Text(
            text = stringResource(error.messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Why the action changes: retrying a dead session can only fail again. Offering the
        // button that cannot work is worse than offering none.
        if (error.needsSignIn) {
            FilledTonalButton(onClick = onSignIn) {
                Text(stringResource(R.string.action_sign_in_again))
            }
        } else {
            FilledTonalButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.ItemSpacing * 1.5f),
                )
                Text(
                    text = stringResource(R.string.action_retry),
                    modifier = Modifier.padding(start = Dimens.TextSpacing * 2),
                )
            }
        }
    }
}
