package dev.cazh0.civily.feature.nation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import dev.cazh0.civily.R
import dev.cazh0.civily.core.graph
import dev.cazh0.civily.core.text.NsId
import dev.cazh0.civily.data.nation.Nation
import dev.cazh0.civily.ui.component.LoadStateScaffold
import dev.cazh0.civily.ui.theme.Dimens

/**
 * The seven subjects a nation has, as tabs.
 *
 * The set and the order are the game's own nation page, which the legacy client already mirrors:
 * a reader who knows NationStates knows where to look before they have looked. Splitting them is
 * what lets each subject be answered properly — the government's paragraph next to the
 * government's budget — instead of thirty facts and four paragraphs in one column.
 */
private enum class NationTab(@StringRes val labelRes: Int) {
    Overview(R.string.tab_overview),
    Policies(R.string.tab_policies),
    People(R.string.tab_people),
    Government(R.string.tab_government),
    Economy(R.string.tab_economy),
    Rankings(R.string.tab_rankings),
    Happenings(R.string.tab_happenings),
}

@Composable
fun NationScreen(
    nationId: String,
    onOpenNation: (String) -> Unit,
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

    // Held out here rather than inside the Ready branch so a pull-to-refresh, a rotation or a
    // failed retry all come back to the tab the user was reading.
    var selected by rememberSaveable { mutableIntStateOf(0) }

    // One instant for the whole feed, read once per load rather than per recomposition: two
    // happenings a frame apart must not disagree about what "3 hours ago" means.
    val now = remember(state) { System.currentTimeMillis() }

    LoadStateScaffold(
        title = NsId.toName(nationId),
        state = state,
        onRetry = viewModel::refresh,
        onSignIn = onSignIn,
        onBack = onBack,
        modifier = modifier,
    ) { nation ->
        NationContent(
            nation = nation,
            now = now,
            imageLoader = graph.imageLoader,
            selected = selected,
            onSelect = { selected = it },
            onOpenNation = onOpenNation,
            onOpenRegion = onOpenRegion,
        )
    }
}

/**
 * The tab bar, pinned, and the pane under it.
 *
 * Why the bar does not scroll away with the content: it is the only way between subjects, and a
 * reader four screens down the Rankings has to be able to leave without scrolling back up.
 *
 * Why each pane owns its own scroll rather than one scroll around the lot: two of them are real
 * lists — ninety census scales, twenty policy banners — and those have to be lazy. A shared outer
 * scroll would give a nested lazy list no bounded height to lay out in, and would also carry one
 * scroll position across seven unrelated pages.
 */
@Composable
private fun NationContent(
    nation: Nation,
    now: Long,
    imageLoader: ImageLoader,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = NationTab.entries
    val tab = tabs[selected.coerceIn(tabs.indices)]

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(tab),
            // The default edge padding is a 52dp indent that leaves the first tab looking
            // half-scrolled. Aligning it with the screen's own margin is what makes the row read
            // as part of the page.
            edgePadding = Dimens.ScreenPadding,
        ) {
            tabs.forEach { entry ->
                Tab(
                    selected = entry == tab,
                    onClick = { onSelect(tabs.indexOf(entry)) },
                    text = {
                        Text(
                            text = stringResource(entry.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        when (tab) {
            NationTab.Overview -> Scrolling {
                NationOverviewPane(nation, now, imageLoader, onOpenRegion)
            }

            // Already a lazy list, and it does its own padding: wrapping it in a scroll would
            // give it no bounded height.
            NationTab.Policies -> NationPoliciesPane(nation.policies, imageLoader)

            NationTab.People -> Scrolling {
                NationPeoplePane(nation, onOpenNation, onOpenRegion)
            }

            NationTab.Government -> Scrolling {
                NationGovernmentPane(nation, onOpenNation, onOpenRegion)
            }

            NationTab.Economy -> Scrolling {
                NationEconomyPane(nation, onOpenNation, onOpenRegion)
            }

            NationTab.Rankings -> NationRankingsPane(nation.rankings)

            NationTab.Happenings -> Scrolling {
                NationHappeningsPane(nation.happenings, now, onOpenNation, onOpenRegion)
            }
        }
    }
}

/** The frame the five prose-and-grid panes share: one scroll, one screen margin. */
@Composable
private fun Scrolling(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
    ) {
        content()
    }
}
