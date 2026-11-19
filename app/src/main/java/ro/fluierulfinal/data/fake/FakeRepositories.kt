package ro.fluierulfinal.data.fake

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ro.fluierulfinal.data.repository.AuthError
import ro.fluierulfinal.data.repository.AuthException
import ro.fluierulfinal.data.repository.AuthRepository
import ro.fluierulfinal.data.repository.AuthUser
import ro.fluierulfinal.data.repository.DataSource
import ro.fluierulfinal.data.repository.FavoritesRepository
import ro.fluierulfinal.data.repository.FixtureRepository
import ro.fluierulfinal.data.repository.FixturesResult
import ro.fluierulfinal.data.repository.LeagueRepository
import ro.fluierulfinal.data.repository.TeamRepository
import ro.fluierulfinal.domain.model.AlertSettings
import ro.fluierulfinal.domain.model.AppTime
import ro.fluierulfinal.domain.model.Favorite
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.LeagueCoverage
import ro.fluierulfinal.domain.model.Score
import ro.fluierulfinal.domain.model.SupportedLeagues
import ro.fluierulfinal.domain.model.Team
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// Fake repositories with mock data, used when BuildConfig.USE_FAKE_DATA = true.
// No network at all: the UI can be built and demoed without spending API requests.
// A short delay() imitates the network, so loading states are visible.

private const val FAKE_DELAY_MS = 400L

/** Real API-Football ids, so logos load from the media server (free). */
internal object FakeData {
    private fun team(id: Int, name: String, country: String) =
        Team(id, name, "https://media.api-sports.io/football/teams/$id.png", country = country)

    val teamsByLeague: Map<Int, List<Team>> = mapOf(
        283 to listOf(team(6231, "Rapid", "Romania"), team(559, "FCSB", "Romania"), team(632, "Universitatea Craiova", "Romania"),
            team(2246, "CFR 1907 Cluj", "Romania"), team(635, "Dinamo Bucuresti", "Romania"), team(2596, "Farul Constanta", "Romania")),
        284 to listOf(team(2606, "FC Politehnica Timisoara", "Romania"), team(20033, "Bihor Oradea", "Romania"),
            team(20034, "Corvinul Hunedoara", "Romania"), team(2604, "Metaloglobus", "Romania")),
        39 to listOf(team(40, "Liverpool", "England"), team(42, "Arsenal", "England"), team(49, "Chelsea", "England"), team(55, "Brentford", "England")),
        140 to listOf(team(529, "Barcelona", "Spain"), team(541, "Real Madrid", "Spain"), team(536, "Sevilla", "Spain"), team(548, "Real Sociedad", "Spain")),
        135 to listOf(team(489, "AC Milan", "Italy"), team(505, "Inter", "Italy"), team(492, "Napoli", "Italy"), team(487, "Lazio", "Italy")),
        78 to listOf(team(157, "Bayern München", "Germany"), team(165, "Borussia Dortmund", "Germany"), team(173, "RB Leipzig", "Germany"), team(168, "Bayer Leverkusen", "Germany")),
        61 to listOf(team(85, "Paris Saint Germain", "France"), team(80, "Lyon", "France"), team(81, "Marseille", "France"), team(91, "Monaco", "France")),
        2 to listOf(team(529, "Barcelona", "Spain"), team(157, "Bayern München", "Germany"), team(40, "Liverpool", "England"), team(505, "Inter", "Italy")),
    )

    val allTeams: List<Team> = teamsByLeague.values.flatten().distinctBy { it.id }

    fun leagueOf(teamId: Int): Int? = teamsByLeague.entries.firstOrNull { (id, teams) -> id != 2 && teams.any { it.id == teamId } }?.key
}

@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {
    private val _user = MutableStateFlow<AuthUser?>(null)
    override val currentUser: StateFlow<AuthUser?> = _user.asStateFlow()
    override val isRestoringSession: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun signIn(email: String, password: String) {
        delay(FAKE_DELAY_MS)
        if (password.length < 6) throw AuthException(AuthError.INVALID_CREDENTIALS)
        _user.value = AuthUser("fake-user", email)
    }

    override suspend fun signUp(email: String, password: String): Boolean {
        signIn(email, password)
        return true
    }

    override suspend fun signOut() {
        _user.value = null
    }
}

@Singleton
class FakeTeamRepository @Inject constructor() : TeamRepository {
    override suspend fun search(query: String): List<Team> {
        require(query.trim().length >= 3) { "Search needs at least 3 characters" }
        delay(FAKE_DELAY_MS)
        return FakeData.allTeams.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    override suspend fun getTeam(teamId: Int): Team =
        FakeData.allTeams.firstOrNull { it.id == teamId } ?: error("Team $teamId not found")
}

@Singleton
class FakeLeagueRepository @Inject constructor() : LeagueRepository {
    override fun supportedLeagues(): List<League> = SupportedLeagues.all

    override suspend fun getLeague(leagueId: Int): League {
        delay(FAKE_DELAY_MS)
        val base = SupportedLeagues.all.firstOrNull { it.id == leagueId } ?: error("League $leagueId not supported")
        return base.copy(coverage = LeagueCoverage(events = true, lineups = true, statisticsFixtures = true))
    }
}

/**
 * Generates the same fixtures for the same league and day every time (seeded random).
 * Past days are finished, today has a mix (finished, live, scheduled), future days are scheduled.
 */
@Singleton
class FakeFixtureRepository @Inject constructor(private val clock: Clock) : FixtureRepository {

    override suspend fun fixtures(leagueId: Int, day: LocalDate): FixturesResult {
        delay(FAKE_DELAY_MS)
        val teams = FakeData.teamsByLeague[leagueId].orEmpty()
        val today = AppTime.today(clock.instant())
        val rnd = Random(leagueId * 100_000 + day.toEpochDay().toInt())
        if (teams.size < 2 || rnd.nextInt(5) == 0) return FixturesResult(emptyList(), DataSource.NETWORK) // some days have no matches

        val kickoffs = listOf(LocalTime.of(15, 0), LocalTime.of(18, 0), LocalTime.of(20, 30))
        val fixtures = teams.shuffled(rnd).chunked(2).filter { it.size == 2 }.mapIndexed { i, (home, away) ->
            val kickoff = day.atTime(kickoffs[i % kickoffs.size]).atZone(AppTime.ZONE).toInstant()
            val status = when {
                day < today -> FixtureStatus.FINISHED
                day > today -> FixtureStatus.SCHEDULED
                else -> listOf(FixtureStatus.FINISHED, FixtureStatus.LIVE, FixtureStatus.SCHEDULED)[i % 3]
            }
            val score = if (status == FixtureStatus.SCHEDULED) Score(null, null) else Score(rnd.nextInt(4), rnd.nextInt(3))
            Fixture(
                id = leagueId * 1000 + day.dayOfYear * 10 + i, leagueId = leagueId, season = SupportedLeagues.CURRENT_SEASON,
                kickoff = kickoff, status = status,
                elapsedMinutes = if (status == FixtureStatus.LIVE) 30 + rnd.nextInt(55) else null,
                homeTeam = home, awayTeam = away, score = score,
            )
        }
        return FixturesResult(fixtures, DataSource.NETWORK)
    }
}

@Singleton
class FakeFavoritesRepository @Inject constructor(private val clock: Clock) : FavoritesRepository {
    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    override val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun add(team: Team, leagueId: Int?) {
        if (_favorites.value.any { it.teamId == team.id }) return
        val favorite = Favorite(team.id, team.name, team.country, team.logoUrl, leagueId ?: FakeData.leagueOf(team.id), clock.instant())
        _favorites.update { listOf(favorite) + it }
    }

    override suspend fun updateAlerts(teamId: Int, alerts: AlertSettings) =
        _favorites.update { list -> list.map { if (it.teamId == teamId) it.copy(alerts = alerts) else it } }

    override suspend fun remove(teamId: Int) = _favorites.update { list -> list.filterNot { it.teamId == teamId } }

    override suspend fun restore(favorite: Favorite) =
        _favorites.update { list -> (list + favorite).sortedByDescending { it.addedAt } }
}
