package ro.fluierulfinal.data.repository

import kotlinx.coroutines.flow.StateFlow
import ro.fluierulfinal.domain.model.AlertSettings
import ro.fluierulfinal.domain.model.Favorite
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.Standings
import ro.fluierulfinal.domain.model.Team
import java.time.LocalDate

// The contracts the UI depends on. Each has a real implementation (API-Football + Supabase)
// and a Fake one with mock data; BuildConfig.USE_FAKE_DATA picks which one Hilt injects (RepositoryModule).

/** The signed-in user, as far as the app cares. */
data class AuthUser(val id: String, val email: String?)

/** Why an auth action failed, independent of the backend (Supabase or fake). The UI turns it into text. */
enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_NOT_CONFIRMED,
    USER_ALREADY_EXISTS,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    RATE_LIMITED,
    NETWORK,
    UNKNOWN,
}

class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(error.name, cause)

interface AuthRepository {
    /** null = signed out. Screens observe it to switch between Login and the app. */
    val currentUser: StateFlow<AuthUser?>

    /** True until the saved session (if any) has been loaded at app start. */
    val isRestoringSession: StateFlow<Boolean>

    /** @throws AuthException */
    suspend fun signIn(email: String, password: String)

    /**
     * Returns true if the account is ready to use, false if the user must first confirm their email.
     * @throws AuthException
     */
    suspend fun signUp(email: String, password: String): Boolean

    suspend fun signOut()
}

interface TeamRepository {
    /** Team search by name; [query] must have at least 3 characters (API-Football rule). */
    suspend fun search(query: String): List<Team>

    suspend fun getTeam(teamId: Int): Team
}

interface LeagueRepository {
    /** The leagues the app supports (fixed list, no API call). */
    fun supportedLeagues(): List<League>

    /** League details with `coverage`, from cache (7 days) or API-Football. */
    suspend fun getLeague(leagueId: Int): League
}

/** Where a list of fixtures came from; shown in the debug log and handy for the acceptance test. */
enum class DataSource { CACHE, NETWORK, STALE_CACHE }

data class FixturesResult(val fixtures: List<Fixture>, val source: DataSource)

/** The free API-Football plan only serves fixtures for yesterday, today and tomorrow. */
class DayNotAvailableException(val day: LocalDate) :
    Exception("Planul gratuit API-Football are acces doar la ieri, azi și mâine.")

interface FixtureRepository {
    /**
     * Fixtures of one league on one day. Cache first (6 hours; forever once all are finished),
     * otherwise one /fixtures?date= call that also fills the cache for every other supported league.
     * @throws DayNotAvailableException for days outside the free plan with nothing cached
     */
    suspend fun fixtures(leagueId: Int, day: LocalDate): FixturesResult
}

interface StandingsRepository {
    /** League table for [season]: memory → Supabase cache → one /standings call. */
    suspend fun standings(leagueId: Int, season: Int): Standings
}

interface FavoritesRepository {
    /** The user's favorites, newest first. Updated after every change below. */
    val favorites: StateFlow<List<Favorite>>

    /** (Re)loads the favorites of the signed-in user. */
    suspend fun refresh()

    suspend fun add(team: Team, leagueId: Int?)

    suspend fun updateAlerts(teamId: Int, alerts: AlertSettings)

    suspend fun remove(teamId: Int)

    /** Puts back a favorite exactly as it was (used by "Undo" after swipe to delete). */
    suspend fun restore(favorite: Favorite)
}
