package dev.cazh0.civily.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** The app under measurement. Not derived from anything: this module exists to drive it. */
const val CIVILY = "dev.cazh0.civily"

/**
 * The journey every measurement here runs, written once.
 *
 * Why one journey shared by the profile generator and the benchmarks: a Baseline Profile is a
 * recording of the code a *typical* first session runs, so what it records and what is measured
 * afterwards have to be the same walk through the app. Two copies would drift, and the profile
 * would start covering a session nobody has.
 *
 * Why it is written in the user's own words — "Nation name", "Rankings" — rather than against
 * test tags: nothing in the app may exist for the benefit of the tests. Text is what a reader
 * sees and what TalkBack reads, so a journey written against it breaks only when the app changes
 * in a way a user would also notice.
 *
 * **Compose puts the words and the press on different nodes.** A `Tab`, a `LinkRow` or a fact
 * tile is a clickable node with a `Text` inside it, and the text node is not itself clickable —
 * verified by dumping the tree off the device rather than assumed. So everything here finds the
 * words and clicks *them*: the tap lands inside the control that owns them, which is what a
 * finger does too.
 *
 * Every step fails loudly and says which step it was. A profile recorded from a run that quietly
 * did half the journey is worse than no profile, because it looks like one (spec §1.2).
 */

/** A nation that has existed for as long as the game has. The site's own test subject. */
private const val TEST_NATION = "testlandia"

/**
 * The same nation as the app prints it, and the exact case matters.
 *
 * The search field still holds what was typed, so a substring match on "estlandia" finds the
 * field before it finds the answer — and pressing that types nothing and opens nothing while
 * looking like it worked. The result row is the only node carrying the *name*.
 */
private const val TEST_NATION_NAME = "Testlandia"

/** Long enough for a cold request over a slow connection; short enough to fail a hung one. */
private const val TIMEOUT_MILLIS = 15_000L

/**
 * Compose reports an editable field as an EditText, which is the only stable handle on it.
 *
 * Every selector here is confined to the app's own package. Without that, a step that runs while
 * the app is not in front searches whatever is — and a scrollable list of names is exactly what
 * the launcher's app drawer looks like to a matcher.
 */
private val SEARCH_FIELD: BySelector = By.pkg(CIVILY).clazz("android.widget.EditText")

/** Every tab of the nation screen, in the order the game puts them. */
private val TABS = listOf(
    "Overview",
    "Policies",
    "People",
    "Government",
    "Economy",
    "Rankings",
    "Happenings",
)

/** Search for a nation from the front door and open its screen. */
fun MacrobenchmarkScope.openNation() {
    val field = device.await(SEARCH_FIELD, "the nation search field")

    // Why the field is pressed before it is filled: setting text is an accessibility action and
    // does not move focus, so the Enter below would be delivered to whatever had focus instead —
    // which is nothing, and the search would never run. A finger focuses the field by tapping
    // it, and so does this.
    field.click()
    device.waitForIdle()
    field.text = TEST_NATION
    device.waitForIdle()

    // The field carries ImeAction.Search, so this is the same key the on-screen keyboard sends.
    device.pressEnter()

    // The result is a row carrying the nation's own name — which is the search having answered,
    // not merely having been asked.
    device.await(By.pkg(CIVILY).text(TEST_NATION_NAME), "the search result").click()

    // The tab bar is the nation screen and nothing else in the app has one.
    device.await(By.pkg(CIVILY).text(TABS.first()), "the nation screen")
    device.waitForIdle()
}

/**
 * Read the nation the way somebody who opened it does: down the page, back up, then through
 * every subject.
 *
 * Each pane is flung, which is what makes the profile cover the lazy lists rather than only the
 * first screenful of each.
 */
fun MacrobenchmarkScope.readEveryTab() {
    scrollPane()
    TABS.drop(1).forEach { label ->
        tab(label).click()
        device.waitForIdle()
        scrollPane()
    }
}

/** Down the current pane and back, at a real gesture's speed. */
fun MacrobenchmarkScope.scrollPane() {
    // Two things on this screen scroll: the tab bar across the top and the pane under it. The
    // pane is the tall one — picking by size rather than by position is what survives the bar
    // being pinned, collapsed or absent.
    val pane = device.scrollables().maxByOrNull { it.visibleBounds.height() } ?: return
    if (pane.visibleBounds.height() < device.displayHeight / 2) return

    // Without a margin the gesture starts at the edge of the display and the system claims it as
    // a back swipe.
    pane.setGestureMargin(device.displayWidth / 5)
    pane.fling(Direction.DOWN)
    device.waitForIdle()
    pane.fling(Direction.UP)
    device.waitForIdle()
}

/**
 * One tab of the nation screen, scrolled into reach if it is past the edge of the bar.
 *
 * Why the search is confined to the bar rather than run over the whole screen: "Government" and
 * "Economy" are also section headings inside the Overview, and a journey that pressed one of
 * those would silently stop moving between tabs while still passing.
 */
private fun MacrobenchmarkScope.tab(label: String): UiObject2 {
    device.waitForIdle()
    bar().findObject(By.text(label))?.let { return it }

    // Why the bar is rewound before it is walked: seven tabs, five on screen, and the bar is
    // wherever the last selection left it. Stepping right from an unknown position can only
    // search the tabs after that position — which is how looking for Policies while the bar sat
    // at Happenings failed, having scrolled further away from it each try.
    bar().scroll(Direction.LEFT, WHOLE_BAR)
    device.waitForIdle()

    repeat(TAB_STEPS) {
        bar().findObject(By.text(label))?.let { return it }
        bar().scroll(Direction.RIGHT, TAB_STEP)
        device.waitForIdle()
    }
    error("Walked the tab bar and never found the $label tab. Saw: ${visibleTabs()}")
}

/**
 * The tab bar: the shorter of the screen's two scrolling things, the other being the pane.
 * Chosen by size rather than position so it survives the top app bar collapsing or being gone.
 */
private fun MacrobenchmarkScope.bar(): UiObject2 =
    device.scrollables().minByOrNull { it.visibleBounds.height() }
        ?: error("The nation screen has no tab bar")

/** For the failure message, so a broken journey says what it was looking at. */
private fun MacrobenchmarkScope.visibleTabs(): List<String> =
    bar().findObjects(By.clazz("android.widget.TextView")).mapNotNull { it.text }

private const val TAB_STEPS = 4
private const val TAB_STEP = 0.5f
private const val WHOLE_BAR = 1.0f

private fun UiDevice.scrollables(): List<UiObject2> =
    findObjects(By.pkg(CIVILY).scrollable(true))

/**
 * Why this and not `device.wait`: a null from `wait` is a NullPointerException three lines later
 * naming nothing. The whole value of a failure here is knowing which step of the journey the app
 * did not reach.
 */
private fun UiDevice.await(selector: BySelector, what: String): UiObject2 =
    requireNotNull(wait(Until.findObject(selector), TIMEOUT_MILLIS)) {
        "Waited ${TIMEOUT_MILLIS}ms for $what and it never appeared"
    }
