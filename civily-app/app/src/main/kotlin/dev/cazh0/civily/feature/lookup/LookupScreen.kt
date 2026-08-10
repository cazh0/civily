package dev.cazh0.civily.feature.lookup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import dev.cazh0.civily.R
import dev.cazh0.civily.core.graph
import dev.cazh0.civily.feature.accounts.AccountsSection
import dev.cazh0.civily.ui.component.LinkRow
import dev.cazh0.civily.ui.component.SectionHeader
import dev.cazh0.civily.ui.theme.Dimens

/**
 * The front door: reach your own nation, or find anyone else's.
 *
 * Why search resolves here instead of navigating straight to a detail screen: a misspelled
 * name used to take the user to a full-screen error they had to press back out of, retype
 * from scratch, and try again. Resolving in place means a wrong guess costs one word of
 * editing, and a right one shows the flag before you commit to opening it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookupScreen(
    onOpenNation: (String) -> Unit,
    onOpenWorldAssembly: () -> Unit,
    onOpenIssues: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = LocalContext.current.graph
    val accounts by graph.sessionStore.accounts.collectAsStateWithLifecycle()
    val viewModel: LookupViewModel = viewModel(
        factory = LookupViewModel.factory(graph.nationRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var typedName by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun runSearch() {
        focusManager.clearFocus()
        viewModel.search(typedName)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(bottom = Dimens.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            AccountsSection(
                accounts = accounts,
                imageLoader = graph.imageLoader,
                onOpenNation = onOpenNation,
                onOpenIssues = onOpenIssues,
                onSwitch = graph.sessionStore::switchTo,
                onForget = graph.sessionStore::forget,
                onAddNation = onSignIn,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                SectionHeader(stringResource(R.string.section_find_nation))

                OutlinedTextField(
                    value = typedName,
                    onValueChange = {
                        typedName = it
                        viewModel.clear()
                    },
                    label = { Text(stringResource(R.string.label_nation_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.CardCornerRadius),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (typedName.isNotEmpty()) {
                            IconButton(onClick = {
                                typedName = ""
                                viewModel.clear()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.action_clear),
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                SearchResult(
                    state = state,
                    imageLoader = graph.imageLoader,
                    onOpenNation = onOpenNation,
                    onRetry = { runSearch() },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                SectionHeader(stringResource(R.string.section_explore))
                LinkRow(
                    name = stringResource(R.string.title_wa),
                    subtitle = stringResource(R.string.subtitle_wa),
                    flagUrl = "",
                    imageLoader = graph.imageLoader,
                    onClick = onOpenWorldAssembly,
                )
            }
        }
    }
}

@Composable
private fun SearchResult(
    state: LookupState,
    imageLoader: ImageLoader,
    onOpenNation: (String) -> Unit,
    onRetry: () -> Unit,
) {
    // Why animated: the result area appears and disappears as the user types. Popping content
    // in and out without a transition reads as a glitch rather than a response.
    AnimatedVisibility(visible = state != LookupState.Idle) {
        when (state) {
            LookupState.Idle -> Unit

            LookupState.Searching -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.ItemSpacing),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.size(Dimens.ProgressSize))
            }

            is LookupState.Found -> LinkRow(
                name = state.nation.name,
                subtitle = state.nation.region,
                flagUrl = state.nation.flagUrl,
                imageLoader = imageLoader,
                onClick = { onOpenNation(state.nationId) },
            )

            is LookupState.Missing -> InlineNotice(
                title = stringResource(R.string.lookup_no_nation, state.typedName),
                body = stringResource(R.string.lookup_no_nation_hint),
            )

            is LookupState.Failed -> Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                InlineNotice(
                    title = stringResource(state.error.messageRes),
                    body = "",
                    error = true,
                )
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
    }
}

@Composable
private fun InlineNotice(title: String, body: String, error: Boolean = false) {
    val container = if (error) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val onContainer = if (error) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = onContainer,
            )
            if (body.isNotEmpty()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer,
                )
            }
        }
    }
}
