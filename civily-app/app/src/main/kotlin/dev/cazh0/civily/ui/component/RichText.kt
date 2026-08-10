package dev.cazh0.civily.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import dev.cazh0.civily.R
import dev.cazh0.civily.core.text.NsId
import dev.cazh0.civily.core.text.bbcode.BbBlock
import dev.cazh0.civily.core.text.bbcode.BbColor
import dev.cazh0.civily.core.text.bbcode.BbSpan
import dev.cazh0.civily.core.text.bbcode.BbStyle
import dev.cazh0.civily.core.text.bbcode.BbTarget
import dev.cazh0.civily.ui.theme.Dimens

/**
 * Renders parsed NationStates BBCode.
 *
 * Nation and region links stay inside the app; a web link leaves it. Nothing here can render
 * raw markup, because by this point there is none left — [dev.cazh0.civily.core.text.bbcode.BbParser]
 * has already turned it into a tree or dropped it.
 */
@Composable
fun RichText(
    blocks: List<BbBlock>,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary

    val openTarget: (BbTarget) -> Unit = { target ->
        when (target) {
            is BbTarget.Nation -> onOpenNation(target.id)
            is BbTarget.Region -> onOpenRegion(target.id)
            // Why guarded: a post can contain any string, and handing an unopenable one to the
            // system throws. A link that does nothing beats a crash inside someone's factbook.
            is BbTarget.Web -> runCatching { uriHandler.openUri(target.url) }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        blocks.forEach { block ->
            BlockView(block, linkColor, openTarget, onOpenNation, onOpenRegion)
        }
    }
}

@Composable
private fun BlockView(
    block: BbBlock,
    linkColor: Color,
    openTarget: (BbTarget) -> Unit,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
) {
    when (block) {
        is BbBlock.Paragraph -> Text(
            text = annotate(block.spans, linkColor, openTarget),
            style = MaterialTheme.typography.bodyLarge,
        )

        BbBlock.Rule -> HorizontalDivider()

        is BbBlock.Preformatted -> Surface(
            shape = RoundedCornerShape(Dimens.FlagCornerRadius),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Why it scrolls sideways: preformatted text is usually ASCII art or a table
            // whose alignment is the content. Wrapping it destroys the thing it is.
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                softWrap = false,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(Dimens.CardPadding),
            )
        }

        is BbBlock.Quote -> QuoteView(block, linkColor, openTarget, onOpenNation, onOpenRegion)

        is BbBlock.Spoiler -> SpoilerView(block, onOpenNation, onOpenRegion)

        is BbBlock.Bullets -> Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            block.items.forEachIndexed { index, item ->
                Row {
                    Text(
                        text = if (block.ordered) "${index + 1}." else "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(Dimens.BulletGutter),
                    )
                    RichText(
                        blocks = item,
                        onOpenNation = onOpenNation,
                        onOpenRegion = onOpenRegion,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuoteView(
    quote: BbBlock.Quote,
    linkColor: Color,
    openTarget: (BbTarget) -> Unit,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
) {
    // Why IntrinsicSize.Min: the stripe has no content of its own, so it can only take its
    // height from the row. Without this the row is as tall as the text and the stripe is
    // zero-height — present in the tree, invisible on screen.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        // A stripe rather than a box: quotes nest, and nested boxes quickly leave no room
        // for the words.
        Box(
            modifier = Modifier
                .width(Dimens.QuoteBarWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(Dimens.QuoteBarWidth))
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            quote.author?.let { author ->
                Text(
                    text = stringResource(R.string.quote_author, author),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    // The author is written as a display name; the link needs the id.
                    modifier = Modifier.clickable {
                        openTarget(BbTarget.Nation(NsId.fromName(author)))
                    },
                )
            }
            RichText(quote.blocks, onOpenNation, onOpenRegion)
        }
    }
}

@Composable
private fun SpoilerView(
    spoiler: BbBlock.Spoiler,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
) {
    // Why collapsed by default and why the state is saveable: hiding the text is the entire
    // purpose of a spoiler, and a rotation that reveals it defeats it.
    var expanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(Dimens.FlagCornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = spoiler.title ?: stringResource(R.string.spoiler_default_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = stringResource(
                        if (expanded) R.string.action_hide else R.string.action_reveal,
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                RichText(
                    blocks = spoiler.blocks,
                    onOpenNation = onOpenNation,
                    onOpenRegion = onOpenRegion,
                    modifier = Modifier.padding(top = Dimens.ItemSpacing),
                )
            }
        }
    }
}

/**
 * Flattens spans into one styled string, with links the platform knows are links — so
 * TalkBack announces them and the touch target is the text itself.
 */
private fun annotate(
    spans: List<BbSpan>,
    linkColor: Color,
    openTarget: (BbTarget) -> Unit,
): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        val base = span.style.toSpanStyle()
        when (span) {
            is BbSpan.Plain -> withStyle(base) { append(span.text) }

            is BbSpan.Link -> withLink(
                LinkAnnotation.Clickable(
                    tag = span.text,
                    linkInteractionListener = { openTarget(span.target) },
                ),
            ) {
                withStyle(
                    base.copy(
                        // The author's own colour wins if they set one; otherwise a link
                        // looks like a link.
                        color = base.color.takeIf { it != Color.Unspecified } ?: linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(span.text)
                }
            }
        }
    }
}

private fun BbStyle.toSpanStyle(): SpanStyle = SpanStyle(
    fontWeight = if (bold) FontWeight.Bold else null,
    fontStyle = if (italic) FontStyle.Italic else null,
    color = color?.let(BbColor::parse)?.let(::Color) ?: Color.Unspecified,
    textDecoration = decoration(),
    baselineShift = when {
        superscript -> BaselineShift.Superscript
        subscript -> BaselineShift.Subscript
        else -> null
    },
    // Superscripts and subscripts sit smaller as well as higher; only shifting them looks
    // broken. `em` keeps it relative, so it still works at any font scale.
    fontSize = if (superscript || subscript) 0.75.em else TextUnit.Unspecified,
)

private fun BbStyle.decoration(): TextDecoration? = when {
    underline && strikethrough ->
        TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))

    underline -> TextDecoration.Underline
    strikethrough -> TextDecoration.LineThrough
    else -> null
}
