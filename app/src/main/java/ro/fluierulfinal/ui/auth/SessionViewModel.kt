package ro.fluierulfinal.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ro.fluierulfinal.data.repository.AuthRepository
import ro.fluierulfinal.data.repository.AuthUser
import javax.inject.Inject

/**
 * App-level session state, used by the root composable to pick what to show:
 * a loading screen while the saved session is restored, Login when signed out, the app otherwise.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = auth.currentUser
    val isRestoringSession: StateFlow<Boolean> = auth.isRestoringSession

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}
