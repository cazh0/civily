package dev.cazh0.civily.feature.issues

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.R
import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.core.text.Classification
import dev.cazh0.civily.core.text.FreedomLadder
import dev.cazh0.civily.core.text.Newspaper
import dev.cazh0.civily.data.issues.IssueResult
import dev.cazh0.civily.data.issues.Reclassification
import dev.cazh0.civily.ui.component.NewspaperStack
import dev.cazh0.civily.ui.component.PolicyCard
import dev.cazh0.civily.ui.component.TrendPill
import dev.cazh0.civily.ui.theme.Dimens
import java.util.Locale
import kotlin.math.abs

/**
 * What the legislation did.
 *
 * The order is the order a player cares about it: what the decision led to, what the nation is
 * now called, what the papers made of it, what changed in law, what it earned the nation, and
 * last what it did to the numbers. The talking point and the reclassification are one thought —
 * the sentence and the word it moved — so they sit together above the papers. There is no front
 * page at the top: the headlines *are* the newspapers, and printing one above them said the
 * same thing twice.
 *
 * Every section is absent when the answer carried nothing for it, which is the normal case:
 * most answers move statistics and print headlines without touching a policy.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IssueResultView(
    result: IssueResult,
    issueId: Int,
    masthead: String,
    nationName: String,
    demonym: String,
    edition: Newspaper.Edition,
    price: String?,
    flagUrl: String?,
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
                // The API's legacy result text is a sentence fragment, while the site aftermath
                // is already a sentence. Normalising here keeps both sources presentable.
                Text(
                    text = result.description.asSentence(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                )
            }
        }

        if (result.reclassifications.isNotEmpty()) {
            item { Heading(stringResource(R.string.reclassification_heading)) }
            items(
                // The nation's own title first, then what happened to its ratings: the
                // classification is the headline of this section and the ratings are why it
                // moved. The API sends them in whatever order it likes.
                items = result.reclassifications.sortedBy { it.rating.ordinal },
                key = { "reclassification-${it.rating}" },
            ) { reclassification ->
                ReclassificationRow(
                    reclassification = reclassification,
                    nationName = nationName,
                    demonym = demonym,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                )
            }
        }

        if (result.headlines.isNotEmpty()) {
            item { Heading(stringResource(R.string.recent_headlines_heading)) }
            item {
                NewspaperStack(
                    issueId = issueId,
                    headlines = result.headlines.map { it.text },
                    masthead = masthead,
                    edition = edition,
                    price = price,
                    flagUrl = flagUrl,
                    imageUrlsByHeadline = result.headlines.map { it.imageUrls },
                    imageLoader = imageLoader,
                )
            }
        }

        if (result.newPolicies.isNotEmpty()) {
            item { Heading(stringResource(R.string.new_policies_heading)) }
            items(
                items = result.newPolicies,
                key = { "new-policy-${it.name}" },
            ) { policy ->
                PolicyCard(
                    policy = policy,
                    imageLoader = imageLoader,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                )
            }
        }

        if (result.canceledPolicies.isNotEmpty()) {
            item { Heading(stringResource(R.string.canceled_policies_heading)) }
            items(
                items = result.canceledPolicies,
                key = { "canceled-policy-${it.name}" },
            ) { policy ->
                PolicyCard(
                    policy = policy,
                    imageLoader = imageLoader,
                    banner = false,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                )
            }
        }

        if (result.postcards.isNotEmpty()) {
            item { Heading(stringResource(R.string.postcards_heading, nationName)) }
            items(
                items = result.postcards,
                key = { "postcard-$it" },
            ) { bannerId ->
                Postcard(
                    bannerId = bannerId,
                    imageLoader = imageLoader,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
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

/**
 * One reclassification, written the way the game writes it.
 *
 * "Itagui Republic was reclassified from an **Inoffensive Centrist Democracy** to **Democratic
 * Socialists**." — the classification names the nation, because that is the nation's own title.
 * "The Itagui Republician Economy fell from **Reasonable** to **Developing**." — a rating names
 * the people, because it is a fact about them.
 *
 * The two words that changed are bold and nothing here is coloured. The sentence already says
 * which way it went, and a colour on top of it would answer a question the sentence did not
 * ask: whether the new rung is good news is `FreedomRating`'s judgement, and it disagrees with
 * the direction often enough — a rise into "Excessive" is still a rise — that showing both at
 * once reads as a contradiction rather than as two facts.
 *
 * The verb is [FreedomLadder]: which of the two words sits higher on the scale that produced
 * them. That is the one thing here that is a judgement about the scale rather than about the
 * word, and it is the game's own.
 */
@Composable
private fun ReclassificationRow(
    reclassification: Reclassification,
    nationName: String,
    demonym: String,
    modifier: Modifier = Modifier,
) {
    // Null names the classification, which is the nation's own title rather than one of its
    // ratings, and so is the one that takes the other sentence.
    val ratingRes = reclassification.rating.labelRes

    val sentence = if (ratingRes == null) {
        stringResource(
            R.string.reclassification_government,
            nationName,
            withArticle(reclassification.from),
            withArticle(reclassification.to),
        )
    } else {
        stringResource(
            when (FreedomLadder.move(reclassification.from, reclassification.to)) {
                FreedomLadder.Move.Rose -> R.string.reclassification_rose
                FreedomLadder.Move.Fell -> R.string.reclassification_fell
                FreedomLadder.Move.Changed -> R.string.reclassification_changed
            },
            demonym,
            stringResource(ratingRes),
            reclassification.from.emphasised(),
            reclassification.to.emphasised(),
        )
    }

    Text(
        text = remember(sentence) { sentence.withEmphasis() },
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth(),
    )
}

/** The article goes outside the emphasis: "from an **Inoffensive Centrist Democracy**". */
@Composable
private fun withArticle(classification: String): String {
    val emphasised = classification.emphasised()
    return when (Classification.article(classification)) {
        Classification.Article.A -> stringResource(R.string.article_a, emphasised)
        Classification.Article.An -> stringResource(R.string.article_an, emphasised)
        Classification.Article.None -> emphasised
    }
}

/**
 * Marks a run for bolding before it is formatted into a sentence.
 *
 * Why markers and not `AnnotatedString.fromHtml` on a resource carrying `<b>`: the same
 * sentence also formats in the nation's demonym, which is text its owner typed. An `&` in it
 * would reach the HTML parser as the start of an entity and take the rest of the sentence with
 * it. Control characters cannot arrive from a NationStates field, so nothing the game sends can
 * turn the emphasis on or off by accident.
 */
private fun String.emphasised(): String = "$EMPHASIS_ON$this$EMPHASIS_OFF"

/** Turns the marked runs into bold spans and drops the markers. */
private fun String.withEmphasis(): AnnotatedString = buildAnnotatedString {
    var start = -1
    this@withEmphasis.forEach { char ->
        when (char) {
            EMPHASIS_ON -> start = length
            EMPHASIS_OFF -> {
                if (start >= 0) addStyle(BoldSpan, start, length)
                start = -1
            }

            else -> append(char)
        }
    }
}

private val BoldSpan = SpanStyle(fontWeight = FontWeight.Bold)

/** START OF TEXT and END OF TEXT. Written as escapes so they survive a copy-paste. */
private const val EMPHASIS_ON = '\u0002'
private const val EMPHASIS_OFF = '\u0003'

/** Null for the classification, which is named in its own sentence rather than labelled. */
@get:StringRes
private val Reclassification.Rating.labelRes: Int?
    get() = when (this) {
        Reclassification.Rating.CivilRights -> R.string.rating_civil_rights
        Reclassification.Rating.Economy -> R.string.rating_economy
        Reclassification.Rating.PoliticalFreedom -> R.string.rating_political_freedom
        Reclassification.Rating.Government -> null
    }

/**
 * A piece of artwork the nation has just earned, and nothing else on the card.
 *
 * No caption: `UNLOCKS` gives an artwork code and no title, and the heading above already says
 * whose postcard this is. No content description for the same reason — there is no text the
 * app holds that describes the picture, and inventing one would describe it wrong.
 */
@Composable
private fun Postcard(bannerId: String, imageLoader: ImageLoader, modifier: Modifier = Modifier) {
    AsyncImage(
        model = NsUrl.banner(bannerId),
        contentDescription = null,
        imageLoader = imageLoader,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.PostcardHeight)
            .clip(RoundedCornerShape(Dimens.TileCornerRadius)),
    )
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
