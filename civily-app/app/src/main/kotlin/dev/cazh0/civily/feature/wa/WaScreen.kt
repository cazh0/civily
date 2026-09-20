package dev.cazh0.civily.feature.wa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import dev.cazh0.civily.data.wa.VoteTally
import dev.cazh0.civily.ui.component.LoadStateContent
import dev.cazh0.civily.ui.component.richTextItems
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
    val resolution = assembly.resolution

    // Why lazy: a resolution's text is the longest single piece of prose the app renders — a
    // General Assembly proposal runs to dozens of clauses — and every one of them would be laid
    // out before the title appeared.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        if (resolution == null) {
            // Why a neutral card and not an error: between votes the chamber is genuinely
            // empty for hours at a time. That is the World Assembly working normally.
            item(contentType = "empty") { NoResolutionCard() }
        } else {
            item(contentType = "resolution") {
                ResolutionCard(resolution, onOpenNation)
            }
        }
        item(contentType = "facts") { AssemblyFactsCard(assembly) }
        if (resolution != null) {
            richTextItems(
                blocks = resolution.body,
                onOpenNation = onOpenNation,
                onOpenRegion = onOpenRegion,
            )
        }
    }
}

@Composable
private fun AssemblyFactsCard(assembly: Assembly) {
    OutlinedCard(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            AssemblyFact(
                label = stringResource(R.string.label_wa_members),
                value = Numbers.grouped(assembly.memberCount),
            )
            HorizontalDivider()
            AssemblyFact(
                label = stringResource(R.string.label_wa_delegates),
                value = Numbers.grouped(assembly.delegateCount),
            )
        }
    }
}

@Composable
private fun AssemblyFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun VoteBreakdownRow(label: String, votesFor: Int, votesAgainst: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.votes_for, Numbers.grouped(votesFor)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.votes_against, Numbers.grouped(votesAgainst)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun VoteHistoryChart(history: List<VoteTally>, contentDescription: String) {
    val votesForColor = MaterialTheme.colorScheme.primary
    val votesAgainstColor = MaterialTheme.colorScheme.error
    // Why one scale: the two lines are comparable only when their shared height means one value.
    val maximum = history.maxOf { maxOf(it.votesFor, it.votesAgainst) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.VoteHistoryHeight)
            .semantics { this.contentDescription = contentDescription },
    ) {
        if (maximum == 0) return@Canvas

        fun coordinate(index: Int, votes: Int) = Offset(
            x = size.width * index / history.lastIndex,
            y = size.height * (1f - votes.toFloat() / maximum),
        )

        fun drawTrend(votes: (VoteTally) -> Int, color: androidx.compose.ui.graphics.Color) {
            val path = Path().apply {
                history.forEachIndexed { index, tally ->
                    val point = coordinate(index, votes(tally))
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
            drawPath(path, color, style = Stroke(Dimens.VoteHistoryStroke.toPx()))
        }

        drawTrend(VoteTally::votesFor, votesForColor)
        drawTrend(VoteTally::votesAgainst, votesAgainstColor)
    }
}

@Composable
private fun ResolutionCard(resolution: Resolution, onOpenNation: (String) -> Unit) {
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

            if (resolution.proposedBy.isNotEmpty()) {
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.TouchTarget)
                        .clickable { onOpenNation(resolution.proposedBy) },
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.label_proposed_by),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = NsId.toName(resolution.proposedBy),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                Text(
                    text = stringResource(R.string.section_vote_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                )
                VoteBreakdownRow(
                    label = stringResource(R.string.label_nation_votes),
                    votesFor = resolution.voteBreakdown.nationsFor,
                    votesAgainst = resolution.voteBreakdown.nationsAgainst,
                )
                VoteBreakdownRow(
                    label = stringResource(R.string.label_delegate_votes),
                    votesFor = resolution.voteBreakdown.delegatesFor,
                    votesAgainst = resolution.voteBreakdown.delegatesAgainst,
                )
            }

            if (resolution.voteHistory.size >= 2) {
                val latest = resolution.voteHistory.last()
                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    Text(
                        text = stringResource(R.string.section_vote_history),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    VoteHistoryChart(
                        history = resolution.voteHistory,
                        contentDescription = stringResource(
                            R.string.vote_history_description,
                            Numbers.grouped(latest.votesFor),
                            Numbers.grouped(latest.votesAgainst),
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.votes_for, Numbers.grouped(latest.votesFor)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.votes_against, Numbers.grouped(latest.votesAgainst)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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
