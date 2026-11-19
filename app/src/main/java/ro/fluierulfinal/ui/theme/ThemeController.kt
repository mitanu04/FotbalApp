package ro.fluierulfinal.ui.theme

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Light or dark. */
enum class ThemeMode {
    /** Follow the phone's dark mode setting (default). */
    SYSTEM,
    LIGHT,
    DARK,
}

/** Which color palette the app uses; both exist in light and dark versions (Color.kt). */
enum class AppPalette(val label: String) {
    /** "Gazon de noapte / de zi": green background, gold accent (default). */
    GAZON("Gazon"),
    /** "Nocturnă de stadion": blue-grey background, lime accent. */
    STADION("Stadion"),
}

/**
 * Single source of truth for the theme (mode + palette), shared by the whole app (@Singleton).
 * Screens observe the StateFlows, so a change applies everywhere at once. The choice is saved
 * (SharedPreferences), so it survives restarts. In S10 the light sensor can call [setMode].
 */
@Singleton
class ThemeController @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(readEnum(KEY_MODE, ThemeMode.SYSTEM))
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private val _palette = MutableStateFlow(readEnum(KEY_PALETTE, AppPalette.GAZON))
    val palette: StateFlow<AppPalette> = _palette.asStateFlow()

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        prefs.edit { putString(KEY_MODE, mode.name) }
    }

    fun setPalette(palette: AppPalette) {
        _palette.value = palette
        prefs.edit { putString(KEY_PALETTE, palette.name) }
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T =
        prefs.getString(key, null)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_PALETTE = "palette"
    }
}
