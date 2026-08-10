package dev.cazh0.civily.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.cazh0.civily.core.graph
import dev.cazh0.civily.feature.issues.IssueDetailScreen
import dev.cazh0.civily.feature.issues.IssuesScreen
import dev.cazh0.civily.feature.issues.IssuesViewModel
import dev.cazh0.civily.feature.lookup.LookupScreen
import dev.cazh0.civily.feature.nation.NationScreen
import dev.cazh0.civily.feature.region.RegionScreen
import dev.cazh0.civily.feature.rmb.RmbScreen
import dev.cazh0.civily.feature.signin.SignInScreen
import dev.cazh0.civily.feature.wa.WaScreen

/**
 * Every destination in the app, and the only place route strings are written.
 *
 * Why routes are built here rather than inline at call sites: a typo in a navigate() string
 * is a runtime crash. Funnelling them through [Routes.nation] and [Routes.region] makes the
 * compiler catch it.
 */
object Routes {
    const val LOOKUP = "lookup"
    const val SIGN_IN = "sign-in"
    const val WORLD_ASSEMBLY = "world-assembly"
    const val ISSUES = "issues"
    const val ARG_ISSUE_ID = "issueId"
    const val ISSUE_PATTERN = "issue/{$ARG_ISSUE_ID}"

    fun issue(issueId: Int) = "issue/$issueId"

    const val ARG_NATION_ID = "nationId"
    const val NATION_PATTERN = "nation/{$ARG_NATION_ID}"

    const val ARG_REGION_ID = "regionId"
    const val REGION_PATTERN = "region/{$ARG_REGION_ID}"
    const val RMB_PATTERN = "rmb/{$ARG_REGION_ID}"

    fun nation(nationId: String) = "nation/$nationId"

    fun region(regionId: String) = "region/$regionId"

    fun rmb(regionId: String) = "rmb/$regionId"
}

@Composable
fun CivilyNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOOKUP) {
        composable(Routes.LOOKUP) { entry ->
            val nav = navActions(navController, entry)
            LookupScreen(
                onOpenNation = nav.openNation,
                onOpenWorldAssembly = { nav.go(Routes.WORLD_ASSEMBLY) },
                onOpenIssues = { nav.go(Routes.ISSUES) },
                onSignIn = nav.openSignIn,
            )
        }

        composable(Routes.SIGN_IN) { entry ->
            val nav = navActions(navController, entry)
            SignInScreen(
                onSignedIn = { id ->
                    // Why popUpTo: a signed-in user pressing back must not land on the form
                    // they just completed.
                    nav.go(Routes.nation(id)) {
                        popUpTo(Routes.SIGN_IN) { inclusive = true }
                    }
                },
                onBack = nav.back,
            )
        }

        composable(
            route = Routes.NATION_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_NATION_ID) { type = NavType.StringType }),
        ) { entry ->
            val nav = navActions(navController, entry)
            NationScreen(
                nationId = entry.requireArg(Routes.ARG_NATION_ID),
                onOpenRegion = nav.openRegion,
                onSignIn = nav.openSignIn,
                onBack = nav.back,
            )
        }

        composable(
            route = Routes.REGION_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_REGION_ID) { type = NavType.StringType }),
        ) { entry ->
            val nav = navActions(navController, entry)
            RegionScreen(
                regionId = entry.requireArg(Routes.ARG_REGION_ID),
                onOpenNation = nav.openNation,
                onOpenRegion = nav.openRegion,
                onOpenMessageBoard = { id -> nav.go(Routes.rmb(id)) },
                onSignIn = nav.openSignIn,
                onBack = nav.back,
            )
        }

        composable(
            route = Routes.RMB_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_REGION_ID) { type = NavType.StringType }),
        ) { entry ->
            val nav = navActions(navController, entry)
            RmbScreen(
                regionId = entry.requireArg(Routes.ARG_REGION_ID),
                onOpenNation = nav.openNation,
                onOpenRegion = nav.openRegion,
                onSignIn = nav.openSignIn,
                onBack = nav.back,
            )
        }

        composable(Routes.ISSUES) { entry ->
            val nav = navActions(navController, entry)
            IssuesScreen(
                viewModel = issuesViewModel(navController, entry),
                onOpenIssue = { id -> nav.go(Routes.issue(id)) },
                onSignIn = nav.openSignIn,
                onBack = nav.back,
            )
        }

        composable(
            route = Routes.ISSUE_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_ISSUE_ID) { type = NavType.IntType }),
        ) { entry ->
            val nav = navActions(navController, entry)
            IssueDetailScreen(
                issueId = requireNotNull(entry.arguments?.getInt(Routes.ARG_ISSUE_ID)) {
                    "Route ${Routes.ISSUE_PATTERN} reached without ${Routes.ARG_ISSUE_ID}"
                },
                viewModel = issuesViewModel(navController, entry),
                onOpenNation = nav.openNation,
                onOpenRegion = nav.openRegion,
                onBack = nav.back,
            )
        }

        composable(Routes.WORLD_ASSEMBLY) { entry ->
            val nav = navActions(navController, entry)
            WaScreen(
                onOpenNation = nav.openNation,
                onOpenRegion = nav.openRegion,
                onSignIn = nav.openSignIn,
                onBack = nav.back,
            )
        }
    }
}

/**
 * The navigation one screen may perform, refused once that screen is on its way out.
 *
 * Why every destination goes through this: a destination stays composed — and keeps taking
 * taps — for the whole of its exit transition, so a tap that lands a frame after the user
 * pressed back is delivered to a screen that is no longer on the back stack. Acting on it
 * navigates *from* a popped entry: pressing back on the issue list and then tapping a headline
 * pushed an issue detail with no list beneath it, and [issuesViewModel] — which looks that list
 * up to share one ViewModel across the pair — then asked for an entry that had gone. The same
 * stale tap pushes a second copy of any screen a user double-taps.
 *
 * The test is that the entry is still the one on top of the back stack — which is what "the
 * destination the user is on" means, and is the same thing the crash was reporting when it
 * named the current destination. The lifecycle answer, `RESUMED`, would hold this crash off
 * too, but it ties the permission to how long an animation runs: an entry is not RESUMED until
 * its *arrival* transition has finished, so a screen that has drawn refuses to be used until
 * then, and that window grows with any transition added later. This test has no window at all.
 * A screen may be moved from the moment it exists; it may not be moved once something else has
 * taken its place.
 */
private class NavActions(
    private val controller: NavHostController,
    private val entry: NavBackStackEntry,
) {
    val openNation: (String) -> Unit = { id -> go(Routes.nation(id)) }
    val openRegion: (String) -> Unit = { id -> go(Routes.region(id)) }
    val openSignIn: () -> Unit = { go(Routes.SIGN_IN) }
    val back: () -> Unit = { if (current()) controller.navigateUp() }

    fun go(route: String, options: NavOptionsBuilder.() -> Unit = {}) {
        if (current()) controller.navigate(route, options)
    }

    private fun current() = controller.currentBackStackEntry == entry
}

@Composable
private fun navActions(controller: NavHostController, entry: NavBackStackEntry): NavActions =
    remember(entry) { NavActions(controller, entry) }

/**
 * Why this fails loudly: the argument is declared non-optional on the route, so a null here
 * means the route and its composable have drifted apart. Crashing at the seam beats rendering
 * an empty screen nobody can explain (spec §2 R1).
 */
private fun NavBackStackEntry.requireArg(key: String): String =
    requireNotNull(arguments?.getString(key)) { "Route reached without argument '$key'" }

/**
 * One [IssuesViewModel] shared by the list and the detail, scoped to the list's back stack
 * entry.
 *
 * Why: the list already holds every issue in full, so opening one should not cost a request
 * or show a spinner over data the app is holding. Scoping to the list entry also means the
 * pair is discarded together when the user leaves issues entirely.
 *
 * The list is on the back stack whenever the detail is: [NavActions] is what makes that true,
 * by refusing to open an issue from a list the user has already left.
 */
@Composable
private fun issuesViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): IssuesViewModel {
    val owner = remember(entry) { navController.getBackStackEntry(Routes.ISSUES) }
    val graph = LocalContext.current.graph
    return viewModel(
        viewModelStoreOwner = owner,
        factory = IssuesViewModel.factory(graph.issuesRepository),
    )
}
