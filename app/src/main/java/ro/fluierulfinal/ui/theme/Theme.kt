package ro.fluierulfinal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta în stilul Sofascore: fundal închis, carduri puțin mai deschise,
// accent albastru-violet, roșu pentru live.
// Ce înseamnă fiecare slot din colorScheme la noi:
//   background        = fundalul paginii
//   surface           = cardurile (lista de meciuri, secțiunile din detalii)
//   surfaceVariant    = pastile gri (filtre, butoane secundare)
//   primary           = accentul (tab activ, săgeți, butoane)
//   primaryContainer  = bara de sus (albastră pe light, închisă pe dark, ca în aplicația Sofascore)
//   error             = live (minut, scor live)
//   errorContainer    = fundalul roșiatic al elementelor live

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B1B23),
    onPrimaryContainer = Color(0xFFF2F2F7),
    background = Color(0xFF121218),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1E1E27),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2A2A35),
    onSurfaceVariant = Color(0xFF9C9CAB),
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
    background = Color(0xFFEDF1F6),
    onBackground = Color(0xFF222226),
    surface = Color.White,
    onSurface = Color(0xFF222226),
    surfaceVariant = Color(0xFFF0F2F7),
    onSurfaceVariant = Color(0xFF6B6F80),
    outline = Color(0xFFE1E5EC),
    error = Color(0xFFCB1818),
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFFCB1818),
)

// Tema urmează setarea telefonului; mai târziu o va schimba senzorul de lumină.
@Composable
fun FluierulFinalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
