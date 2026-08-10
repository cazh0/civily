package dev.cazh0.civily.feature.issues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import coil.ImageLoader
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.Newspaper
import dev.cazh0.civily.data.issues.IssueResult
import dev.cazh0.civily.ui.component.NewspaperStack
import dev.cazh0.civily.ui.component.TrendPill
import dev.cazh0.civily.ui.theme.Dimens
import java.util.Locale
import kotlin.math.abs

/**
 * What the legislation did.
 *
 * Three sections, in the order a player cares about them: what the decision led to, what the
 * papers made of it, and what it did to the nation's numbers. There is no front page at the
 * top — the headlines *are* the newspapers, and printing one above them said the same thing
 * twice.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IssueResultView(
    result: IssueResult,
    masthead: String,
    edition: Newspaper.Edition,
    price: String?,
    flagUrl: String?,
    bannerUrl: String?,
    imageLoader: ImageLoader,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaleNames = stringArrayResource(R.array.census_scales)
    val scaleUnits = stringArrayResource(R.array.census_units)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        if (result.description.isNotBlank()) {
            item { Heading(stringResource(R.string.talking_point_heading)) }
            item {
                // The API's DESC is a sentence fragment in the game's voice — no leading
                // capital, no stop, because the site prints it inside a longer sentence.
                // Standing alone under a heading, it needs to be a sentence.
                Text(
                    text = result.description.asSentence(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                )
            }
        }

        if (result.headlines.isNotEmpty()) {
            item { Heading(stringResource(R.string.recent_headlines_heading)) }
            item {
                NewspaperStack(
                    headlines = result.headlines,
                    masthead = masthead,
                    edition = edition,
                    price = price,
                    flagUrl = flagUrl,
                    imageLoader = imageLoader,
                )
            }
        }

        if (result.rankings.isNotEmpty()) {
            item { Heading(stringResource(R.string.trends_heading)) }
            item {
                // An overview, not an audit: one decision nudges thirty-odd scales, and the
                // ones that moved by a rounding error are noise.
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    result.rankings
                        .sortedByDescending { abs(it.percentChange) }
                        .take(TRENDS_SHOWN)
                        .forEach { change ->
                            TrendPill(
                                name = scaleNames.getOrNull(change.scaleId)
                                    ?: stringResource(
                                        R.string.census_unknown_scale,
                                        change.scaleId,
                                    ),
                                unit = scaleUnits.getOrNull(change.scaleId).orEmpty(),
                                percentChange = change.percentChange,
                            )
                        }
                }
            }
        }

        item {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dimens.ScreenPadding,
                        end = Dimens.ScreenPadding,
                        top = Dimens.ItemSpacing,
                    ),
            ) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}

/** Section headings follow the theme, like every other heading in the app. */
@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            top = Dimens.ItemSpacing,
        ),
    )
}

private fun String.asSentence(): String =
    replaceFirstChar { it.titlecase(Locale.US) }
        .let { if (it.endsWith('.') || it.endsWith('!') || it.endsWith('?')) it else "$it." }

private const val TRENDS_SHOWN = 8
