package ro.fluierulfinal.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.fluierulfinal.data.remote.ApiFootballException
import ro.fluierulfinal.data.repository.FavoritesRepository
import ro.fluierulfinal.data.repository.TeamRepository
import ro.fluierulfinal.domain.model.Team
import javax.inject.Inject

data class AddFavoriteUiState(
    val query: String = "",
    val results: List<Team> = emptyList(),
    val searching: Boolean = false,
    val error: String? = null,
    val favoriteIds: Set<Int> = emptySet(),   // to show "Adăugată" instead of "Adaugă"
    val searchedOnce: Boolean = false,        // distinguishes "no results" from "nothing searched yet"
)

/**
 * Create part of the favorites CRUD: search a team and add it.
 *
 * Search rules (each one saves API requests):
 * - at least [MIN_QUERY] characters (API-Football refuses shorter queries anyway);
 * - debounce: wait until the user stops typing for [DEBOUNCE_MS] ms, so "barcelona" is
 *   one request instead of nine;
 * - distinctUntilChanged: the same text twice in a row is not searched again;
 * - TeamRepository keeps results per query in memory, so retyping an old query is free.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class AddFavoriteViewModel @Inject constructor(
    private val teams: TeamRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddFavoriteUiState())
    val state: StateFlow<AddFavoriteUiState> = _state.asStateFlow()

    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            favorites.favorites.collect { list -> _state.update { it.copy(favoriteIds = list.map { f -> f.teamId }.toSet()) } }
        }
        viewModelScope.launch {
            queries
                .map { it.trim() }
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { q -> if (q.length >= MIN_QUERY) search(q) else _state.update { it.copy(results = emptyList(), searchedOnce = false, error = null) } }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        queries.value = value
    }

    fun add(team: Team) {
        viewModelScope.launch {
            try {
                favorites.add(team, leagueId = null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Nu am putut adăuga echipa. Încearcă din nou.") }
            }
        }
    }

    private suspend fun search(q: String) {
        _state.update { it.copy(searching = true, error = null) }
        try {
            val found = teams.search(q)
            _state.update { it.copy(results = found, searchedOnce = true) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiFootballException) {
            _state.update { it.copy(error = "API-Football: ${e.message}") }   // e.g. daily quota used up
        } catch (e: Exception) {
            _state.update { it.copy(error = "Căutarea nu a mers. Verifică internetul.") }
        } finally {
            _state.update { it.copy(searching = false) }
        }
    }

    companion object {
        const val MIN_QUERY = 3
        const val DEBOUNCE_MS = 500L
    }
}
