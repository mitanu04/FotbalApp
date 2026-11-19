package ro.fluierulfinal.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import ro.fluierulfinal.data.cache.CacheDataSource
import ro.fluierulfinal.data.cache.Cached
import ro.fluierulfinal.data.remote.dto.FixtureItemDto
import ro.fluierulfinal.data.remote.dto.StandingsItemDto
import ro.fluierulfinal.data.remote.dto.TopScorerItemDto
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.Team
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CacheDataSource] backed by the Supabase tables teams / leagues / fixtures_by_day.
 * `upsert` = insert, or update if a row with the same primary key already exists.
 */
@Singleton
class SupabaseCacheDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) : CacheDataSource {

    override suspend fun getTeam(teamId: Int): Cached<Team>? =
        supabase.from("teams").select { filter { eq("team_id", teamId) } }
            .decodeSingleOrNull<TeamRow>()
            ?.let { Cached(it.toDomain(), parseTimestamp(it.updatedAt)) }

    override suspend fun putTeams(teams: List<Team>, now: Instant) {
        if (teams.isEmpty()) return
        supabase.from("teams").upsert(teams.map { it.toRow(now) })
    }

    override suspend fun getLeague(leagueId: Int, season: Int): Cached<League>? =
        supabase.from("leagues").select { filter { eq("id", leagueRowId(leagueId, season)) } }
            .decodeSingleOrNull<LeagueRow>()
            ?.let { Cached(it.toDomain(), parseTimestamp(it.updatedAt)) }

    override suspend fun putLeagues(leagues: List<League>, now: Instant) {
        if (leagues.isEmpty()) return
        supabase.from("leagues").upsert(leagues.map { it.toRow(now) })
    }

    override suspend fun getStandings(leagueId: Int, season: Int): Cached<List<StandingsItemDto>>? =
        supabase.from("standings").select { filter { eq("id", leagueRowId(leagueId, season)) } }
            .decodeSingleOrNull<StandingsRow>()
            ?.let { Cached(it.standings, parseTimestamp(it.updatedAt)) }

    override suspend fun putStandings(leagueId: Int, season: Int, items: List<StandingsItemDto>, now: Instant) {
        supabase.from("standings").upsert(StandingsRow(leagueRowId(leagueId, season), leagueId, season, items, now.toString()))
    }

    override suspend fun getTopScorers(leagueId: Int, season: Int): Cached<List<TopScorerItemDto>>? =
        supabase.from("top_scorers").select { filter { eq("id", leagueRowId(leagueId, season)) } }
            .decodeSingleOrNull<TopScorersRow>()
            ?.let { Cached(it.scorers, parseTimestamp(it.updatedAt)) }

    override suspend fun putTopScorers(leagueId: Int, season: Int, items: List<TopScorerItemDto>, now: Instant) {
        supabase.from("top_scorers").upsert(TopScorersRow(leagueRowId(leagueId, season), leagueId, season, items, now.toString()))
    }

    override suspend fun getFixturesDay(leagueId: Int, day: LocalDate): Cached<List<FixtureItemDto>>? =
        supabase.from("fixtures_by_day").select { filter { eq("id", fixturesDayRowId(leagueId, day.toString())) } }
            .decodeSingleOrNull<FixturesDayRow>()
            ?.let { Cached(it.fixtures, parseTimestamp(it.updatedAt)) }

    override suspend fun putFixturesDays(days: Map<Int, List<FixtureItemDto>>, day: LocalDate, now: Instant) {
        if (days.isEmpty()) return
        val rows = days.map { (leagueId, fixtures) ->
            FixturesDayRow(fixturesDayRowId(leagueId, day.toString()), leagueId, day.toString(), fixtures, now.toString())
        }
        supabase.from("fixtures_by_day").upsert(rows)
    }
}
