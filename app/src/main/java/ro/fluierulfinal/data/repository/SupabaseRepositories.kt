package ro.fluierulfinal.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ro.fluierulfinal.data.supabase.FavoriteRow
import ro.fluierulfinal.data.supabase.toDomain
import ro.fluierulfinal.data.supabase.toJson
import ro.fluierulfinal.data.supabase.toRow
import ro.fluierulfinal.di.ApplicationScope
import ro.fluierulfinal.domain.model.AlertSettings
import ro.fluierulfinal.domain.model.Favorite
import ro.fluierulfinal.domain.model.Team
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Supabase Auth (email + password; Google Sign-In is added in step 6). */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationScope scope: CoroutineScope,
) : AuthRepository {

    // Supabase restores the saved session by itself at start-up and reports it through sessionStatus.
    override val currentUser: StateFlow<AuthUser?> = supabase.auth.sessionStatus
        .map { status ->
            (status as? SessionStatus.Authenticated)?.session?.user?.let { AuthUser(it.id, it.email) }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val isRestoringSession: StateFlow<Boolean> = supabase.auth.sessionStatus
        .map { it is SessionStatus.Initializing }
        .stateIn(scope, SharingStarted.Eagerly, true)

    override suspend fun signIn(email: String, password: String) = mapAuthErrors {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String): Boolean = mapAuthErrors {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // With "Confirm email" on, Supabase creates the account but no session until the link is clicked.
        supabase.auth.currentSessionOrNull() != null
    }

    /** Translates Supabase exceptions into [AuthException], so screens never see backend types. */
    private suspend fun <T> mapAuthErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: AuthWeakPasswordException) {
        throw AuthException(AuthError.WEAK_PASSWORD, e)
    } catch (e: AuthRestException) {
        val error = when (e.errorCode) {
            AuthErrorCode.InvalidCredentials -> AuthError.INVALID_CREDENTIALS
            AuthErrorCode.EmailNotConfirmed -> AuthError.EMAIL_NOT_CONFIRMED
            AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> AuthError.USER_ALREADY_EXISTS
            AuthErrorCode.WeakPassword -> AuthError.WEAK_PASSWORD
            AuthErrorCode.EmailAddressInvalid, AuthErrorCode.ValidationFailed -> AuthError.INVALID_EMAIL
            AuthErrorCode.OverRequestRateLimit, AuthErrorCode.OverEmailSendRateLimit -> AuthError.RATE_LIMITED
            else -> AuthError.UNKNOWN
        }
        throw AuthException(error, e)
    } catch (e: HttpRequestException) {
        throw AuthException(AuthError.NETWORK, e)
    } catch (e: HttpRequestTimeoutException) {
        throw AuthException(AuthError.NETWORK, e)
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }
}

/**
 * Favorites in the Supabase `favorites` table. Row Level Security makes every query see only the
 * signed-in user's rows, so no user_id filter is needed here (and none could bypass it).
 * Changes are applied to [favorites] first (instant UI), then sent; on failure we reload and rethrow.
 */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val clock: Clock,
) : FavoritesRepository {

    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    override val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()

    private val table get() = supabase.from(TABLE)

    override suspend fun refresh() {
        _favorites.value = table.select { order("added_at", Order.DESCENDING) }
            .decodeList<FavoriteRow>()
            .map { it.toDomain() }
    }

    override suspend fun add(team: Team, leagueId: Int?) {
        if (_favorites.value.any { it.teamId == team.id }) return
        val favorite = Favorite(team.id, team.name, team.country, team.logoUrl, leagueId, addedAt = clock.instant())
        change({ listOf(favorite) + it }) { table.insert(favorite.toRow()) }
    }

    override suspend fun updateAlerts(teamId: Int, alerts: AlertSettings) {
        change({ list -> list.map { if (it.teamId == teamId) it.copy(alerts = alerts) else it } }) {
            table.update({ set("alerts", alerts.toJson()) }) { filter { eq("team_id", teamId) } }
        }
    }

    override suspend fun remove(teamId: Int) {
        change({ list -> list.filterNot { it.teamId == teamId } }) {
            table.delete { filter { eq("team_id", teamId) } }
        }
    }

    override suspend fun restore(favorite: Favorite) {
        change({ list -> (list + favorite).sortedByDescending { it.addedAt } }) { table.upsert(favorite.toRow()) }
    }

    private suspend fun change(local: (List<Favorite>) -> List<Favorite>, remote: suspend () -> Unit) {
        _favorites.update(local)
        try {
            remote()
        } catch (e: Exception) {
            runCatching { refresh() }   // put the list back in sync with the server
            throw e
        }
    }

    private companion object {
        const val TABLE = "favorites"
    }
}
