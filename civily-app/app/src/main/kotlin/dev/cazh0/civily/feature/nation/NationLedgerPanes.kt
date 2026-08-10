package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.Numbers
import dev.cazh0.civily.core.text.Percent
import dev.cazh0.civily.data.nation.Nation
import dev.cazh0.civily.ui.component.SectionHeader
import dev.cazh0.civily.ui.theme.Dimens

/**
 * The two panes that are a paragraph and a chart: where the money comes from, and where it goes.
 *
 * Both open with the game's own finished prose — `GOVTDESC` and `INDUSTRYDESC` — because the game
 * has already written the sentence a chart would need a caption for. The figures follow, then the
 * shares. That is the legacy client's order for these two tabs, and it is the right one: the
 * paragraph tells you what kind of country this is, and the bars tell you by how much.
 */
@Composable
fun NationGovernmentPane(
    nation: Nation,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        ProseSection(
            titleRes = R.string.section_nation_summary,
            blocks = nation.government.description,
            onOpenNation = onOpenNation,
            onOpenRegion = onOpenRegion,
        )

        FactSection(
            titleRes = R.string.section_government,
            facts = listOf(
                Fact(stringResource(R.string.label_govt_priority), nation.government.priority),
                Fact(
                    label = stringResource(R.string.label_tax),
                    value = stringResource(
                        R.string.value_percent,
                        Percent.rounded(nation.government.taxPercent),
                    ),
                ),
            ),
            columns = PAIRED,
        )

        // Why percentages and no total in the nation's own money: the API publishes the split of
        // the budget, not its size. The legacy client multiplies GDP by the government's share of
        // the economy and labels the result "Total", which is a different quantity wearing the
        // budget's name. The share of the economy is a real figure and it is on the Economy tab.
        if (nation.government.budget.isNotEmpty()) {
            Column {
                SectionHeader(stringResource(R.string.section_expenditures))
                ShareBars(
                    nation.government.budget.map { spend ->
                        Share(spend.department.words(), spend.percent)
                    },
                )
            }
        }
    }
}

@Composable
fun NationEconomyPane(
    nation: Nation,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val economy = nation.economy

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        ProseSection(
            titleRes = R.string.section_nation_summary,
            blocks = economy.description,
            onOpenNation = onOpenNation,
            onOpenRegion = onOpenRegion,
        )

        FactSection(
            titleRes = R.string.section_economy,
            // The currency sits with the figures rather than in a corner of the Overview,
            // because every number in this grid is denominated in it and none of them say so.
            facts = listOf(
                Fact(stringResource(R.string.label_currency), nation.currency),
                Fact(
                    label = stringResource(R.string.label_gdp),
                    value = economy.gdp.takeIf { it > 0 }?.let { magnitudeWords(it) }.orEmpty(),
                ),
                Fact(stringResource(R.string.label_major_industry), economy.majorIndustry),
                Fact(
                    label = stringResource(R.string.label_average_income),
                    value = economy.averageIncome.money(),
                ),
                Fact(stringResource(R.string.label_poorest), economy.poorestIncome.money()),
                Fact(stringResource(R.string.label_richest), economy.richestIncome.money()),
            ),
            columns = PAIRED,
        )

        if (economy.sectors.isNotEmpty()) {
            Column {
                SectionHeader(stringResource(R.string.section_sectors))
                ShareBars(economy.sectors.map { Share(it.kind.words(), it.percent) })
            }
        }
    }
}

/** An income, or nothing at all when the API had none — never a tile reading "0". */
private fun Long.money(): String = if (this > 0) Numbers.grouped(this) else ""
