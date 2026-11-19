package ro.fluierulfinal.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.fluierulfinal.ui.auth.LoginScreen
import ro.fluierulfinal.ui.auth.SessionViewModel

/**
 * Top of the UI: decides between the loading screen (saved session being restored),
 * Login (signed out) and the app with the bottom bar (signed in).
 *
 * Because it only reacts to `currentUser`, login and logout need no explicit navigation:
 * signing in swaps Login for the app (which opens on Favorite); signing out swaps it back,
 * and the app's back stack is thrown away, so "back" cannot return to a signed-in screen.
 */
@Composable
fun AppRoot(session: SessionViewModel = hiltViewModel()) {
    val restoring by session.isRestoringSession.collectAsStateWithLifecycle()
    val user by session.currentUser.collectAsStateWithLifecycle()

    when {
        restoring -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        user == null -> LoginScreen()
        else -> AppNavHost(userEmail = user?.email, onSignOut = session::signOut)
    }
}
