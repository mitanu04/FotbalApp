package ro.fluierulfinal.data.repository

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ro.fluierulfinal.data.cache.CacheDataSource
import ro.fluierulfinal.data.cache.CachePolicy
import ro.fluierulfinal.data.cache.Cached
import ro.fluierulfinal.data.remote.ApiFootballService
import ro.fluierulfinal.data.remote.dto.FixtureItemDto
import ro.fluierulfinal.data.remote.mapper.toDomain
import ro.fluierulfinal.data.remote.orThrow
import ro.fluierulfinal.domain.model.AppTime
import ro.fluierulfinal.domain.model.SupportedLeagues
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fixtures: memory → Supabase cache → API-Football, in that order.
 *
 * One /fixtures?date= call returns every league's matches for that day, so after a call we cache
 * a row for *each* supported league. Opening another league on the same day then costs no request.
 * (The free plan refuses /fixtures with league + season for the current season; see the coverage check.)
 */
@Singleton
class FixtureRepositoryImpl @Inject constructor(
    private val api: ApiFootballService,
    private val cache: CacheDataSource,
    private val clock: Clock,
) : FixtureRepository {

    // Fastest layer: survives while the app is running, avoids even the Supabase round trip.
    private val memory = mutableMapOf<String, Cached<List<FixtureItemDto>>>()

    // Two screens asking for the same day at once must not cause two API calls.
    private val mutex = Mutex()

    override suspend fun fixtures(leagueId: Int, day: LocalDate): FixturesResult = mutex.withLock {
        val now = clock.instant()
        val key = "${leagueId}_$day"

        val cached = memory[key] ?: runCatching { cache.getFixturesDay(leagueId, day) }
            .onFailure { Log.w(TAG, "cache read failed for $key, falling back to the API", it) }
            .getOrNull()
            ?.also { memory[key] = it }

        if (cached != null) {
            val fixtures = cached.value.toSortedDomain()
            if (CachePolicy.isFixturesDayFresh(cached.updatedAt, now, fixtures)) {
                Log.i(TAG, "$key served from cache, no API request")
                return@withLock FixturesResult(fixtures, DataSource.CACHE)
            }
        }

        if (!AppTime.isInFreePlanWindow(day, now)) {
            if (cached != null) return@withLock FixturesResult(cached.value.toSortedDomain(), DataSource.STALE_CACHE)
            throw DayNotAvailableException(day)
        }

        Log.i(TAG, "$key not cached or expired, calling /fixtures?date=$day")
        val items = api.fixtures(date = day.toString()).orThrow()
        val byLeague = (SupportedLeagues.ids + leagueId).associateWith { id -> items.filter { it.league.id == id } }
        byLeague.forEach { (id, list) -> memory["${id}_$day"] = Cached(list, now) }
        runCatching { cache.putFixturesDays(byLeague, day, now) }
            .onFailure { Log.w(TAG, "cache write failed for $day", it) }

        FixturesResult(byLeague.getValue(leagueId).toSortedDomain(), DataSource.NETWORK)
    }

    private fun List<FixtureItemDto>.toSortedDomain() = map { it.toDomain() }.sortedBy { it.kickoff }

    private companion object {
        const val TAG = "FixtureRepo"
    }
}
