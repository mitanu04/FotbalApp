package ro.fluierulfinal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Our own palette (not the Material defaults): dark background, slightly lighter cards,
// blue-violet accent, red for live matches.
// What each colorScheme slot means in this app:
//   background        = page background
//   surface           = cards (match lists, sections)
//   surfaceVariant    = grey pills (filters, secondary buttons)
//   primary           = accent (selected tab, arrows, buttons)
//   primaryContainer  = top bar (blue in light mode, dark in dark mode)
//   error             = live (minute, live score)
//   errorContainer    = reddish background of live elements

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B1B23),
    onPrimaryContainer = Color(0xFFF2F2F7),
    secondaryContainer = Color(0xFF2E3270),
    onSecondaryContainer = Color(0xFFE0E2FF),
    background = Color(0xFF121218),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1E1E27),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2A2A35),
    onSurfaceVariant = Color(0xFF9C9CAB),
    surfaceContainer = Color(0xFF1B1B23),
    outline = Color(0xFF2E2E3A),
    error = Color(0xFFFF4F4F),
    errorContainer = Color(0xFF3A1C22),
    onErrorContainer = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF374DF5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF374DF5),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFFDDE1FF),
    onSecondaryContainer = Color(0xFF1A2380),
    background = Color(0xFFEDF1F6),
    onBackground = Color(0xFF222226),
    surface = Color.White,
    onSurface = Color(0xFF222226),
    surfaceVariant = Color(0xFFF0F2F7),
    onSurfaceVariant = Color(0xFF6B6F80),
    surfaceContainer = Color.White,
    outline = Color(0xFFE1E5EC),
    error = Color(0xFFCB1818),
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFFCB1818),
)

/**
 * App theme. [mode] comes from [ThemeController]; SYSTEM follows the phone setting.
 * secondaryContainer / surfaceContainer are set explicitly because the bottom navigation bar uses them.
 */
@Composable
fun FluierulFinalTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
