package ro.fluierulfinal.navigation

import kotlinx.serialization.Serializable

// Type-safe routes for Navigation Compose: each screen is a @Serializable object or class.
// Arguments are plain properties (e.g. MatchDetailsRoute(fixtureId = 1575221)), so a wrong type
// is a compile error instead of a crash, unlike string routes such as "match/{id}".

// --- bottom bar ---
@Serializable data object FavoritesRoute
@Serializable data object ExploreRoute
@Serializable data object TicketRoute
@Serializable data object SettingsRoute

// --- reachable from the bottom-bar screens ---
@Serializable data object AddFavoriteRoute
@Serializable data class MatchDetailsRoute(val fixtureId: Int)
@Serializable data class TeamDetailsRoute(val teamId: Int)
@Serializable data class PlayerRoute(val playerId: Int)
@Serializable data object NearbyMatchesRoute
@Serializable data class AiAnalysisRoute(val fixtureId: Int)
@Serializable data object WeekendPlanRoute
@Serializable data object SimulationRoute
@Serializable data object ApiCoverageRoute
