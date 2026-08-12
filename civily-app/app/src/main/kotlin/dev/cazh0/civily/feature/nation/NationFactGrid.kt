package dev.cazh0.civily.feature.nation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.cazh0.civily.ui.theme.Dimens
import dev.cazh0.civily.ui.theme.TrendColors

/**
 * One labelled fact in a grid.
 *
 * [onClick] is what makes a fact a door — the region a nation is in is a place you can go, and
 * the tile says so with a caret and the theme's link colour. A fact with no destination gets
 * neither, because a control that does nothing must not look like a control.
 */
data class Fact(
    val label: String,
    val value: String,
    /** Takes a row to itself. For the one figure that is the headline of its set. */
    val wide: Boolean = false,
    /** Whether this value is good or bad news. Most facts are neither. */
    val emphasis: Emphasis = Emphasis.None,
    val onClick: (() -> Unit)? = null,
)

/**
 * A judgement a tile may carry.
 *
 * Deliberately not a colour: the grid decides what good and bad look like, so a caller cannot
 * introduce a third green. [None] is the default because almost nothing is a judgement — a
 * currency is not good or bad, and colouring one would spend the reader's attention on nothing.
 */
enum class Emphasis { None, Good, Bad }

/**
 * Facts as a grid rather than a column of cards.
 *
 * Why a grid: these are short, parallel values, and one full-width card each turns eight of
 * them into eight screens of scrolling where the eye has to travel the width of the display to
 * read four words. Side by side they are one object the eye takes in at once, and the screen's
 * real content — the summary and the feed — starts above the fold instead of below it.
 *
 * Why hand-packed rows and not `LazyVerticalGrid`: this sits inside a scrolling column, and a
 * lazy grid nested in a scroll of the same axis has no bounded height to lay out in. Everything
 * here is on screen at once anyway — a nation has a fixed dozen facts, not a feed of them — so
 * laziness would cost complexity and buy nothing.
 *
 * A row is measured to its tallest tile so wrapped values do not step the row's baseline, and
 * `IntrinsicSize.Min` is how that is asked for rather than an oversight. The cheaper-looking
 * alternative — a custom `Layout` that measures each tile once, takes the tallest, and measures
 * the short ones again at that height — is not available: Compose throws on the second call
 * ("measure() may not be called multiple times on the same Measurable") and names intrinsics as
 * the supported way to ask a child how big it wants to be. The cost is one extra pass over a row
 * of two tiles, and it is paid when the row is laid out rather than per frame.
 */
@Composable
fun FactGrid(
    facts: List<Fact>,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    // A fact with nothing in it is dropped before packing, so a missing shard leaves no hole in
    // the middle of the grid — the tiles simply close up.
    val present = facts.filter { it.value.isNotEmpty() }
    if (present.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
    ) {
        present.pack(columns).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
            ) {
                row.forEach { fact ->
                    FactTile(
                        fact = fact,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                // A short last row keeps its tiles the width of every other tile. Stretching
                // them to fill would make the final fact look like a different kind of thing.
                if (!row.isWide()) {
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun FactTile(fact: Fact, modifier: Modifier) {
    val openable = fact.onClick != null

    val plate = MaterialTheme.colorScheme.surfaceContainerHigh
    val accent = when (fact.emphasis) {
        Emphasis.Good -> TrendColors.up
        Emphasis.Bad -> TrendColors.down
        Emphasis.None -> null
    }

    Surface(
        shape = RoundedCornerShape(Dimens.TileCornerRadius),
        // A judged tile is washed with its own accent rather than outlined or badged: the grid
        // is read by sweeping it, and a colour is the only thing that survives being glanced at.
        color = accent?.let { TrendColors.wash(it, plate) } ?: plate,
        modifier = modifier,
    ) {
        Column(
            // Why the press lands here rather than on the Surface's modifier: Surface clips its
            // content to the shape, so a ripple started inside it is round like the tile. On the
            // modifier it would be a rectangle sitting outside that clip.
            modifier = Modifier
                .then(
                    if (fact.onClick != null) Modifier.clickable(onClick = fact.onClick)
                    else Modifier,
                )
                .fillMaxSize()
                .padding(Dimens.TilePadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.TextSpacing),
        ) {
            Text(
                text = fact.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fact.value,
                    style = MaterialTheme.typography.titleMedium,
                    // The accent at full strength on its own wash: the same tone pairing the
                    // trend strip uses, and the one M3 contrast-checks for a light and a dark
                    // surface. A judged tile is never also a link, so these cannot collide.
                    color = accent
                        ?: if (openable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (openable) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        // The label and value already say where this goes; announcing the caret
                        // as well would read the tile twice.
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.PillIconSize),
                    )
                }
            }
        }
    }
}

/**
 * Facts in the order given, cut into rows of [columns] — except a wide one, which breaks the
 * row it lands in and takes the next to itself.
 */
private fun List<Fact>.pack(columns: Int): List<List<Fact>> {
    val rows = mutableListOf<List<Fact>>()
    val pending = mutableListOf<Fact>()

    fun flush() {
        if (pending.isNotEmpty()) {
            rows += pending.toList()
            pending.clear()
        }
    }

    forEach { fact ->
        if (fact.wide) {
            flush()
            rows += listOf(fact)
        } else {
            pending += fact
            if (pending.size == columns) flush()
        }
    }
    flush()
    return rows
}

private fun List<Fact>.isWide(): Boolean = singleOrNull()?.wide == true
