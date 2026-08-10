package dev.cazh0.civily.feature.wa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cazh0.civily.R
import dev.cazh0.civily.core.graph
import dev.cazh0.civily.core.text.Numbers
import dev.cazh0.civily.core.text.NsId
import dev.cazh0.civily.data.wa.Assembly
import dev.cazh0.civily.data.wa.Council
import dev.cazh0.civily.data.wa.Resolution
import dev.cazh0.civily.ui.component.FactCard
import dev.cazh0.civily.ui.component.LoadStateContent
import dev.cazh0.civily.ui.component.RichText
import dev.cazh0.civily.ui.theme.Dimens

private val COUNCILS = listOf(Council.GeneralAssembly, Council.SecurityCouncil)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaScreen(
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Why the screen owns its chrome instead of using LoadStateScaffold: each chamber loads
    // separately, and tabs that vanish while one of them is loading would make the other
    // unreachable.
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_wa)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { insets ->
        Column(Modifier.padding(insets)) {
            TabRow(selectedTabIndex = selected) {
                COUNCILS.forEachIndexed { index, council ->
                    Tab(
                        selected = index == selected,
                        onClick = { selected = index },
                        text = {
                            Text(
                                text = stringResource(council.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
            CouncilPane(COUNCILS[selected], onOpenNation, onOpenRegion, onSignIn)
        }
    }
}

private val Council.labelRes: Int
    get() = when (this) {
        Council.GeneralAssembly -> R.string.council_general_assembly
        Council.SecurityCouncil -> R.string.council_security_council
    }

@Composable
private fun CouncilPane(
    council: Council,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    val graph = LocalContext.current.graph
    val viewModel: WaViewModel = viewModel(
        key = "wa-${council.id}",
        factory = WaViewModel.factory(graph.assemblyRepository, council),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LoadStateContent(
        state = state,
        onRetry = viewModel::refresh,
        onSignIn = onSignIn,
    ) { assembly ->
        CouncilContent(assembly, onOpenNation, onOpenRegion)
    }
}

@Composable
private fun CouncilContent(
    assembly: Assembly,
    onOpenNation: (String) -> Unit,
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
        val resolution = assembly.resolution
        if (resolution == null) {
            // Why a neutral card and not an error: between votes the chamber is genuinely
            // empty for hours at a time. That is the World Assembly working normally.
            NoResolutionCard()
        } else {
            ResolutionCard(resolution)
            if (resolution.proposedBy.isNotEmpty()) {
                FactCard(
                    labelRes = R.string.label_proposed_by,
                    value = NsId.toName(resolution.proposedBy),
                    onClick = { onOpenNation(resolution.proposedBy) },
                )
            }
            if (resolution.body.isNotEmpty()) {
                RichText(
                    blocks = resolution.body,
                    onOpenNation = onOpenNation,
                    onOpenRegion = onOpenRegion,
                )
            }
        }

        FactCard(
            labelRes = R.string.label_wa_members,
            value = Numbers.grouped(assembly.memberCount),
        )
        FactCard(
            labelRes = R.string.label_wa_delegates,
            value = Numbers.grouped(assembly.delegateCount),
        )
    }
}

@Composable
private fun ResolutionCard(resolution: Resolution) {
    Card(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing)) {
                Text(
                    text = stringResource(R.string.section_at_vote),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = resolution.name, style = MaterialTheme.typography.titleLarge)
                if (resolution.category.isNotEmpty()) {
                    Text(
                        text = resolution.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing)) {
                LinearProgressIndicator(
                    progress = { resolution.supportFraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(
                            R.string.votes_for,
                            Numbers.grouped(resolution.votesFor),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(
                            R.string.votes_against,
                            Numbers.grouped(resolution.votesAgainst),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoResolutionCard() {
    Card(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            Text(
                text = stringResource(R.string.wa_no_resolution),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.wa_no_resolution_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
