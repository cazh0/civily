package dev.cazh0.civily.feature.nation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.FreedomRating
import dev.cazh0.civily.core.text.bbcode.BbBlock
import dev.cazh0.civily.data.nation.Nation
import dev.cazh0.civily.ui.component.RichText
import dev.cazh0.civily.ui.component.SectionHeader

/**
 * The two blocks more than one pane builds: a titled grid of facts, and the freedom trio.
 */

/**
 * A titled grid, which disappears entirely when the API sent none of its facts — a header over
 * nothing is the same defect as a label with nothing under it.
 */
@Composable
fun FactSection(
    @StringRes titleRes: Int,
    facts: List<Fact>,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    if (facts.none { it.value.isNotEmpty() }) return

    Column(modifier = modifier) {
        SectionHeader(stringResource(titleRes))
        FactGrid(facts = facts, columns = columns)
    }
}

/**
 * A titled block of the game's own prose, absent when the shard came back empty.
 *
 * It goes through [dev.cazh0.civily.ui.component.RichText] rather than a `Text` even though the
 * game writes these paragraphs itself — that is the rule for anything rendering NationStates
 * content, and it is what guarantees no reader sees a tag the day the game dresses them.
 */
@Composable
fun ProseSection(
    @StringRes titleRes: Int,
    blocks: List<BbBlock>,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (blocks.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(stringResource(titleRes))
        RichText(blocks = blocks, onOpenNation = onOpenNation, onOpenRegion = onOpenRegion)
    }
}

/**
 * The three ratings, three across.
 *
 * Not two: these three are one comparison, and a grid that wraps the third onto its own line
 * invites the reader to treat it as a different kind of fact.
 */
@Composable
fun FreedomSection(nation: Nation, modifier: Modifier = Modifier) {
    FactSection(
        titleRes = R.string.section_freedoms,
        // The only facts on this screen that are good or bad news rather than simply true, so
        // the only ones that carry a colour.
        facts = listOf(
            rating(R.string.label_civil_rights, nation.civilRights),
            rating(R.string.label_economy_rating, nation.economyRating),
            rating(R.string.label_political_freedom, nation.politicalFreedom),
        ),
        columns = FREEDOM_COLUMNS,
        modifier = modifier,
    )
}

/**
 * One freedom rating, coloured by what its word says rather than by where its score sits.
 *
 * [FreedomRating] carries the reason that distinction matters: the top of each of these ladders
 * turns against the nation, so the highest scores in the game are "Frightening" and "Corrupted".
 */
@Composable
private fun rating(@StringRes labelRes: Int, value: String) = Fact(
    label = stringResource(labelRes),
    value = value,
    emphasis = when (FreedomRating.standing(value)) {
        FreedomRating.Standing.Good -> Emphasis.Good
        FreedomRating.Standing.Bad -> Emphasis.Bad
        FreedomRating.Standing.Middling -> Emphasis.None
    },
)

internal const val PAIRED = 2
private const val FREEDOM_COLUMNS = 3
