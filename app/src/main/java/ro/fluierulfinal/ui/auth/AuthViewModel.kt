package ro.fluierulfinal.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.fluierulfinal.data.repository.AuthError
import ro.fluierulfinal.data.repository.AuthException
import ro.fluierulfinal.data.repository.AuthRepository
import javax.inject.Inject

data class AuthUiState(
    val isRegister: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fieldErrors: FieldErrors = FieldErrors(),
    val loading: Boolean = false,
    val error: String? = null,       // server / network error, shown above the button
    val info: String? = null,        // e.g. "check your email" after registering
)

/**
 * Login / Register screen state. On success there is nothing to navigate here:
 * the root observes AuthRepository.currentUser and swaps Login for the app (Favorite tab).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    // Typing in a field clears that field's error and any server message.
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, fieldErrors = it.fieldErrors.copy(email = null), error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, fieldErrors = it.fieldErrors.copy(password = null), error = null) }
    fun onConfirmPasswordChange(value: String) =
        _state.update { it.copy(confirmPassword = value, fieldErrors = it.fieldErrors.copy(confirmPassword = null), error = null) }

    fun toggleMode() = _state.update {
        AuthUiState(isRegister = !it.isRegister, email = it.email)   // keep the email, reset the rest
    }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val errors = AuthValidation.validate(s.email, s.password, s.confirmPassword, s.isRegister)
        if (!errors.isValid) {
            _state.update { it.copy(fieldErrors = errors) }
            return
        }
        _state.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            try {
                val email = s.email.trim()
                if (s.isRegister) {
                    val ready = auth.signUp(email, s.password)
                    if (!ready) {
                        _state.update {
                            it.copy(isRegister = false, password = "", confirmPassword = "",
                                info = "Cont creat. Deschide linkul primit pe $email, apoi autentifică-te.")
                        }
                    }
                } else {
                    auth.signIn(email, s.password)
                }
            } catch (e: AuthException) {
                _state.update { it.copy(error = AuthValidation.messageFor(e.error)) }
            } catch (e: CancellationException) {
                throw e   // the screen was closed: let the coroutine stop normally
            } catch (e: Exception) {
                _state.update { it.copy(error = AuthValidation.messageFor(AuthError.UNKNOWN)) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}
