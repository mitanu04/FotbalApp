package ro.fluierulfinal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.fluierulfinal.data.Match
import ro.fluierulfinal.data.MatchRepository
import ro.fluierulfinal.data.MatchStatus
import ro.fluierulfinal.data.PlanSlot
import ro.fluierulfinal.data.WeekendPlanner
import ro.fluierulfinal.data.api.ApiException
import java.time.LocalDate

enum class StatusFilter { ALL, LIVE, FINISHED, UPCOMING }

/** Tot ce trebuie să știe ecranul ca să se deseneze. */
data class HomeUiState(
    val favoriteTeamIds: Set<String>,
    val myLeagueIds: List<String>,      // ligile alese de utilizator, în bara de ligi
    val day: LocalDate,
    val filter: StatusFilter,
    val openMatchId: Int?,              // != null => e deschis ecranul de detalii
    val openLeagueId: String? = null,   // != null => e deschisă pagina ligii
    val followedMatchIds: Set<Int> = emptySet(),     // meciuri urmărite cu steaua din rând
    val collapsedLeagueIds: Set<String> = emptySet(), // ligi strânse în listă (săgeata din antet)
    val loading: Boolean = false,       // se aduc acum meciuri din API
    val error: String? = null,          // ultima eroare de la API, de arătat utilizatorului
)

/**
 * ViewModel-ul (MVVM): ține starea ecranului și o schimbă când utilizatorul apasă ceva.
 * Ecranul doar citește `state` și cheamă funcțiile de mai jos; nu face niciodată apeluri de date.
 */
class HomeViewModel : ViewModel() {

    val repository = MatchRepository()

    private val _state = MutableStateFlow(
        HomeUiState(
            favoriteTeamIds = setOf("api6231", "api2606", "api529", "api489", "api40", "api632"),
            myLeagueIds = listOf("sl", "l2", "ll", "pl", "sa", "ucl"),
            day = repository.today,
            filter = StatusFilter.ALL,
            openMatchId = null,
        )
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // La pornire aducem toate cele 3 zile cu acces (3 cereri), ca „Live”, planul de weekend
        // și programul favoritelor să aibă date reale; ziua afișată (azi) e prima.
        viewModelScope.launch {
            load(repository.today)
            repository.apiDays.filter { it != repository.today }.forEach { load(it) }
        }
    }

    /**
     * Aduce o zi din API în fundal. `viewModelScope` oprește automat cererea dacă ecranul
     * e închis definitiv, ca să nu lucrăm degeaba.
     */
    private suspend fun load(day: LocalDate, force: Boolean = false) {
        if (!repository.isApiDay(day)) return
        _state.update { it.copy(loading = true, error = null) }
        try {
            repository.loadDay(day, force)
        } catch (e: ApiException) {
            _state.update { it.copy(error = e.message) }
        } finally {
            _state.update { it.copy(loading = false) }
        }
    }

    /** Butonul „Reîncearcă” / reîmprospătare: cere din nou ziua afișată, chiar dacă o avem. */
    fun refresh() {
        viewModelScope.launch { load(_state.value.day, force = true) }
    }

    /** True dacă ziua afișată nu e acoperită de planul gratuit și arătăm date de exemplu. */
    fun isSampleDay(s: HomeUiState) = !repository.isApiDay(s.day)

    fun toggleFavorite(teamId: String) = _state.update {
        val favs = if (teamId in it.favoriteTeamIds) it.favoriteTeamIds - teamId else it.favoriteTeamIds + teamId
        it.copy(favoriteTeamIds = favs)
    }

    fun setMyLeagues(ids: List<String>) = _state.update { it.copy(myLeagueIds = ids) }

    fun openLeague(id: String) = _state.update { it.copy(openLeagueId = id) }
    fun closeLeague() = _state.update { it.copy(openLeagueId = null) }

    fun setFilter(filter: StatusFilter) = _state.update { it.copy(filter = filter) }
    fun previousDay() = changeDay(-1)
    fun nextDay() = changeDay(+1)

    private fun changeDay(delta: Long) {
        _state.update { it.copy(day = it.day.plusDays(delta), error = null) }
        viewModelScope.launch { load(_state.value.day) }
    }

    fun openMatch(id: Int) = _state.update { it.copy(openMatchId = id) }
    fun closeMatch() = _state.update { it.copy(openMatchId = null) }

    fun toggleFollow(matchId: Int) = _state.update {
        val ids = if (matchId in it.followedMatchIds) it.followedMatchIds - matchId else it.followedMatchIds + matchId
        it.copy(followedMatchIds = ids)
    }

    fun toggleCollapsed(leagueId: String) = _state.update {
        val ids = if (leagueId in it.collapsedLeagueIds) it.collapsedLeagueIds - leagueId else it.collapsedLeagueIds + leagueId
        it.copy(collapsedLeagueIds = ids)
    }

    /** Un meci apare la „Favorite” dacă joacă o echipă favorită sau dacă l-ai urmărit cu steaua. */
    fun isFavoriteMatch(match: Match, s: HomeUiState = state.value) =
        match.homeId in s.favoriteTeamIds || match.awayId in s.favoriteTeamIds || match.id in s.followedMatchIds

    /** Meciurile din listă, după toate filtrele, grupate pe ligă. Live primele, apoi viitoare, apoi terminate. */
    fun visibleMatches(s: HomeUiState): Map<String, List<Match>> {
        val order = mapOf(MatchStatus.LIVE to 0, MatchStatus.UPCOMING to 1, MatchStatus.FINISHED to 2)
        return repository.matches
            .filter { it.date == s.day }
            .filter { it.leagueId in s.myLeagueIds }
            .filter {
                when (s.filter) {
                    StatusFilter.ALL -> true
                    StatusFilter.LIVE -> it.status == MatchStatus.LIVE
                    StatusFilter.FINISHED -> it.status == MatchStatus.FINISHED
                    StatusFilter.UPCOMING -> it.status == MatchStatus.UPCOMING
                }
            }
            .sortedWith(compareBy({ order.getValue(it.status) }, { it.kickoff }))
            .groupBy { it.leagueId }
    }

    /** Pentru pagina ligii: meciurile ligii pe zile, zilele în ordine; în fiecare zi, după oră. */
    fun leagueMatchesByDay(leagueId: String): List<Pair<LocalDate, List<Match>>> = repository.matches
        .filter { it.leagueId == leagueId }
        .sortedWith(compareBy({ it.date }, { it.kickoff }))
        .groupBy { it.date }
        .toList()

    /** Planul de weekend calculat acum, plus textul cu intervalul („30 oct – 1 nov”). */
    fun weekendPlan(s: HomeUiState): Pair<String, List<PlanSlot>> {
        val friday = WeekendPlanner.weekendStart(repository.today)
        val sunday = friday.plusDays(2)
        val months = listOf("ian", "feb", "mar", "apr", "mai", "iun", "iul", "aug", "sep", "oct", "nov", "dec")
        val range = "${friday.dayOfMonth} ${months[friday.monthValue - 1]} – ${sunday.dayOfMonth} ${months[sunday.monthValue - 1]}"
        val slots = WeekendPlanner.plan(
            today = repository.today,
            matches = repository.matches,
            favoriteTeamIds = s.favoriteTeamIds,
            teamById = repository::team,
            userLat = repository.userLat,
            userLng = repository.userLng,
        )
        return range to slots
    }

    /** Meciurile live de azi (nu și cele „live” din datele de exemplu, care sunt pe altă zi). */
    fun liveMatches(): List<Match> = repository.matches.filter { it.status == MatchStatus.LIVE && it.date == repository.today }

    /** Pentru „Program TV”: meciurile favoritelor de azi încolo care nu s-au terminat. */
    fun upcomingFavoriteMatches(s: HomeUiState): List<Match> = repository.matches
        .filter { it.date >= repository.today && it.status != MatchStatus.FINISHED && isFavoriteMatch(it, s) }
        .sortedWith(compareBy({ it.date }, { it.kickoff }))
}
