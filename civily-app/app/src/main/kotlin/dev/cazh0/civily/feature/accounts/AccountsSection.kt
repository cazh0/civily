package dev.cazh0.civily.feature.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.cazh0.civily.R
import dev.cazh0.civily.core.session.Accounts
import dev.cazh0.civily.core.session.Session
import dev.cazh0.civily.core.text.Initials
import dev.cazh0.civily.core.text.NsId
import dev.cazh0.civily.ui.component.LinkRow
import dev.cazh0.civily.ui.component.SectionHeader
import dev.cazh0.civily.ui.theme.Dimens
import dev.cazh0.civily.ui.theme.avatarColor

/**
 * Your nation, and every other nation you hold a login for, in one card.
 *
 * The card *is* the switcher. Closed it is the thing the user came for: their flag, their
 * name, their region, and a press that opens their dashboard. The caret at its edge opens the
 * rest of the accounts inside the same card — no menu in a corner, no second section
 * underneath, nothing that appears somewhere the user was not already looking. Choosing
 * another nation folds the card back with that nation now at its head, which is the whole
 * gesture: one press to open, one to become someone else.
 *
 * Switching costs no request. NationStates issues a session per nation, so each account keeps
 * its own token and PIN, and the next request simply carries a different pair.
 */
@Composable
fun AccountsSection(
    accounts: Accounts,
    imageLoader: ImageLoader,
    onOpenNation: (String) -> Unit,
    onOpenIssues: () -> Unit,
    onSwitch: (String) -> Unit,
    onForget: (String) -> Unit,
    onAddNation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (accounts.all.isEmpty()) {
        SignedOutCard(onSignIn = onAddNation, modifier = modifier)
        return
    }

    var expanded by rememberSaveable { mutableStateOf(false) }

    // Why only one at a time: two rows mid-confirmation is two nations the user is half-way
    // through removing, with no way to tell which one the next tap lands on.
    var armedId by rememberSaveable { mutableStateOf<String?>(null) }

    val active = accounts.active
    val others = accounts.all.filterNot { it.nationId == accounts.activeId }

    // Why it cannot be closed with nobody signed in: there is no active nation to fold into,
    // and a card whose only content is a control for revealing its content is a riddle.
    val open = expanded || active == null

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        if (active != null) SectionHeader(stringResource(R.string.section_your_nation))

        NationCard(
            active = active,
            others = others,
            accounts = accounts,
            imageLoader = imageLoader,
            open = open,
            armedId = armedId,
            onToggle = {
                expanded = !expanded
                armedId = null
            },
            onArm = { armedId = it },
            onCancel = { armedId = null },
            onOpenNation = onOpenNation,
            onSwitch = {
                armedId = null
                expanded = false
                onSwitch(it)
            },
            onForget = {
                armedId = null
                onForget(it)
            },
            onAddNation = onAddNation,
        )

        if (active != null) {
            LinkRow(
                name = stringResource(R.string.title_issues),
                subtitle = stringResource(R.string.subtitle_issues),
                flagUrl = "",
                imageLoader = imageLoader,
                onClick = onOpenIssues,
            )
        }
    }
}

@Composable
private fun NationCard(
    active: Session?,
    others: List<Session>,
    accounts: Accounts,
    imageLoader: ImageLoader,
    open: Boolean,
    armedId: String?,
    onToggle: () -> Unit,
    onArm: (String) -> Unit,
    onCancel: () -> Unit,
    onOpenNation: (String) -> Unit,
    onSwitch: (String) -> Unit,
    onForget: (String) -> Unit,
    onAddNation: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            if (active == null) {
                Text(
                    text = stringResource(R.string.accounts_choose),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = Dimens.CardPadding,
                        end = Dimens.CardPadding,
                        top = Dimens.CardPadding,
                    ),
                )
            } else {
                AccountRow(
                    session = active,
                    imageLoader = imageLoader,
                    onPress = { onOpenNation(active.nationId) },
                    enabled = armedId == null,
                ) {
                    ExpandButton(open = open, onClick = onToggle)
                }
            }

            AnimatedVisibility(visible = open) {
                Column {
                    others.forEach { session ->
                        val removeLabel = stringResource(
                            R.string.action_remove_account,
                            session.nationName,
                        )

                        HorizontalDivider()
                        AccountRow(
                            session = session,
                            imageLoader = imageLoader,
                            // Folding the card back is what says the switch happened: the
                            // nation the user pressed is now the one at its head.
                            onPress = { onSwitch(session.nationId) },
                            enabled = armedId == null,
                        ) {
                            ArmButton(
                                label = removeLabel,
                                armed = armedId == session.nationId,
                                onClick = { onArm(session.nationId) },
                            )
                        }
                        RemoveConfirmation(
                            visible = armedId == session.nationId,
                            commitLabel = removeLabel,
                            onCancel = onCancel,
                            onCommit = { onForget(session.nationId) },
                        )
                    }

                    HorizontalDivider()

                    CardActions(
                        hasRoom = accounts.hasRoom,
                        signedInAs = active,
                        signOutArmed = active != null && armedId == active.nationId,
                        onAddNation = onAddNation,
                        onArmSignOut = { active?.let { onArm(it.nationId) } },
                    )

                    if (active != null) {
                        RemoveConfirmation(
                            visible = armedId == active.nationId,
                            commitLabel = stringResource(
                                R.string.action_sign_out_of,
                                active.nationName,
                            ),
                            onCancel = onCancel,
                            onCommit = { onForget(active.nationId) },
                        )
                    }
                }
            }
        }
    }
}

/** Flag, name, region, and whatever control belongs at the end of this particular row. */
@Composable
private fun AccountRow(
    session: Session,
    imageLoader: ImageLoader,
    onPress: () -> Unit,
    enabled: Boolean,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // A row with a confirmation open somewhere in the card must not also be a button:
            // the next press belongs to that confirmation, not to a change of nation.
            .clickable(enabled = enabled, onClick = onPress)
            .padding(Dimens.ItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        AccountFlag(session, imageLoader)

        Column(Modifier.weight(1f)) {
            Text(
                text = session.nationName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val regionId = session.regionId
            if (regionId != null) {
                Text(
                    text = NsId.toName(regionId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        trailing()
    }
}

/** The one control that opens the card. It turns over, so its state is never in doubt. */
@Composable
private fun ExpandButton(open: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (open) HALF_TURN else 0f,
        label = "accounts caret",
    )

    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(R.string.action_show_nations),
            modifier = Modifier.rotate(rotation),
        )
    }
}

/** Arms a removal. The commit that follows names the nation, so this only ever asks. */
@Composable
private fun ArmButton(label: String, armed: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = !armed) {
        Icon(imageVector = Icons.Default.Close, contentDescription = label)
    }
}

/**
 * Adding a nation, and leaving one.
 *
 * Sign-out sits here rather than beside the active nation's name because the card's head must
 * mean one thing — this is your nation, press it to open it — in both of its states.
 */
@Composable
private fun CardActions(
    hasRoom: Boolean,
    signedInAs: Session?,
    signOutArmed: Boolean,
    onAddNation: () -> Unit,
    onArmSignOut: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Dimens.TextSpacing)) {
        // A control that cannot do anything must not look like a control, so the ceiling is a
        // sentence rather than a greyed row the user presses twice before reading it.
        if (!hasRoom) {
            Text(
                text = stringResource(R.string.accounts_full, Accounts.MAX),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = Dimens.ItemSpacing,
                    vertical = Dimens.TextSpacing,
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasRoom) {
                TextButton(onClick = onAddNation) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.action_add_nation),
                        modifier = Modifier.padding(start = Dimens.TextSpacing),
                    )
                }
            } else {
                // Holds the left edge so sign-out stays at the right whether or not there is
                // room to add another nation.
                Spacer(Modifier)
            }

            if (signedInAs != null) {
                TextButton(onClick = onArmSignOut, enabled = !signOutArmed) {
                    Text(stringResource(R.string.action_sign_out))
                }
            }
        }
    }
}

/**
 * The second of two separately aimed actions.
 *
 * Throwing away the only copy of a login token cannot be undone, so it takes arming and then a
 * commit that says which nation it is about to forget. A dialog repeating that same sentence
 * would be a third tap without a third thought, which is how people learn to dismiss dialogs
 * unread.
 */
@Composable
private fun RemoveConfirmation(
    visible: Boolean,
    commitLabel: String,
    onCancel: () -> Unit,
    onCommit: () -> Unit,
) {
    AnimatedVisibility(visible = visible) {
        Column(
            modifier = Modifier.padding(
                start = Dimens.ItemSpacing,
                end = Dimens.ItemSpacing,
                bottom = Dimens.ItemSpacing,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            Text(
                text = stringResource(R.string.remove_account_explainer),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = onCommit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = commitLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * The nation's flag, or its initials when the sign-in response carried none.
 *
 * Both land in the same 3:2 box, so a row never changes shape depending on what the API
 * happened to send. The flag is cropped to fill that box rather than fitted inside it: filled,
 * the artwork is the tile, and there is no plate left to look like a container.
 */
@Composable
private fun AccountFlag(session: Session, imageLoader: ImageLoader) {
    val hasFlag = session.flagUrl.isNotEmpty()

    Surface(
        shape = RoundedCornerShape(Dimens.FlagCornerRadius),
        color = if (hasFlag) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            avatarColor(session.nationId)
        },
        modifier = Modifier.size(Dimens.FlagThumbnailWidth, Dimens.FlagThumbnailHeight),
    ) {
        if (hasFlag) {
            AsyncImage(
                model = session.flagUrl,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = Initials.of(session.nationName),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }
    }
}

/** The first sign-in. Nothing is stored yet, so there is nothing to switch between. */
@Composable
private fun SignedOutCard(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Text(
                text = stringResource(R.string.signed_out_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.signed_out_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onSignIn) {
                Text(stringResource(R.string.action_sign_in))
            }
        }
    }
}

private const val HALF_TURN = 180f
