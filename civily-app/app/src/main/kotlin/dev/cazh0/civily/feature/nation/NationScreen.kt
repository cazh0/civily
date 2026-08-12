package dev.cazh0.civily.feature.nation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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

    LoadStateScaffold(
        // Why remembered: the title is derived from the route argument, which cannot change
        // while this screen exists, so re-deriving it on every recomposition is pure waste.
        title = remember(nationId) { NsId.toName(nationId) },
        state = state,
        onRetry = viewModel::refresh,
        onSignIn = onSignIn,
        onBack = onBack,
        modifier = modifier,
    ) { nation ->
        // One instant for the whole feed, read once per load rather than per recomposition: two
        // happenings a frame apart must not disagree about what "3 hours ago" means. Keyed on
        // the nation rather than on the load state, because a data class holding ninety
        // rankings is an expensive thing to compare and the instance is what actually changes.
        val now = remember(nation) { System.currentTimeMillis() }

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
 * Why each pane owns its own scroll rather than one scroll around the lot: every pane is a lazy
 * list, and a lazy list nested in a scroll of the same axis has no bounded height to lay out in.
 * A shared scroll would also carry one position across seven unrelated pages.
 *
 * Why every pane is lazy and not just the two long ones: the top app bar collapses as the reader
 * scrolls, which changes the height the content is measured against on every frame of the
 * gesture. With an eager column that is a re-measure of the whole page per frame; with a lazy
 * one it is a re-measure of what is on screen, and a page that is opened but not scrolled to the
 * bottom never composes the part nobody looked at. That is the difference between a tab that
 * appears and a tab that arrives.
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
    val index = selected.coerceIn(tabs.indices)

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = index,
            // The default edge padding is a 52dp indent that leaves the first tab looking
            // half-scrolled. Aligning it with the screen's own margin is what makes the row read
            // as part of the page.
            edgePadding = Dimens.ScreenPadding,
        ) {
            tabs.forEachIndexed { position, entry ->
                Tab(
                    selected = position == index,
                    onClick = { onSelect(position) },
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

        when (tabs[index]) {
            NationTab.Overview -> NationOverviewPane(nation, now, imageLoader, onOpenRegion)

            NationTab.Policies -> NationPoliciesPane(nation.policies, imageLoader)

            NationTab.People -> NationPeoplePane(nation, onOpenNation, onOpenRegion)

            NationTab.Government -> NationGovernmentPane(nation, onOpenNation, onOpenRegion)

            NationTab.Economy -> NationEconomyPane(nation, onOpenNation, onOpenRegion)

            NationTab.Rankings -> NationRankingsPane(nation.rankings)

            NationTab.Happenings ->
                NationHappeningsPane(nation.happenings, now, onOpenNation, onOpenRegion)
        }
    }
}

/**
 * The frame the prose-and-grid panes share: one lazy list and one screen margin.
 *
 * Why the margin is [LazyColumn]'s own `contentPadding` rather than a `Modifier.padding` around
 * it: a modifier insets the scrolling viewport, so an item leaving the top would be clipped a
 * margin early and the list would overscroll against an edge that is not the screen's. As
 * content padding the list fills the pane and only its items are inset.
 *
 * Why the gap between blocks is [SectionGap] on each block rather than an arrangement on the
 * list: a section with nothing in it draws nothing, and a lazy list spaces a zero-height item
 * exactly as it spaces a full one — which would leave a section's worth of blank page where the
 * API happened to send no data. A gap carried by the block disappears with the block.
 */
@Composable
internal fun NationPane(
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // No bottom margin of its own: the last block's own gap is the foot of the page.
        contentPadding = PaddingValues(
            start = Dimens.ScreenPadding,
            top = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
        ),
        content = content,
    )
}

/**
 * What a pane's items are, for the list's own reuse pool.
 *
 * A lazy list can only reuse a scrapped item's layout nodes for an item of the same type, and
 * these panes are a handful of one-off blocks and one long run of identical ones. Naming them is
 * what lets the run of happenings recycle among themselves instead of against the chart above.
 */
internal object PaneContent {
    const val Identity = "identity"
    const val Facts = "facts"
    const val Prose = "prose"
    const val Chart = "chart"
    const val Happening = "happening"
    const val Ranking = "ranking"
    const val Policy = "policy"
    const val Header = "header"
}
