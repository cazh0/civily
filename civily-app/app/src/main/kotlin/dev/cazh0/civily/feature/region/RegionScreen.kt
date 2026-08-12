package dev.cazh0.civily.feature.region

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import dev.cazh0.civily.R
import dev.cazh0.civily.core.graph
import dev.cazh0.civily.core.text.Numbers
import dev.cazh0.civily.core.text.NsId
import dev.cazh0.civily.data.region.Region
import dev.cazh0.civily.ui.component.FactCard
import dev.cazh0.civily.ui.component.FlagHero
import dev.cazh0.civily.ui.component.LinkRow
import dev.cazh0.civily.ui.component.LoadStateScaffold
import dev.cazh0.civily.ui.component.richTextItems
import dev.cazh0.civily.ui.theme.Dimens

@Composable
fun RegionScreen(
    regionId: String,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    onOpenMessageBoard: (String) -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = LocalContext.current.graph
    val viewModel: RegionViewModel = viewModel(
        key = regionId,
        factory = RegionViewModel.factory(graph.regionRepository, regionId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LoadStateScaffold(
        // Derived from a route argument that cannot change while this screen exists, so it is
        // derived once rather than on every recomposition.
        title = remember(regionId) { NsId.toName(regionId) },
        state = state,
        onRetry = viewModel::refresh,
        onSignIn = onSignIn,
        onBack = onBack,
        modifier = modifier,
    ) { region ->
        RegionContent(
            region = region,
            imageLoader = graph.imageLoader,
            onOpenNation = onOpenNation,
            onOpenRegion = onOpenRegion,
            onOpenMessageBoard = { onOpenMessageBoard(regionId) },
        )
    }
}

@Composable
private fun RegionContent(
    region: Region,
    imageLoader: ImageLoader,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    onOpenMessageBoard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Why lazy: a regional factbook has no length limit — the largest are hundreds of paragraphs
    // of parsed markup — and an eager column lays out every one of them, and re-lays them out on
    // each frame the top app bar collapses through. As list items the reader pays for the page
    // they are on.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        // Guarded here as well as inside FlagHero: an item that draws nothing is still an item,
        // and the list would space it exactly as it spaces the flag it does not have.
        if (region.flagUrl.isNotEmpty()) {
            item(contentType = "flag") {
                FlagHero(
                    flagUrl = region.flagUrl,
                    contentDescription = stringResource(R.string.description_region_flag),
                    imageLoader = imageLoader,
                )
            }
        }

        item(contentType = "name") {
            Text(text = region.name, style = MaterialTheme.typography.headlineSmall)
        }

        item(contentType = "link") {
            LinkRow(
                name = stringResource(R.string.section_message_board),
                subtitle = stringResource(R.string.subtitle_message_board),
                flagUrl = "",
                imageLoader = imageLoader,
                onClick = onOpenMessageBoard,
            )
        }

        item(contentType = "fact") {
            FactCard(
                labelRes = R.string.label_num_nations,
                value = stringResource(R.string.value_nations, Numbers.grouped(region.nationCount)),
            )
        }

        item(contentType = "fact") {
            FactCard(
                labelRes = R.string.label_delegate,
                value = region.delegate?.let {
                    stringResource(R.string.value_delegate, NsId.toName(it), region.delegateVotes)
                } ?: stringResource(R.string.value_none),
                onClick = region.delegate?.let { { onOpenNation(it) } },
            )
        }

        item(contentType = "fact") {
            FactCard(
                labelRes = R.string.label_founder,
                value = region.founder?.let { NsId.toName(it) }
                    ?: stringResource(R.string.value_none),
                onClick = region.founder?.let { { onOpenNation(it) } },
            )
        }

        if (region.factbook.isNotEmpty()) {
            item(contentType = "header") {
                Text(
                    text = stringResource(R.string.section_factbook),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Dimens.TextSpacing),
                )
            }
            richTextItems(
                blocks = region.factbook,
                onOpenNation = onOpenNation,
                onOpenRegion = onOpenRegion,
            )
        }
    }
}
