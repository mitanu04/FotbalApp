package ro.fluierulfinal.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ro.fluierulfinal.data.Match
import ro.fluierulfinal.data.MatchRepository
import ro.fluierulfinal.data.MatchStatus
import ro.fluierulfinal.data.PlanSlot
import ro.fluierulfinal.data.WeekendPlanner
import java.time.LocalDate

enum class StatusFilter { ALL, LIVE, FINISHED, UPCOMING }

/** Tot ce trebuie să știe ecranul ca să se deseneze. */
data class HomeUiState(
    val favoriteTeamIds: Set<String>,
    val myLeagueIds: List<String>,      // ligile alese de utilizator, în bara de ligi
    val selectedLeagueId: String?,      // null = toate ligile alese
    val day: LocalDate,
    val filter: StatusFilter,
    val openMatchId: Int?,              // != null => e deschis ecranul de detalii
    val followedMatchIds: Set<Int> = emptySet(),     // meciuri urmărite cu steaua din rând
    val collapsedLeagueIds: Set<String> = emptySet(), // ligi strânse în listă (săgeata din antet)
)

/**
 * ViewModel-ul (MVVM): ține starea ecranului și o schimbă când utilizatorul apasă ceva.
 * Ecranul doar citește `state` și cheamă funcțiile de mai jos; nu face niciodată apeluri de date.
 */
class HomeViewModel : ViewModel() {

    val repository = MatchRepository()

    private val _state = MutableStateFlow(
        HomeUiState(
            favoriteTeamIds = setOf("rap", "poli", "bar", "mil", "liv", "ucv"),
            myLeagueIds = listOf("sl", "l2", "ll", "pl", "sa", "ucl"),
            selectedLeagueId = null,
            day = repository.today,
            filter = StatusFilter.ALL,
            openMatchId = null,
        )
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun toggleFavorite(teamId: String) = _state.update {
        val favs = if (teamId in it.favoriteTeamIds) it.favoriteTeamIds - teamId else it.favoriteTeamIds + teamId
        it.copy(favoriteTeamIds = favs)
    }

    fun selectLeague(leagueId: String?) = _state.update { it.copy(selectedLeagueId = leagueId) }

    fun setMyLeagues(ids: List<String>) = _state.update {
        // dacă liga selectată a fost scoasă, revenim la „Toate ligile”
        val selected = it.selectedLeagueId?.takeIf { id -> id in ids }
        it.copy(myLeagueIds = ids, selectedLeagueId = selected)
    }

    fun setFilter(filter: StatusFilter) = _state.update { it.copy(filter = filter) }
    fun previousDay() = _state.update { it.copy(day = it.day.minusDays(1)) }
    fun nextDay() = _state.update { it.copy(day = it.day.plusDays(1)) }

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
            .filter { if (s.selectedLeagueId != null) it.leagueId == s.selectedLeagueId else it.leagueId in s.myLeagueIds }
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

    fun liveMatches(): List<Match> = repository.matches.filter { it.status == MatchStatus.LIVE }

    /** Pentru „Program TV”: meciurile favoritelor de azi încolo care nu s-au terminat. */
    fun upcomingFavoriteMatches(s: HomeUiState): List<Match> = repository.matches
        .filter { it.date >= repository.today && it.status != MatchStatus.FINISHED && isFavoriteMatch(it, s) }
        .sortedWith(compareBy({ it.date }, { it.kickoff }))
}
