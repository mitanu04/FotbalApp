package ro.fluierulfinal.ui.explore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.fluierulfinal.data.remote.ApiFootballException
import ro.fluierulfinal.data.repository.DataSource
import ro.fluierulfinal.data.repository.DayNotAvailableException
import ro.fluierulfinal.data.repository.FixtureRepository
import ro.fluierulfinal.data.repository.LeagueRepository
import ro.fluierulfinal.data.repository.StandingsRepository
import ro.fluierulfinal.domain.model.AppTime
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.Standings
import ro.fluierulfinal.domain.model.SupportedLeagues
import ro.fluierulfinal.navigation.ExploreLeagueRoute
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Step 1 of Explorează: the list of supported leagues (fixed, costs no request). */
@HiltViewModel
class ExploreViewModel @Inject constructor(leagues: LeagueRepository) : ViewModel() {
    val leagues: List<League> = leagues.supportedLeagues()
}

/** The three groups shown on the fixtures list, in this order. */
data class FixtureGroups(
    val live: List<Fixture>,
    val scheduled: List<Fixture>,    // includes postponed
    val finished: List<Fixture>,     // includes cancelled
) {
    val isEmpty get() = live.isEmpty() && scheduled.isEmpty() && finished.isEmpty()

    companion object {
        fun of(fixtures: List<Fixture>) = FixtureGroups(
            live = fixtures.filter { it.status == FixtureStatus.LIVE },
            scheduled = fixtures.filter { it.status == FixtureStatus.SCHEDULED || it.status == FixtureStatus.POSTPONED },
            finished = fixtures.filter { it.status == FixtureStatus.FINISHED || it.status == FixtureStatus.CANCELLED },
        )
    }
}

/** The league table tab: loaded only when the user opens it (no prefetch). */
sealed interface StandingsState {
    data object NotLoaded : StandingsState
    data object Loading : StandingsState
    data class Loaded(val standings: Standings) : StandingsState
    data class Error(val message: String) : StandingsState
}

sealed interface FixturesState {
    data object Loading : FixturesState
    data class Loaded(val groups: FixtureGroups, val source: DataSource) : FixturesState
    data class NotAvailable(val message: String) : FixturesState   // outside the free plan, nothing cached
    data class Error(val message: String) : FixturesState
}

data class ExploreLeagueUiState(
    val league: League,
    val today: LocalDate,
    val days: List<LocalDate>,
    val selectedDay: LocalDate,
    val fixtures: FixturesState = FixturesState.Loading,
    val standings: StandingsState = StandingsState.NotLoaded,
)

/**
 * Steps 2 and 3 of Explorează: pick a day (today ± 7), see that day's fixtures grouped by status.
 * The league id comes from the navigation route (ExploreLeagueRoute), read through SavedStateHandle.
 */
@HiltViewModel
class ExploreLeagueViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    leagues: LeagueRepository,
    private val fixtures: FixtureRepository,
    private val standingsRepository: StandingsRepository,
    clock: Clock,
) : ViewModel() {

    private val leagueId = savedStateHandle.toRoute<ExploreLeagueRoute>().leagueId

    private val _state: MutableStateFlow<ExploreLeagueUiState>
    val state: StateFlow<ExploreLeagueUiState>

    private var loadJob: Job? = null

    init {
        val today = AppTime.today(clock.instant())
        val league = leagues.supportedLeagues().first { it.id == leagueId }
        _state = MutableStateFlow(
            ExploreLeagueUiState(
                league = league,
                today = today,
                days = (-DAYS_AROUND..DAYS_AROUND).map { today.plusDays(it.toLong()) },
                selectedDay = today,
            )
        )
        state = _state.asStateFlow()
        load()
    }

    /** False for days outside FREE_PLAN_DATE_WINDOW: their chips are disabled and never trigger a request. */
    fun isDayAvailable(day: LocalDate): Boolean = AppTime.isInFreePlanWindow(day, _state.value.today)

    fun selectDay(day: LocalDate) {
        if (day == _state.value.selectedDay || !isDayAvailable(day)) return
        _state.update { it.copy(selectedDay = day) }
        load()
    }

    fun retry() = load()

    /** Called when the "Clasament" tab is opened; does nothing if the table is already there or loading. */
    fun loadStandings(force: Boolean = false) {
        val current = _state.value.standings
        if (!force && (current is StandingsState.Loaded || current is StandingsState.Loading)) return
        _state.update { it.copy(standings = StandingsState.Loading) }
        viewModelScope.launch {
            val result = try {
                StandingsState.Loaded(standingsRepository.standings(leagueId, SupportedLeagues.STANDINGS_SEASON))
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiFootballException) {
                StandingsState.Error("API-Football: ${e.message}")
            } catch (e: Exception) {
                StandingsState.Error("Nu am putut încărca clasamentul. Verifică internetul.")
            }
            _state.update { it.copy(standings = result) }
        }
    }

    private fun load() {
        loadJob?.cancel()   // switching days quickly: only the last chosen day matters
        val day = _state.value.selectedDay
        _state.update { it.copy(fixtures = FixturesState.Loading) }
        loadJob = viewModelScope.launch {
            val result = try {
                val r = fixtures.fixtures(leagueId, day)
                FixturesState.Loaded(FixtureGroups.of(r.fixtures), r.source)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DayNotAvailableException) {
                FixturesState.NotAvailable(e.message ?: "")
            } catch (e: ApiFootballException) {
                FixturesState.Error("API-Football: ${e.message}")
            } catch (e: Exception) {
                FixturesState.Error("Nu am putut încărca meciurile. Verifică internetul.")
            }
            _state.update { if (it.selectedDay == day) it.copy(fixtures = result) else it }
        }
    }

    companion object {
        const val DAYS_AROUND = 7
    }
}
