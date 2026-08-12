package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.R
import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.data.nation.Policy
import dev.cazh0.civily.ui.component.SectionHeader
import dev.cazh0.civily.ui.theme.Dimens

/**
 * The laws this nation has on the books, grouped the way the game groups them.
 *
 * Why grouped and not one flat list: the API returns policies in category order already —
 * Government, Society, Law & Order, Economy, International — and a heading over each turns a
 * scroll of twenty cards into five short answers to "what kind of country is this".
 *
 * Why lazy: this is the one pane whose length is unbounded, and every card pulls a banner over
 * the network. A `Column` would fetch all twenty images to show the first three.
 */
@Composable
fun NationPoliciesPane(
    policies: List<Policy>,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    if (policies.isEmpty()) {
        // A brand-new nation has enacted nothing. Not a failure, so not dressed as one.
        Text(
            text = stringResource(R.string.policies_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(Dimens.ScreenPadding),
        )
        return
    }

    val groups = remember(policies) { policies.groupIntoRuns() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        groups.forEachIndexed { index, group ->
            if (group.category.isNotEmpty()) {
                item(key = "category-$index", contentType = PaneContent.Header) {
                    SectionHeader(
                        text = group.category,
                        // Breathing room above every heading but the first, so the groups read
                        // as groups rather than as one long ladder.
                        modifier = Modifier.padding(
                            top = if (index == 0) Dimens.TextSpacing else Dimens.ItemSpacing,
                        ),
                    )
                }
            }
            items(
                items = group.policies,
                key = { it.name },
                contentType = { PaneContent.Policy },
            ) { policy ->
                PolicyCard(policy, imageLoader)
            }
        }
    }
}

@Composable
private fun PolicyCard(policy: Policy, imageLoader: ImageLoader) {
    Surface(
        shape = RoundedCornerShape(Dimens.TileCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            if (policy.imageId.isNotEmpty()) {
                // Cropped to fill, like every other piece of artwork in the app: a Rift banner
                // is wide and decorative, and fitting one leaves plate above and below it. No
                // content description — the policy's name is directly beneath it.
                AsyncImage(
                    model = NsUrl.banner(policy.imageId),
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.PolicyBannerHeight),
                )
            }
            Column(
                modifier = Modifier.padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
            ) {
                Text(text = policy.name, style = MaterialTheme.typography.titleMedium)
                if (policy.description.isNotEmpty()) {
                    Text(
                        text = policy.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private class PolicyGroup(val category: String, val policies: List<Policy>)

/**
 * Policies cut into the consecutive runs that share a category.
 *
 * Runs rather than `groupBy`: grouping would silently merge a category the API listed in two
 * places, which moves a policy away from the neighbours the game put it with. The order on
 * screen is the order on the wire.
 */
private fun List<Policy>.groupIntoRuns(): List<PolicyGroup> {
    val runs = mutableListOf<Pair<String, MutableList<Policy>>>()
    forEach { policy ->
        val open = runs.lastOrNull()
        if (open != null && open.first == policy.category) {
            open.second += policy
        } else {
            runs += policy.category to mutableListOf(policy)
        }
    }
    return runs.map { (category, policies) -> PolicyGroup(category, policies) }
}
