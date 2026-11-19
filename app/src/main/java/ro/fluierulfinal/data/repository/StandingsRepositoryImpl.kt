package ro.fluierulfinal.data.repository

import android.util.Log
import ro.fluierulfinal.data.cache.CacheDataSource
import ro.fluierulfinal.data.cache.CachePolicy
import ro.fluierulfinal.data.remote.ApiFootballService
import ro.fluierulfinal.data.remote.mapper.toDomain
import ro.fluierulfinal.data.remote.mapper.toTopScorers
import ro.fluierulfinal.data.remote.orThrow
import ro.fluierulfinal.domain.model.Standings
import ro.fluierulfinal.domain.model.SupportedLeagues
import ro.fluierulfinal.domain.model.TopScorer
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * League tables: memory → Supabase cache → one /standings call (only when a league's table is opened).
 * A finished season never expires in the cache, so each league costs one request in total.
 */
@Singleton
class StandingsRepositoryImpl @Inject constructor(
    private val api: ApiFootballService,
    private val cache: CacheDataSource,
    private val clock: Clock,
) : StandingsRepository {

    private val memory = mutableMapOf<String, Standings>()

    override suspend fun standings(leagueId: Int, season: Int): Standings {
        val key = "${leagueId}_$season"
        memory[key]?.let { return it }

        val now = clock.instant()
        val cached = runCatching { cache.getStandings(leagueId, season) }
            .onFailure { Log.w(TAG, "cache read failed for $key: ${it.safeDescription()}") }.getOrNull()
        if (cached != null && CachePolicy.isStandingsFresh(cached.updatedAt, now, season, SupportedLeagues.CURRENT_SEASON)) {
            Log.i(TAG, "$key served from cache, no API request")
            return cached.value.first().toDomain().also { memory[key] = it }
        }

        Log.i(TAG, "$key not cached, calling /standings")
        val items = api.standings(leagueId, season).orThrow()
        val standings = items.firstOrNull()?.toDomain() ?: error("No standings for league $leagueId, season $season")
        runCatching { cache.putStandings(leagueId, season, items, now) }.onFailure { Log.w(TAG, "cache write failed for $key: ${it.safeDescription()}") }
        memory[key] = standings
        return standings
    }

    private val scorersMemory = mutableMapOf<String, List<TopScorer>>()

    override suspend fun topScorers(leagueId: Int, season: Int): List<TopScorer> {
        val key = "${leagueId}_$season"
        scorersMemory[key]?.let { return it }

        val now = clock.instant()
        val cached = runCatching { cache.getTopScorers(leagueId, season) }
            .onFailure { Log.w(TAG, "scorers cache read failed for $key: ${it.safeDescription()}") }.getOrNull()
        if (cached != null && CachePolicy.isStandingsFresh(cached.updatedAt, now, season, SupportedLeagues.CURRENT_SEASON)) {
            Log.i(TAG, "scorers $key served from cache, no API request")
            return cached.value.toTopScorers().also { scorersMemory[key] = it }
        }

        Log.i(TAG, "scorers $key not cached, calling /players/topscorers")
        val items = api.topScorers(leagueId, season).orThrow()
        runCatching { cache.putTopScorers(leagueId, season, items, now) }
            .onFailure { Log.w(TAG, "scorers cache write failed for $key: ${it.safeDescription()}") }
        return items.toTopScorers().also { scorersMemory[key] = it }
    }

    private companion object {
        const val TAG = "StandingsRepo"
    }
}
