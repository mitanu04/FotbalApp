package ro.fluierulfinal.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.fluierulfinal.data.repository.FavoritesRepository
import ro.fluierulfinal.domain.model.AlertSettings
import ro.fluierulfinal.domain.model.Favorite
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Favorite> = emptyList(),
    val loading: Boolean = true,          // first load from the server
    val error: String? = null,
)

/** A one-off message for the snackbar; [undo] = the favorite that can be put back. */
data class FavoritesEvent(val message: String, val undo: Favorite? = null)

/**
 * Favorite screen: Read (list), Update (alert settings) and Delete (with Undo).
 * Create happens on the search screen (AddFavoriteViewModel); both use the same singleton
 * FavoritesRepository, so this list updates by itself when a team is added there.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: FavoritesRepository,
) : ViewModel() {

    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<FavoritesUiState> = combine(repository.favorites, loading, error) { list, isLoading, err ->
        FavoritesUiState(list, isLoading, err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    // One-off messages (snackbar). A Channel delivers each event exactly once, even if the screen
    // is rotated, unlike a StateFlow which would replay the last message.
    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events: Flow<FavoritesEvent> = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runAction("Nu am putut încărca favoritele. Verifică internetul.") { repository.refresh() }
            loading.value = false
        }
    }

    fun remove(favorite: Favorite) {
        viewModelScope.launch {
            val ok = runAction("Nu am putut șterge echipa. Încearcă din nou.") { repository.remove(favorite.teamId) }
            if (ok) _events.send(FavoritesEvent("${favorite.name} a fost scoasă din favorite", undo = favorite))
        }
    }

    fun undoRemove(favorite: Favorite) {
        viewModelScope.launch { runAction("Nu am putut readăuga echipa.") { repository.restore(favorite) } }
    }

    fun updateAlerts(teamId: Int, alerts: AlertSettings) {
        viewModelScope.launch { runAction("Nu am putut salva setările de alerte.") { repository.updateAlerts(teamId, alerts) } }
    }

    /** Runs a repository call; on failure shows [message] in the snackbar. Returns true on success. */
    private suspend fun runAction(message: String, block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        _events.send(FavoritesEvent(message))
        if (state.value.favorites.isEmpty()) error.update { message }
        false
    }
}
