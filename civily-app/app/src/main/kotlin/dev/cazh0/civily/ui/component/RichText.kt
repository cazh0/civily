package dev.cazh0.civily.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
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
    val openTarget = rememberOpenTarget(onOpenNation, onOpenRegion)
    val linkColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        blocks.forEach { block ->
            BlockView(block, linkColor, openTarget, onOpenNation, onOpenRegion)
        }
    }
}

/**
 * The same content as [RichText], one list item per block.
 *
 * Why it exists: a regional factbook is prose without a length limit — some run to hundreds of
 * paragraphs, and [RichText] lays out every one of them before the first is on screen. As items
 * of the screen's own list, the reader pays for what they can see. The caller's
 * `verticalArrangement` is the gap between blocks, which is why nothing here adds one.
 *
 * Nested prose — a quote, a bullet's contents — still goes through [RichText]: it is bounded by
 * the block it sits inside, and a lazy list cannot nest in its own axis anyway.
 */
fun LazyListScope.richTextItems(
    blocks: List<BbBlock>,
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
) {
    items(
        items = blocks,
        // Blocks are not reordered or removed, so the index is a stable identity. The type is
        // what lets the list reuse a paragraph's layout node for the next paragraph — and it is
        // `javaClass` rather than `::class` because the latter allocates a wrapper per call for
        // an answer compared by the very class it wraps.
        contentType = { block -> block.javaClass },
    ) { block ->
        val openTarget = rememberOpenTarget(onOpenNation, onOpenRegion)
        BlockView(
            block = block,
            linkColor = MaterialTheme.colorScheme.primary,
            openTarget = openTarget,
            onOpenNation = onOpenNation,
            onOpenRegion = onOpenRegion,
        )
    }
}

/**
 * Why this is remembered rather than written inline: it is a key to the [annotate] cache below,
 * and a lambda rebuilt on every recomposition would invalidate that cache on every recomposition
 * — which is the whole cost it exists to avoid.
 */
@Composable
private fun rememberOpenTarget(
    onOpenNation: (String) -> Unit,
    onOpenRegion: (String) -> Unit,
): (BbTarget) -> Unit {
    val uriHandler = LocalUriHandler.current
    return remember(uriHandler, onOpenNation, onOpenRegion) {
        { target: BbTarget ->
            when (target) {
                is BbTarget.Nation -> onOpenNation(target.id)
                is BbTarget.Region -> onOpenRegion(target.id)
                // Why guarded: a post can contain any string, and handing an unopenable one to
                // the system throws. A link that does nothing beats a crash inside someone's
                // factbook.
                is BbTarget.Web -> runCatching { uriHandler.openUri(target.url) }
            }
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
        // Why remembered: flattening spans into an AnnotatedString allocates a string builder,
        // a style per span and a link annotation per link, and none of it depends on anything
        // that changes between recompositions. Rebuilding it per frame is what turns a factbook
        // into a scroll that stutters.
        is BbBlock.Paragraph -> Text(
            text = remember(block, linkColor, openTarget) {
                annotate(block.spans, linkColor, openTarget)
            },
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
    // A stripe rather than a box: quotes nest, and nested boxes quickly leave no room for the
    // words.
    //
    // Why it is drawn rather than laid out: as a sibling Box it had no content of its own, so
    // its height could only come from the row — `IntrinsicSize.Min`, which measures the entire
    // quoted subtree an extra time to ask how tall it wants to be, and does it again for every
    // quote nested inside this one. Drawing the stripe behind the column costs one rounded rect
    // in the draw pass and no measurement at all, and the column is already exactly as tall as
    // the stripe has to be.
    val stripe = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val width = Dimens.QuoteBarWidth.toPx()
                drawRoundRect(
                    color = stripe,
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(width / 2f),
                )
            }
            .padding(start = Dimens.QuoteBarWidth + Dimens.ItemSpacing),
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
