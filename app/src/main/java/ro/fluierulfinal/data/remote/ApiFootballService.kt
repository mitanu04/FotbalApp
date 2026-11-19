package ro.fluierulfinal.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import ro.fluierulfinal.data.remote.dto.ApiResponseDto
import ro.fluierulfinal.data.remote.dto.FixtureItemDto
import ro.fluierulfinal.data.remote.dto.LeagueItemDto
import ro.fluierulfinal.data.remote.dto.StandingsItemDto
import ro.fluierulfinal.data.remote.dto.StatusDto
import ro.fluierulfinal.data.remote.dto.TeamItemDto
import ro.fluierulfinal.data.remote.dto.TopScorerItemDto

/**
 * API-Football v3 endpoints, described for Retrofit: it generates the implementation.
 * Every call except /status costs 1 of the 100 daily requests on the free plan.
 * A `null` query parameter is simply left out of the URL.
 */
interface ApiFootballService {

    /** Account status and today's request count. Free: does not count against the quota. */
    @GET("status")
    suspend fun status(): ApiResponseDto<StatusDto>

    @GET("leagues")
    suspend fun leagues(
        @Query("id") id: Int? = null,
        @Query("country") country: String? = null,
        @Query("season") season: Int? = null,
    ): ApiResponseDto<List<LeagueItemDto>>

    /** Team search; the API requires at least 3 characters. */
    @GET("teams")
    suspend fun searchTeams(@Query("search") query: String): ApiResponseDto<List<TeamItemDto>>

    @GET("teams")
    suspend fun team(@Query("id") id: Int): ApiResponseDto<List<TeamItemDto>>

    /** League table. On the free plan only seasons 2022–2024 are allowed (see SupportedLeagues.STANDINGS_SEASON). */
    @GET("standings")
    suspend fun standings(
        @Query("league") league: Int,
        @Query("season") season: Int,
    ): ApiResponseDto<List<StandingsItemDto>>

    /** Top 20 scorers of a league season (free plan: seasons 2022–2024). */
    @GET("players/topscorers")
    suspend fun topScorers(
        @Query("league") league: Int,
        @Query("season") season: Int,
    ): ApiResponseDto<List<TopScorerItemDto>>

    @GET("fixtures")
    suspend fun fixtures(
        @Query("date") date: String,                 // yyyy-MM-dd
        @Query("league") league: Int? = null,
        @Query("season") season: Int? = null,
        @Query("timezone") timezone: String = "Europe/Bucharest",
    ): ApiResponseDto<List<FixtureItemDto>>
}
