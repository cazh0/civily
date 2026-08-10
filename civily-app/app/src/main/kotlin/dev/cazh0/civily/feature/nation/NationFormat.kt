package dev.cazh0.civily.feature.nation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.Magnitude
import dev.cazh0.civily.core.text.Numbers
import dev.cazh0.civily.core.text.Percent
import dev.cazh0.civily.core.text.Population
import dev.cazh0.civily.core.text.RelativeTime
import dev.cazh0.civily.data.nation.Department
import dev.cazh0.civily.data.nation.Sector

/**
 * The screen's numbers and the API's vocabulary, put into words.
 *
 * Everything here pairs a value from `core/text` with the string resource that names its unit.
 * It lives in one file because the same figure phrased two ways on one screen reads as two
 * different numbers — the population in the Overview grid and the population in the summary
 * sentence have to be the same words.
 */

/** The population as a person says it — "49.909 billion", never "49909". */
@Composable
internal fun populationWords(millions: Int): String {
    val scaled = Population.scale(millions)
    return stringResource(
        if (scaled.inBillions) {
            R.string.value_population_billions
        } else {
            R.string.value_population_millions
        },
        scaled.value,
    )
}

/** A figure too long to read, scaled — "3,315 trillion". */
@Composable
internal fun magnitudeWords(amount: Long): String {
    val scaled = Magnitude.scale(amount)
    val unitRes = when (scaled.unit) {
        Magnitude.Unit.Ones -> null
        Magnitude.Unit.Million -> R.string.value_million
        Magnitude.Unit.Billion -> R.string.value_billion
        Magnitude.Unit.Trillion -> R.string.value_trillion
    }
    return unitRes?.let { stringResource(it, scaled.value) } ?: scaled.value
}

/**
 * One World Census score.
 *
 * The ninety scales share no unit and no order of magnitude: a population score is
 * `49909000000`, a wealth gap is `1.48`. Scaling the huge ones and keeping two places on the
 * small ones is what lets a single column of them be read.
 */
@Composable
internal fun scoreWords(score: Double): String = when {
    score >= SCALE_FROM -> magnitudeWords(score.toLong())
    else -> Percent.rounded(score)
}

/**
 * "3 hours ago", in words, because a happening is a sentence and "3h" beside one reads as a
 * stray token. The plural lives in the resource layer, which is why [RelativeTime] hands back a
 * count and a unit instead of a string.
 */
@Composable
internal fun elapsedWords(epochSeconds: Long, now: Long): String {
    val (count, grain) = RelativeTime.elapsed(epochSeconds, now)
    return when (grain) {
        RelativeTime.Grain.Now -> stringResource(R.string.elapsed_now)
        RelativeTime.Grain.Minutes -> pluralStringResource(R.plurals.elapsed_minutes, count, count)
        RelativeTime.Grain.Hours -> pluralStringResource(R.plurals.elapsed_hours, count, count)
        RelativeTime.Grain.Days -> pluralStringResource(R.plurals.elapsed_days, count, count)
        RelativeTime.Grain.Weeks -> pluralStringResource(R.plurals.elapsed_weeks, count, count)
        RelativeTime.Grain.Years -> pluralStringResource(R.plurals.elapsed_years, count, count)
    }
}

/**
 * When a nation was founded.
 *
 * Zero is not a date — it is the API saying the nation predates its record, which the game itself
 * prints as "Antiquity". Rendering it as an elapsed time would claim the nation was founded on the
 * first of January 1970.
 */
@Composable
internal fun foundedWords(epochSeconds: Long, now: Long): String =
    if (epochSeconds <= 0) stringResource(R.string.value_antiquity) else elapsedWords(epochSeconds, now)

/** "#160,816" — the rank itself, grouped, because six-digit ranks are the common case. */
@Composable
internal fun rankWords(worldRank: Int): String =
    stringResource(R.string.value_rank, Numbers.grouped(worldRank))

/** "Top 0.09%" — the percentile the API reports, which is fractional at the top of a scale. */
@Composable
internal fun percentileWords(percentile: Double): String =
    stringResource(R.string.value_percentile, Percent.rounded(percentile))

@Composable
internal fun Department.words(): String = stringResource(
    when (this) {
        Department.Administration -> R.string.department_administration
        Department.Defence -> R.string.department_defence
        Department.Education -> R.string.department_education
        Department.Environment -> R.string.department_environment
        Department.Healthcare -> R.string.department_healthcare
        Department.Industry -> R.string.department_industry
        Department.InternationalAid -> R.string.department_international_aid
        Department.LawAndOrder -> R.string.department_law_and_order
        Department.PublicTransport -> R.string.department_public_transport
        Department.SocialPolicy -> R.string.department_social_policy
        Department.Spirituality -> R.string.department_spirituality
        Department.Welfare -> R.string.department_welfare
    },
)

@Composable
internal fun Sector.Kind.words(): String = stringResource(
    when (this) {
        Sector.Kind.Government -> R.string.sector_government
        Sector.Kind.StateOwned -> R.string.sector_state_owned
        Sector.Kind.PrivateIndustry -> R.string.sector_private
        Sector.Kind.BlackMarket -> R.string.sector_black_market
    },
)

/** Above a million a raw score stops being readable; below it, two decimals still are. */
private const val SCALE_FROM = 1_000_000.0
