package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.cazh0.civily.R
import dev.cazh0.civily.data.nation.Cause
import dev.cazh0.civily.data.nation.Nation
import dev.cazh0.civily.ui.component.RichText
import dev.cazh0.civily.ui.component.SectionHeader
import dev.cazh0.civily.ui.theme.ChartColors
import dev.cazh0.civily.ui.theme.Dimens

/**
 * Who lives here: what the nation is known for, what its people are like, and what kills them.
 *
 * Most of the summary is the API's own finished prose and none of it is rewritten. What this
 * composes are the sentences the site builds from loose shards: the nation is *admired* for one
 * thing and *remarkable* for others, its population has a temperament, and it has an animal and
 * a religion. Each appears only when every shard it needs came back, because half a sentence is
 * worse than none.
 *
 * The site opens with a population-size adjective — "a massive, efficient nation". No shard
 * carries it and nothing here invents one; see the README's gap list.
 */
@Composable
fun NationPeoplePane(
    nation: Nation,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val crime = nation.people.crime

    NationPane(modifier) {
        item(contentType = PaneContent.Prose) {
            val opening = listOfNotNull(nation.reputationSentence(), nation.peopleSentence())
                .joinToString(separator = " ")
            val closing = nation.animalSentence()

            if (opening.isNotEmpty() || crime.isNotEmpty() || closing != null) {
                Column(modifier = SectionGap) {
                    SectionHeader(stringResource(R.string.section_nation_summary))
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        if (opening.isNotEmpty()) {
                            Text(text = opening, style = MaterialTheme.typography.bodyLarge)
                        }
                        // Guarded rather than left to render nothing: an empty RichText is a
                        // zero-height child that the arrangement still spaces on both sides,
                        // which opens a gap in the middle of the paragraph.
                        if (crime.isNotEmpty()) {
                            RichText(
                                blocks = crime,
                                onOpenNation = onOpenNation,
                                onOpenRegion = onOpenRegion,
                            )
                        }
                        closing?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
        }

        if (nation.people.causesOfDeath.isNotEmpty()) {
            item(contentType = PaneContent.Chart) {
                Column(modifier = SectionGap) {
                    SectionHeader(stringResource(R.string.section_mortality))
                    DonutChart(
                        nation.people.causesOfDeath.mapIndexed { index, cause ->
                            Slice(
                                label = cause.label(nation.animal),
                                percent = cause.percent,
                                color = ChartColors.slice(index),
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * A cause of death, named for this nation's own animal where the API is generic.
 *
 * The API reports every mauling as "Animal Attack" no matter what the nation's animal is, so the
 * one cause of death a player chose for themselves is the one the chart would not name. The site
 * and the legacy client both substitute it; the animal is the player's own words, so it arrives
 * lower-case and stays that way.
 */
@Composable
private fun Cause.label(animal: String): String =
    if (name == ANIMAL_ATTACK && animal.isNotEmpty()) {
        stringResource(R.string.cause_animal_attack, animal)
    } else {
        name
    }

/** "The Hive Mind of Testlandia is environmentally stunning and remarkable for its …" */
@Composable
private fun Nation.reputationSentence(): String? {
    if (type.isEmpty() || name.isEmpty() || notable.isEmpty()) return null
    return if (admirable.isEmpty()) {
        stringResource(R.string.nation_summary_notable, type, name, notable)
    } else {
        stringResource(R.string.nation_summary_admired, type, name, admirable, notable)
    }
}

/** "Its population of 49.909 billion Testlandians is compassionate, democratic." */
@Composable
private fun Nation.peopleSentence(): String? {
    if (population <= 0 || demonymPlural.isEmpty() || sensibilities.isEmpty()) return null
    return stringResource(
        R.string.nation_summary_people,
        populationWords(population),
        demonymPlural,
        sensibilities,
    )
}

/** The API's own generic name for a mauling, whatever the nation's animal actually is. */
private const val ANIMAL_ATTACK = "Animal Attack"

/** "Testlandia's national animal is the nautilus, which …, and its national religion is …" */
@Composable
private fun Nation.animalSentence(): String? {
    if (name.isEmpty() || animal.isEmpty() || animalTrait.isEmpty() || religion.isEmpty()) {
        return null
    }
    return stringResource(R.string.nation_summary_animal, name, animal, animalTrait, religion)
}
