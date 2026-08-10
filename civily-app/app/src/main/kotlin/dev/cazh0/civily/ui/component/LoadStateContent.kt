package dev.cazh0.civily.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cazh0.civily.core.result.LoadState

/**
 * The three-case `when`, written once.
 *
 * [LoadStateScaffold] is this plus a top bar, and is what a plain detail screen wants. A
 * screen that owns its own chrome — tabs, a pager — uses this directly so the chrome stays
 * on screen while the content below it loads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LoadStateContent(
    state: LoadState<T>,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        LoadState.Loading -> LoadingState(modifier)
        is LoadState.Failed -> ErrorState(state.error, onRetry, onSignIn, modifier)
        is LoadState.Ready -> PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRetry,
            modifier = modifier,
        ) {
            content(state.value)
        }
    }
}
