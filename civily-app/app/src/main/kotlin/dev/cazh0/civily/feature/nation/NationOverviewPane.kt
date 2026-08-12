package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import coil.ImageLoader
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.NsId
import dev.cazh0.civily.core.text.Numbers
import dev.cazh0.civily.core.text.Percent
import dev.cazh0.civily.data.nation.Nation
import dev.cazh0.civily.ui.component.FlagHero
import dev.cazh0.civily.ui.theme.Dimens

/**
 * The nation at a glance: its flag, its name, and the facts every other tab elaborates on.
 *
 * The grids are the same five subjects the legacy client's Overview card stack shows, in the same
 * order — standing, freedoms, government, economy, everything else — because that order is what
 * a reader of the game's own nation page already knows. What changes is the shape: tiles side by
 * side instead of a column of full-width cards, so the whole overview is two thumb-scrolls
 * rather than eight.
 *
 * Each block is one item of the pane's list, so opening this tab composes the flag and the first
 * grid rather than all five of them.
 */
@Composable
fun NationOverviewPane(
    nation: Nation,
    now: Long,
    imageLoader: ImageLoader,
    onOpenRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NationPane(modifier) {
        item(contentType = PaneContent.Identity) {
            Column(
                modifier = SectionGap,
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
            }
        }

        item(contentType = PaneContent.Facts) {
            FactSection(
                titleRes = R.string.section_at_a_glance,
                // Population takes a row to itself because it is the figure people came for, and
                // because it is the one value here long enough to wrap in half a screen.
                facts = listOf(
                    Fact(
                        label = stringResource(R.string.label_population),
                        value = populationWords(nation.population),
                        wide = true,
                    ),
                    Fact(
                        label = stringResource(R.string.label_region),
                        value = nation.region,
                        onClick = nation.region
                            .takeIf { it.isNotEmpty() }
                            ?.let { region -> { onOpenRegion(NsId.fromName(region)) } },
                    ),
                    Fact(stringResource(R.string.label_category), nation.category),
                    Fact(stringResource(R.string.label_wa_status), nation.waStatus),
                    Fact(stringResource(R.string.label_influence), nation.influence),
                    // Both are hidden at zero rather than shown as "0": a nation nobody endorses
                    // is the ordinary case outside the World Assembly, and a tile saying so is
                    // noise.
                    Fact(
                        label = stringResource(R.string.label_endorsements),
                        value = nation.endorsementCount
                            .takeIf { it > 0 }
                            ?.let { Numbers.grouped(it) }
                            .orEmpty(),
                    ),
                    Fact(
                        label = stringResource(R.string.label_issues_answered),
                        value = nation.issuesAnswered
                            .takeIf { it > 0 }
                            ?.let { Numbers.grouped(it) }
                            .orEmpty(),
                    ),
                    Fact(
                        label = stringResource(R.string.label_founded),
                        value = foundedWords(nation.foundedEpochSeconds, now),
                    ),
                    Fact(
                        label = stringResource(R.string.label_last_active),
                        value = nation.lastActiveEpochSeconds
                            .takeIf { it > 0 }
                            ?.let { elapsedWords(it, now) }
                            .orEmpty(),
                    ),
                ),
                columns = PAIRED,
            )
        }

        item(contentType = PaneContent.Facts) { FreedomSection(nation) }

        item(contentType = PaneContent.Facts) {
            FactSection(
                titleRes = R.string.section_government,
                facts = listOf(
                    Fact(stringResource(R.string.label_leader), nation.leader),
                    Fact(stringResource(R.string.label_capital), nation.capital),
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
        }

        item(contentType = PaneContent.Facts) {
            FactSection(
                titleRes = R.string.section_economy,
                facts = listOf(
                    Fact(stringResource(R.string.label_currency), nation.currency),
                    Fact(
                        label = stringResource(R.string.label_gdp),
                        value = nation.economy.gdp
                            .takeIf { it > 0 }
                            ?.let { magnitudeWords(it) }
                            .orEmpty(),
                    ),
                    Fact(
                        label = stringResource(R.string.label_average_income),
                        value = nation.economy.averageIncome
                            .takeIf { it > 0 }
                            ?.let { Numbers.grouped(it) }
                            .orEmpty(),
                    ),
                    Fact(
                        label = stringResource(R.string.label_major_industry),
                        value = nation.economy.majorIndustry,
                    ),
                ),
                columns = PAIRED,
            )
        }

        item(contentType = PaneContent.Facts) {
            FactSection(
                titleRes = R.string.section_other,
                facts = listOf(
                    Fact(
                        label = stringResource(R.string.label_demonym),
                        value = if (nation.demonym.isEmpty() || nation.demonymPlural.isEmpty()) {
                            ""
                        } else {
                            stringResource(
                                R.string.value_demonym,
                                nation.demonym,
                                nation.demonymPlural,
                            )
                        },
                    ),
                    Fact(stringResource(R.string.label_religion), nation.religion),
                    // Wide because a national animal is a player's own words and runs long:
                    // Testlandia's is "★★★ nautilus ★★★".
                    Fact(
                        label = stringResource(R.string.label_animal),
                        value = nation.animal,
                        wide = true,
                    ),
                ),
                columns = PAIRED,
            )
        }
    }
}
