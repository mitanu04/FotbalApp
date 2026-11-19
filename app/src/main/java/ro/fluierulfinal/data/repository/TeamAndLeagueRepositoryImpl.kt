package ro.fluierulfinal.data.repository

import android.util.Log
import ro.fluierulfinal.data.cache.CacheDataSource
import ro.fluierulfinal.data.cache.CachePolicy
import ro.fluierulfinal.data.remote.ApiFootballService
import ro.fluierulfinal.data.remote.mapper.toDomain
import ro.fluierulfinal.data.remote.orThrow
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.SupportedLeagues
import ro.fluierulfinal.domain.model.Team
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepositoryImpl @Inject constructor(
    private val api: ApiFootballService,
    private val cache: CacheDataSource,
    private val clock: Clock,
) : TeamRepository {

    // Search results per query for this app session: typing "rapid", deleting and retyping it costs one request.
    private val searchCache = mutableMapOf<String, List<Team>>()

    override suspend fun search(query: String): List<Team> {
        val q = query.trim().lowercase()
        require(q.length >= 3) { "Search needs at least 3 characters" }
        searchCache[q]?.let { return it }

        val teams = api.searchTeams(q).orThrow().map { it.toDomain() }
        searchCache[q] = teams
        // also fill the shared teams cache, so getTeam() for these teams costs nothing later
        runCatching { cache.putTeams(teams, clock.instant()) }.onFailure { Log.w(TAG, "team cache write failed: ${it.safeDescription()}") }
        return teams
    }

    override suspend fun getTeam(teamId: Int): Team {
        val cached = runCatching { cache.getTeam(teamId) }.getOrNull()
        if (cached != null && CachePolicy.isStaticDataFresh(cached.updatedAt, clock.instant())) return cached.value

        val team = api.team(teamId).orThrow().firstOrNull()?.toDomain()
            ?: return cached?.value ?: error("Team $teamId not found")
        runCatching { cache.putTeams(listOf(team), clock.instant()) }.onFailure { Log.w(TAG, "team cache write failed: ${it.safeDescription()}") }
        return team
    }

    private companion object {
        const val TAG = "TeamRepo"
    }
}

@Singleton
class LeagueRepositoryImpl @Inject constructor(
    private val api: ApiFootballService,
    private val cache: CacheDataSource,
    private val clock: Clock,
) : LeagueRepository {

    override fun supportedLeagues(): List<League> = SupportedLeagues.all

    override suspend fun getLeague(leagueId: Int): League {
        val season = SupportedLeagues.CURRENT_SEASON
        val cached = runCatching { cache.getLeague(leagueId, season) }.getOrNull()
        if (cached != null && CachePolicy.isStaticDataFresh(cached.updatedAt, clock.instant())) return cached.value

        val league = api.leagues(id = leagueId).orThrow().firstOrNull()?.toDomain(season)
            ?: return cached?.value ?: error("League $leagueId has no season $season")
        runCatching { cache.putLeagues(listOf(league), clock.instant()) }.onFailure { Log.w("LeagueRepo", "league cache write failed", it) }
        return league
    }
}
