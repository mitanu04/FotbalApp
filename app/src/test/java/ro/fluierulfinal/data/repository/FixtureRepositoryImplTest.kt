package ro.fluierulfinal.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.fluierulfinal.data.cache.CacheDataSource
import ro.fluierulfinal.data.cache.Cached
import ro.fluierulfinal.data.remote.ApiFootballService
import ro.fluierulfinal.data.remote.ApiJson
import ro.fluierulfinal.data.remote.dto.ApiResponseDto
import ro.fluierulfinal.data.remote.dto.FixtureItemDto
import ro.fluierulfinal.data.remote.dto.LeagueItemDto
import ro.fluierulfinal.data.remote.dto.StandingsItemDto
import ro.fluierulfinal.data.remote.dto.StatusDto
import ro.fluierulfinal.data.remote.dto.TeamItemDto
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.Team
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * FixtureRepositoryImpl with a fake API (counts calls, serves the real sample response)
 * and an in-memory cache. No network: the acceptance rule "opening the same day again
 * does not call the API" is checked here.
 */
class FixtureRepositoryImplTest {

    private val now = Instant.parse("2026-11-19T18:00:00Z")          // Thursday 19 Nov, 20:00 in Bucharest
    private val tomorrow = LocalDate.of(2026, 11, 20)

    private val sample: List<FixtureItemDto> = ApiJson.decodeFromString<ApiResponseDto<List<FixtureItemDto>>>(
        requireNotNull(javaClass.classLoader?.getResource("api/fixtures_sample.json")).readText()
    ).response

    private class CountingApi(private val items: List<FixtureItemDto>) : ApiFootballService {
        var fixtureCalls = 0
        override suspend fun fixtures(date: String, league: Int?, season: Int?, timezone: String): ApiResponseDto<List<FixtureItemDto>> {
            fixtureCalls++
            return ApiResponseDto(results = items.size, response = items)
        }
        override suspend fun status(): ApiResponseDto<StatusDto> = error("not used")
        override suspend fun leagues(id: Int?, country: String?, season: Int?): ApiResponseDto<List<LeagueItemDto>> = error("not used")
        override suspend fun searchTeams(query: String): ApiResponseDto<List<TeamItemDto>> = error("not used")
        override suspend fun team(id: Int): ApiResponseDto<List<TeamItemDto>> = error("not used")
        override suspend fun standings(league: Int, season: Int): ApiResponseDto<List<StandingsItemDto>> = error("not used")
    }

    private class InMemoryCache : CacheDataSource {
        val days = mutableMapOf<String, Cached<List<FixtureItemDto>>>()
        override suspend fun getFixturesDay(leagueId: Int, day: LocalDate) = days["${leagueId}_$day"]
        override suspend fun putFixturesDays(days: Map<Int, List<FixtureItemDto>>, day: LocalDate, now: Instant) {
            days.forEach { (id, list) -> this.days["${id}_$day"] = Cached(list, now) }
        }
        override suspend fun getTeam(teamId: Int): Cached<Team>? = null
        override suspend fun putTeams(teams: List<Team>, now: Instant) = Unit
        override suspend fun getLeague(leagueId: Int, season: Int): Cached<League>? = null
        override suspend fun putLeagues(leagues: List<League>, now: Instant) = Unit
        override suspend fun getStandings(leagueId: Int, season: Int): Cached<List<StandingsItemDto>>? = null
        override suspend fun putStandings(leagueId: Int, season: Int, items: List<StandingsItemDto>, now: Instant) = Unit
    }

    private fun repo(api: ApiFootballService, cache: CacheDataSource, at: Instant = now) =
        FixtureRepositoryImpl(api, cache, Clock.fixed(at, ZoneOffset.UTC))

    @Test
    fun secondOpenOfSameDay_comesFromCache_withoutApiCall() = runTest {
        val api = CountingApi(sample)
        val repo = repo(api, InMemoryCache())

        val first = repo.fixtures(78, tomorrow)
        assertEquals(DataSource.NETWORK, first.source)
        assertEquals(listOf("Bayern München"), first.fixtures.map { it.homeTeam.name })

        val second = repo.fixtures(78, tomorrow)
        assertEquals(DataSource.CACHE, second.source)
        assertEquals(1, api.fixtureCalls)
    }

    @Test
    fun otherLeagueSameDay_isAlsoCached_byTheFirstCall() = runTest {
        val api = CountingApi(sample)
        val cache = InMemoryCache()
        repo(api, cache).fixtures(78, tomorrow)

        val laLiga = repo(api, cache).fixtures(140, tomorrow) // new repository = empty memory, Supabase cache only
        assertEquals(DataSource.CACHE, laLiga.source)
        assertTrue(laLiga.fixtures.isEmpty())                  // no La Liga game in the sample
        assertEquals(1, api.fixtureCalls)
    }

    @Test
    fun expiredCache_callsTheApiAgain() = runTest {
        val api = CountingApi(sample)
        val cache = InMemoryCache()
        repo(api, cache).fixtures(78, tomorrow)

        val sevenHoursLater = now.plus(Duration.ofHours(7))
        val result = repo(api, cache, sevenHoursLater).fixtures(78, tomorrow)
        assertEquals(DataSource.NETWORK, result.source)
        assertEquals(2, api.fixtureCalls)
    }

    @Test(expected = DayNotAvailableException::class)
    fun dayOutsideFreePlan_withoutCache_throws() = runTest {
        repo(CountingApi(sample), InMemoryCache()).fixtures(78, LocalDate.of(2026, 11, 25))
    }

    @Test
    fun dayOutsideFreePlan_withOldCache_returnsStaleData_withoutApiCall() = runTest {
        val api = CountingApi(sample)
        val cache = InMemoryCache()
        val longAgo = now.minus(Duration.ofDays(3))
        val day = LocalDate.of(2026, 11, 17)  // Tuesday, outside the window today
        cache.days["78_$day"] = Cached(sample.filter { it.league.id == 78 }, longAgo)

        val result = repo(api, cache).fixtures(78, day)
        assertEquals(DataSource.STALE_CACHE, result.source)
        assertEquals(0, api.fixtureCalls)
    }
}
