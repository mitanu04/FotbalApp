package ro.fluierulfinal.data.cache

import ro.fluierulfinal.data.remote.dto.FixtureItemDto
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.Team
import java.time.Instant
import java.time.LocalDate

/** A cached value and when it was written. */
data class Cached<T>(val value: T, val updatedAt: Instant)

/**
 * Shared cache of API-Football data (Supabase tables teams / leagues / fixtures_by_day).
 * An interface so repositories can be unit tested with an in-memory version, without network.
 *
 * Fixtures are stored as the raw API items ([FixtureItemDto]): the cache keeps exactly what the API sent,
 * and the same mapper turns it into domain models whether it came from the network or the cache.
 */
interface CacheDataSource {
    suspend fun getTeam(teamId: Int): Cached<Team>?
    suspend fun putTeams(teams: List<Team>, now: Instant)

    suspend fun getLeague(leagueId: Int, season: Int): Cached<League>?
    suspend fun putLeagues(leagues: List<League>, now: Instant)

    suspend fun getFixturesDay(leagueId: Int, day: LocalDate): Cached<List<FixtureItemDto>>?
    suspend fun putFixturesDays(days: Map<Int, List<FixtureItemDto>>, day: LocalDate, now: Instant)
}
