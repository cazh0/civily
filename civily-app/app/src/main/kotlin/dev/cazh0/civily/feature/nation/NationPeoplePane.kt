package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.cazh0.civily.R
import dev.cazh0.civily.data.nation.Nation
import dev.cazh0.civily.ui.component.RichText
import dev.cazh0.civily.ui.component.SectionHeader
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
    val opening = listOfNotNull(nation.reputationSentence(), nation.peopleSentence())
        .joinToString(separator = " ")
    val closing = nation.animalSentence()
    val crime = nation.people.crime

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        if (opening.isNotEmpty() || crime.isNotEmpty() || closing != null) {
            Column {
                SectionHeader(stringResource(R.string.section_nation_summary))
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    if (opening.isNotEmpty()) {
                        Text(text = opening, style = MaterialTheme.typography.bodyLarge)
                    }
                    // Guarded rather than left to render nothing: an empty RichText is a
                    // zero-height child that the arrangement still spaces on both sides, which
                    // opens a gap in the middle of the paragraph.
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

        if (nation.people.causesOfDeath.isNotEmpty()) {
            Column {
                SectionHeader(stringResource(R.string.section_mortality))
                ShareBars(nation.people.causesOfDeath.map { Share(it.name, it.percent) })
            }
        }
    }
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

/** "Testlandia's national animal is the nautilus, which …, and its national religion is …" */
@Composable
private fun Nation.animalSentence(): String? {
    if (name.isEmpty() || animal.isEmpty() || animalTrait.isEmpty() || religion.isEmpty()) {
        return null
    }
    return stringResource(R.string.nation_summary_animal, name, animal, animalTrait, religion)
}
