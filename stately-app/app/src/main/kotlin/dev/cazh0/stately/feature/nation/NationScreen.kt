package dev.cazh0.stately.feature.nation

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
import androidx.compose.ui.text.font.FontStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import dev.cazh0.stately.R
import dev.cazh0.stately.core.graph
import dev.cazh0.stately.core.text.NsId
import dev.cazh0.stately.core.text.Population
import dev.cazh0.stately.data.nation.NationDto
import dev.cazh0.stately.ui.component.FactCard
import dev.cazh0.stately.ui.component.FlagHero
import dev.cazh0.stately.ui.component.LoadStateScaffold
import dev.cazh0.stately.ui.theme.Dimens

@Composable
fun NationScreen(
    nationId: String,
    onOpenRegion: (String) -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = LocalContext.current.graph
    val viewModel: NationViewModel = viewModel(
        key = nationId,
        factory = NationViewModel.factory(graph.nationRepository, nationId),
    )
    // Why collectAsStateWithLifecycle: collection stops when the screen leaves the foreground,
    // so a slow response cannot deliver into a composition that is no longer on screen.
    val state by viewModel.state.collectAsStateWithLifecycle()

    LoadStateScaffold(
        title = NsId.toName(nationId),
        state = state,
        onRetry = viewModel::refresh,
        onSignIn = onSignIn,
        onBack = onBack,
        modifier = modifier,
    ) { nation ->
        NationContent(nation, graph.imageLoader, onOpenRegion)
    }
}

@Composable
private fun NationContent(
    nation: NationDto,
    imageLoader: ImageLoader,
    onOpenRegion: (String) -> Unit,
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
            flagUrl = nation.flagUrl,
            contentDescription = stringResource(R.string.description_flag),
            imageLoader = imageLoader,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing)) {
            Text(
                text = stringResource(R.string.nation_full_title, nation.type, nation.name),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (nation.motto.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.nation_motto, nation.motto),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            }
        }

        FactCard(
            labelRes = R.string.label_region,
            value = nation.region,
            onClick = if (nation.region.isNotEmpty()) {
                { onOpenRegion(NsId.fromName(nation.region)) }
            } else {
                null
            },
        )
        FactCard(R.string.label_category, nation.category)
        val population = Population.scale(nation.population)
        FactCard(
            labelRes = R.string.label_population,
            value = stringResource(
                if (population.inBillions) {
                    R.string.value_population_billions
                } else {
                    R.string.value_population_millions
                },
                population.value,
            ),
        )
        FactCard(R.string.label_wa_status, nation.waStatus)
        FactCard(R.string.label_influence, nation.influence)
        FactCard(R.string.label_people, nation.people)
        FactCard(R.string.label_currency, nation.currency)
        FactCard(R.string.label_animal, nation.animal)
    }
}
