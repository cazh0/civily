package dev.cazh0.stately.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.cazh0.stately.core.graph
import dev.cazh0.stately.feature.issues.IssueDetailScreen
import dev.cazh0.stately.feature.issues.IssuesScreen
import dev.cazh0.stately.feature.issues.IssuesViewModel
import dev.cazh0.stately.feature.lookup.LookupScreen
import dev.cazh0.stately.feature.nation.NationScreen
import dev.cazh0.stately.feature.region.RegionScreen
import dev.cazh0.stately.feature.rmb.RmbScreen
import dev.cazh0.stately.feature.signin.SignInScreen
import dev.cazh0.stately.feature.wa.WaScreen

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
fun StatelyNavHost(navController: NavHostController = rememberNavController()) {
    val openNation: (String) -> Unit = { id -> navController.navigate(Routes.nation(id)) }
    val openRegion: (String) -> Unit = { id -> navController.navigate(Routes.region(id)) }
    val openSignIn: () -> Unit = { navController.navigate(Routes.SIGN_IN) }

    NavHost(navController = navController, startDestination = Routes.LOOKUP) {
        composable(Routes.LOOKUP) {
            LookupScreen(
                onOpenNation = openNation,
                onOpenWorldAssembly = { navController.navigate(Routes.WORLD_ASSEMBLY) },
                onOpenIssues = { navController.navigate(Routes.ISSUES) },
                onSignIn = openSignIn,
            )
        }

        composable(Routes.SIGN_IN) {
            SignInScreen(
                onSignedIn = { id ->
                    // Why popUpTo: a signed-in user pressing back must not land on the form
                    // they just completed.
                    navController.navigate(Routes.nation(id)) {
                        popUpTo(Routes.SIGN_IN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.NATION_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_NATION_ID) { type = NavType.StringType }),
        ) { entry ->
            NationScreen(
                nationId = entry.requireArg(Routes.ARG_NATION_ID),
                onOpenRegion = openRegion,
                onSignIn = openSignIn,
                onBack = navController::navigateUp,
            )
        }

        composable(
            route = Routes.REGION_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_REGION_ID) { type = NavType.StringType }),
        ) { entry ->
            RegionScreen(
                regionId = entry.requireArg(Routes.ARG_REGION_ID),
                onOpenNation = openNation,
                onOpenRegion = openRegion,
                onOpenMessageBoard = { id -> navController.navigate(Routes.rmb(id)) },
                onSignIn = openSignIn,
                onBack = navController::navigateUp,
            )
        }

        composable(
            route = Routes.RMB_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_REGION_ID) { type = NavType.StringType }),
        ) { entry ->
            RmbScreen(
                regionId = entry.requireArg(Routes.ARG_REGION_ID),
                onOpenNation = openNation,
                onOpenRegion = openRegion,
                onSignIn = openSignIn,
                onBack = navController::navigateUp,
            )
        }

        composable(Routes.ISSUES) { entry ->
            IssuesScreen(
                viewModel = issuesViewModel(navController, entry),
                onOpenIssue = { id -> navController.navigate(Routes.issue(id)) },
                onSignIn = openSignIn,
                onBack = navController::navigateUp,
            )
        }

        composable(
            route = Routes.ISSUE_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_ISSUE_ID) { type = NavType.IntType }),
        ) { entry ->
            IssueDetailScreen(
                issueId = requireNotNull(entry.arguments?.getInt(Routes.ARG_ISSUE_ID)) {
                    "Route ${Routes.ISSUE_PATTERN} reached without ${Routes.ARG_ISSUE_ID}"
                },
                viewModel = issuesViewModel(navController, entry),
                onOpenNation = openNation,
                onOpenRegion = openRegion,
                onBack = navController::navigateUp,
            )
        }

        composable(Routes.WORLD_ASSEMBLY) {
            WaScreen(
                onOpenNation = openNation,
                onOpenRegion = openRegion,
                onSignIn = openSignIn,
                onBack = navController::navigateUp,
            )
        }
    }
}

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
