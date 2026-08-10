package dev.cazh0.civily.feature.region

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import dev.cazh0.civily.ui.component.RichText
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
        title = NsId.toName(regionId),
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        FlagHero(
            flagUrl = region.flagUrl,
            contentDescription = stringResource(R.string.description_region_flag),
            imageLoader = imageLoader,
        )

        Text(text = region.name, style = MaterialTheme.typography.headlineSmall)

        LinkRow(
            name = stringResource(R.string.section_message_board),
            subtitle = stringResource(R.string.subtitle_message_board),
            flagUrl = "",
            imageLoader = imageLoader,
            onClick = onOpenMessageBoard,
        )

        FactCard(
            labelRes = R.string.label_num_nations,
            value = stringResource(R.string.value_nations, Numbers.grouped(region.nationCount)),
        )

        FactCard(
            labelRes = R.string.label_delegate,
            value = region.delegate?.let {
                stringResource(R.string.value_delegate, NsId.toName(it), region.delegateVotes)
            } ?: stringResource(R.string.value_none),
            onClick = region.delegate?.let { { onOpenNation(it) } },
        )

        FactCard(
            labelRes = R.string.label_founder,
            value = region.founder?.let { NsId.toName(it) }
                ?: stringResource(R.string.value_none),
            onClick = region.founder?.let { { onOpenNation(it) } },
        )

        if (region.factbook.isNotEmpty()) {
            Text(
                text = stringResource(R.string.section_factbook),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Dimens.TextSpacing),
            )
            RichText(
                blocks = region.factbook,
                onOpenNation = onOpenNation,
                onOpenRegion = onOpenRegion,
            )
        }
    }
}
