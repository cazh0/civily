package dev.cazh0.stately.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.cazh0.stately.R
import dev.cazh0.stately.core.result.LoadState

/**
 * The frame every read-only detail screen sits in.
 *
 * Why here: it makes the three-case `when` unwriteable at the feature level, so no screen can
 * quietly forget its failure branch. Adding a screen means supplying only the Ready case.
 *
 * The title collapses on scroll, the bar goes back, and the content pulls to refresh — three
 * behaviours every detail screen needs and none of them worth reimplementing per feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LoadStateScaffold(
    title: String,
    state: LoadState<T>,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        LoadStateContent(
            state = state,
            onRetry = onRetry,
            onSignIn = onSignIn,
            modifier = Modifier.padding(insets),
            content = content,
        )
    }
}
