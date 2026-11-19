package ro.fluierulfinal.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ro.fluierulfinal.ui.debug.ApiCoverageScreen
import ro.fluierulfinal.ui.explore.ExploreLeagueScreen
import ro.fluierulfinal.ui.explore.ExploreScreen
import ro.fluierulfinal.ui.favorites.AddFavoriteScreen
import ro.fluierulfinal.ui.favorites.FavoritesScreen
import ro.fluierulfinal.ui.placeholder.PlaceholderScreen
import ro.fluierulfinal.ui.settings.AccountSection
import ro.fluierulfinal.ui.settings.SettingsLink
import ro.fluierulfinal.ui.settings.SettingsScreen
import kotlin.reflect.KClass

/** One tab of the bottom bar. */
private data class TopLevelTab(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector)

private val TABS = listOf(
    TopLevelTab(FavoritesRoute, FavoritesRoute::class, "Favorite", Icons.Filled.Star),
    TopLevelTab(ExploreRoute, ExploreRoute::class, "Explorează", Icons.Filled.Search),
    TopLevelTab(TicketRoute, TicketRoute::class, "Biletul meu", Icons.AutoMirrored.Filled.List),
    TopLevelTab(SettingsRoute, SettingsRoute::class, "Setări", Icons.Filled.Settings),
)

/**
 * The app's navigation graph. The bottom bar is shown only on the four top-level screens;
 * deeper screens (details, placeholders) take the whole height and have a back arrow.
 */
@Composable
fun AppNavHost(
    userEmail: String?,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = TABS.any { tab -> destination?.hasRoute(tab.routeClass) == true }

    Scaffold(
        // Each screen draws its own top bar and handles the status bar; this Scaffold only adds the bottom bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = FavoritesRoute,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            val back: () -> Unit = { navController.popBackStack() }

            // --- bottom bar ---
            composable<FavoritesRoute> {
                FavoritesScreen(
                    onAddTeam = { navController.navigate(AddFavoriteRoute) },
                    onOpenTeam = { teamId -> navController.navigate(TeamDetailsRoute(teamId)) },
                )
            }
            composable<ExploreRoute> {
                ExploreScreen(onLeagueClick = { leagueId -> navController.navigate(ExploreLeagueRoute(leagueId)) })
            }
            composable<TicketRoute> {
                PlaceholderScreen("Biletul meu", "S12", "Meciurile de pe bilet, programul de urmărire și starea fiecărui pariu, live.")
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    plannedScreens = listOf(
                        SettingsLink("Detalii meci", "Scor, evenimente, statistici, echipe de start", "S10") { navController.navigate(MatchDetailsRoute(fixtureId = 0)) },
                        SettingsLink("Detalii echipă", "Clasament, formă, golgheteri, meciuri viitoare", "S10") { navController.navigate(TeamDetailsRoute(teamId = 0)) },
                        SettingsLink("Meciuri în zonă", "Listă și hartă cu stadioanele din rază (GPS)", "S10") { navController.navigate(NearbyMatchesRoute) },
                        SettingsLink("Simulare", "Gol, roșu, penalty, echipe de start, pentru demo", "S10") { navController.navigate(SimulationRoute) },
                        SettingsLink("Plan weekend", "Planul generat miercurea, editabil", "S12") { navController.navigate(WeekendPlanRoute) },
                        SettingsLink("Analiză AI", "Analiza meciului și chat, din Detalii meci", "S12") { navController.navigate(AiAnalysisRoute(fixtureId = 0)) },
                        SettingsLink("Jucător", "Profilul unui jucător", "S12") { navController.navigate(PlayerRoute(playerId = 0)) },
                    ),
                    debugTools = listOf(
                        SettingsLink("Test acoperire API", "Plan, id-uri ligi, coverage (3 cereri)", null) { navController.navigate(ApiCoverageRoute) },
                    ),
                    accountSection = { AccountSection(userEmail, onSignOut) },
                )
            }

            // --- deeper screens ---
            composable<ExploreLeagueRoute> {
                ExploreLeagueScreen(onBack = back, onFixtureClick = { id -> navController.navigate(MatchDetailsRoute(id)) })
            }
            composable<AddFavoriteRoute> {
                AddFavoriteScreen(onBack = back)
            }
            composable<MatchDetailsRoute> { entry ->
                val route = entry.toRoute<MatchDetailsRoute>()
                PlaceholderScreen("Detalii meci", "S10", "Scor, cronologia evenimentelor, statistici, echipe de start și butonul „Analiză AI”.\n(meciul #${route.fixtureId})", back)
            }
            composable<TeamDetailsRoute> { entry ->
                val route = entry.toRoute<TeamDetailsRoute>()
                PlaceholderScreen("Detalii echipă", "S10", "Clasament, formă, goluri acasă / deplasare, golgheteri, meciuri viitoare.\n(echipa #${route.teamId})", back)
            }
            composable<PlayerRoute> {
                PlaceholderScreen("Jucător", "S12", "Profilul și statisticile unui jucător.", back)
            }
            composable<NearbyMatchesRoute> {
                PlaceholderScreen("Meciuri în zonă", "S10", "Listă și hartă cu stadioanele din raza aleasă, după GPS.", back)
            }
            composable<AiAnalysisRoute> {
                PlaceholderScreen("Analiză AI", "S12", "Analiza meciului pe statistici reale și chat despre meci.", back)
            }
            composable<WeekendPlanRoute> {
                PlaceholderScreen("Plan weekend", "S12", "Planul generat miercurea: ce meci vezi în fiecare slot, la stadion sau la TV.", back)
            }
            composable<SimulationRoute> {
                PlaceholderScreen("Simulare", "S10", "Simulează gol, roșu, penalty sau echipe de start, ca să demonstrezi alertele.", back)
            }
            composable<ApiCoverageRoute> {
                ApiCoverageScreen(onBack = back)
            }
        }
    }
}

/**
 * Switching tabs: keep one copy of each tab on the back stack and remember its state
 * (scroll position, typed text) when coming back to it.
 */
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
