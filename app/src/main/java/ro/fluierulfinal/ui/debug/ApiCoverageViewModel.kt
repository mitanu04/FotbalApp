package ro.fluierulfinal.ui.debug

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.fluierulfinal.data.cache.CacheDataSource
import ro.fluierulfinal.data.remote.ApiFootballService
import ro.fluierulfinal.data.remote.mapper.toDomain
import ro.fluierulfinal.data.remote.orThrow
import ro.fluierulfinal.domain.model.League
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ApiCoverageUiState(
    val running: Boolean = false,
    val report: String? = null,     // last result, kept between app starts
    val error: String? = null,
)

/**
 * One-off API coverage check (S8, before building the UI). It answers:
 * which plan we are on, the real league ids, the current season, the `coverage` flags,
 * and whether the free plan accepts /fixtures with league + season + date.
 *
 * Cost: 3 requests (/status is free). The leagues with their coverage are saved in the Supabase
 * `leagues` table; the text report is also kept on the phone, so the check is not repeated by accident.
 */
@HiltViewModel
class ApiCoverageViewModel @Inject constructor(
    private val api: ApiFootballService,
    private val cache: CacheDataSource,
    private val clock: Clock,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val prefs = context.getSharedPreferences("api_coverage", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(ApiCoverageUiState(report = prefs.getString(KEY_REPORT, null)))
    val state: StateFlow<ApiCoverageUiState> = _state.asStateFlow()

    fun run() {
        if (_state.value.running) return
        _state.update { it.copy(running = true, error = null) }
        viewModelScope.launch {
            try {
                val report = buildReport()
                prefs.edit().putString(KEY_REPORT, report).apply()
                Log.i(TAG, report)
                _state.update { it.copy(report = report) }
            } catch (e: Exception) {
                Log.e(TAG, "coverage check failed", e)
                _state.update { it.copy(error = e.message ?: e.toString()) }
            } finally {
                _state.update { it.copy(running = false) }
            }
        }
    }

    private suspend fun buildReport(): String = buildString {
        val now = ZonedDateTime.now(ZONE)
        appendLine("Rulat la ${now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
        appendLine()

        // 1. /status (free)
        val status = api.status().orThrow()
        appendLine("Plan: ${status.subscription.plan} (activ: ${status.subscription.active})")
        appendLine("Cereri azi înainte de test: ${status.requests.current}/${status.requests.limitDay}")
        appendLine()

        // 2. /leagues?country=Romania  (1 request)
        val romania = api.leagues(country = "Romania").orThrow()
        appendLine("Ligi în România (${romania.size}):")
        romania.sortedBy { it.league.id }.forEach { item ->
            val current = item.seasons.firstOrNull { it.current }?.year
            appendLine("  ${item.league.id} = ${item.league.name} (${item.league.type}), sezon curent: $current")
        }
        appendLine()
        // coverage only for the leagues the app supports: Liga I (283) and Liga II (284)
        val romanianLeagues = romania.filter { it.league.id in setOf(283, 284) }.mapNotNull { it.toDomain() }
        romanianLeagues.forEach { appendCoverage(it) }

        // 3. /leagues?id=140  (1 request)
        val laLiga = api.leagues(id = 140).orThrow().single().toDomain()
            ?: error("La Liga has no season marked as current")
        appendCoverage(laLiga)

        // Save the leagues (with coverage) in the Supabase `leagues` table, so the app reads them
        // from there (LeagueRepository, 7-day cache) instead of asking the API again.
        val saved = runCatching { cache.putLeagues(romanianLeagues + laLiga, clock.instant()) }
        appendLine(
            saved.fold(
                onSuccess = { "Salvat în Supabase (tabelul leagues): ${(romanianLeagues + laLiga).joinToString { "${it.id}_${it.season}" }}" },
                onFailure = { "NU s-a salvat în Supabase: ${it.message}" },
            )
        )
        appendLine()

        // 4. Does the free plan accept league + season + date for the current season?  (1 request)
        val today = LocalDate.now(ZONE)
        appendLine("Test /fixtures?league=140&season=${laLiga.season}&date=$today:")
        val probe = api.fixtures(date = today.toString(), league = 140, season = laLiga.season)
        val result = runCatching { probe.orThrow() }
        appendLine(
            result.fold(
                onSuccess = { "  OK, ${it.size} meciuri" },
                onFailure = { "  REFUZAT: ${it.message}" },
            )
        )
    }

    private fun StringBuilder.appendCoverage(league: League) {
        val c = league.coverage
        appendLine("${league.name} (id ${league.id}, ${league.country}), sezon ${league.season}:")
        appendLine("  events=${c.events}, lineups=${c.lineups}, statistics_fixtures=${c.statisticsFixtures}, " +
            "statistics_players=${c.statisticsPlayers}, odds=${c.odds}")
        appendLine()
    }

    companion object {
        private const val TAG = "ApiCoverage"
        private const val KEY_REPORT = "report"
        private val ZONE: ZoneId = ZoneId.of("Europe/Bucharest")
    }
}
