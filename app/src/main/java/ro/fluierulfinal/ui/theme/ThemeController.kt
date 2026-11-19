package ro.fluierulfinal.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Which theme the app should use. */
enum class ThemeMode {
    /** Follow the phone's dark mode setting (default in S8). */
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Single source of truth for the theme choice, shared by the whole app (@Singleton).
 *
 * In S8 it simply stays on [ThemeMode.SYSTEM]. In S10 the light sensor will call [setMode]
 * (e.g. DARK in a dark room) and every screen will switch automatically, because the UI
 * observes [mode] as a StateFlow.
 */
@Singleton
class ThemeController @Inject constructor() {

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
    }
}
