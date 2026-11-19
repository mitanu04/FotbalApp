package ro.fluierulfinal.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

// "Gazon de noapte" (dark, default) and "Gazon de zi" (light).
// Accent: gold in dark; grass green in light for text/icons/selection, while the main action
// (primary button, FAB) stays gold in both themes via `tertiary`, so the brand is recognizable.
// In light, gold is never used as text or icon color on a light background.

private val NightGrass = Color(0xFF0F1A16)
private val Gold = Color(0xFFFFC94D)

val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = NightGrass,
    primaryContainer = Color(0xFF3A3218),
    onPrimaryContainer = Gold,
    secondaryContainer = Color(0xFF3A3218),
    onSecondaryContainer = Gold,
    tertiary = Gold,                              // main action (primary button, FAB)
    onTertiary = NightGrass,
    background = NightGrass,
    onBackground = Color(0xFFE6F0EA),
    surface = Color(0xFF16241F),
    onSurface = Color(0xFFE6F0EA),
    surfaceVariant = Color(0xFF1E3029),
    onSurfaceVariant = Color(0xFF8FA89C),         // secondary text: 6.3:1 on surface
    surfaceContainer = Color(0xFF16241F),         // bottom bar
    surfaceContainerLow = Color(0xFF16241F),      // bottom sheets
    surfaceContainerHigh = Color(0xFF1E3029),     // dialogs, menus
    surfaceContainerHighest = Color(0xFF1E3029),  // text fields, switch tracks
    outline = Color(0xFF2B4038),
    outlineVariant = Color(0xFF2B4038),
    error = Color(0xFFF05252),
    onError = NightGrass,
    errorContainer = Color(0xFF3A1A1A),
    onErrorContainer = Color(0xFFFF6B6B),
    inverseSurface = Color(0xFFE6F0EA),
    inverseOnSurface = NightGrass,
    inversePrimary = Color(0xFF1E6B45),
)

val LightColors = lightColorScheme(
    primary = Color(0xFF1E6B45),                  // grass green: text, active icons, selected chips (6.5:1 on white)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEFE3),
    onPrimaryContainer = Color(0xFF13201A),
    secondaryContainer = Color(0xFFDCEFE3),
    onSecondaryContainer = Color(0xFF13201A),
    tertiary = Gold,                              // ONLY as background of the main action
    onTertiary = Color(0xFF13201A),
    background = Color(0xFFF3F6F1),
    onBackground = Color(0xFF13201A),
    surface = Color.White,
    onSurface = Color(0xFF13201A),
    surfaceVariant = Color(0xFFE7EEE6),
    onSurfaceVariant = Color(0xFF5E7268),         // secondary text: 5.1:1 on white
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE7EEE6),
    outline = Color(0xFFD3DED3),
    outlineVariant = Color(0xFFD3DED3),
    error = Color(0xFFC93636),
    onError = Color.White,
    errorContainer = Color(0xFFFBE4E4),
    onErrorContainer = Color(0xFF9B2525),
    inverseSurface = Color(0xFF13201A),
    inverseOnSurface = Color(0xFFE6F0EA),
    inversePrimary = Gold,
)

/**
 * Football colors that Material's ColorScheme has no slot for. Read them with
 * `MaterialTheme.extendedColors` (see Theme.kt).
 */
@Immutable
data class ExtendedColors(
    val live: Color,              // live minute, LIVE badge, pulsing dot (never the score itself)
    val liveContainer: Color,     // background of the LIVE badge
    val win: Color,
    val draw: Color,
    val loss: Color,
    val yellowCard: Color,
    val redCard: Color,
    val ratingPoor: Color,        // < 6.0
    val ratingAverage: Color,     // 6.0–6.9
    val ratingGood: Color,        // 7.0–7.9
    val ratingGreat: Color,       // ≥ 8.0
    val textDisabled: Color,      // disabled text, inactive icons
    val logoBackdrop: Color,      // light rounded plate behind logos in dark mode (Transparent in light)
    val skeleton: Color,          // loading placeholders
    val skeletonHighlight: Color, // shimmer highlight
)

val DarkExtendedColors = ExtendedColors(
    live = Color(0xFFFF6B6B),
    liveContainer = Color(0xFF3A1A1A),
    win = Color(0xFF3DDC84),
    draw = Color(0xFF5E7268),
    loss = Color(0xFFF05252),
    yellowCard = Color(0xFFFDE047),
    redCard = Color(0xFFE11D48),
    ratingPoor = Color(0xFFF05252),
    ratingAverage = Color(0xFFF59E0B),
    ratingGood = Color(0xFF3DDC84),
    ratingGreat = Color(0xFF60A5FA),
    textDisabled = Color(0xFF5E7268),
    logoBackdrop = Color(0xFFE6F0EA).copy(alpha = 0.92f),
    skeleton = Color(0xFF1E3029),
    skeletonHighlight = Color(0xFF2B4038),
)

val LightExtendedColors = ExtendedColors(
    live = Color(0xFFD63A3A),
    liveContainer = Color(0xFFFBE4E4),
    win = Color(0xFF1E8E4F),
    draw = Color(0xFF8FA89C),
    loss = Color(0xFFC93636),
    yellowCard = Color(0xFFD9B400),
    redCard = Color(0xFFE11D48),
    ratingPoor = Color(0xFFF05252),
    ratingAverage = Color(0xFFF59E0B),
    ratingGood = Color(0xFF3DDC84),
    ratingGreat = Color(0xFF60A5FA),
    textDisabled = Color(0xFF8FA89C),
    logoBackdrop = Color.Transparent,
    skeleton = Color(0xFFE7EEE6),
    skeletonHighlight = Color(0xFFF6F9F5),
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

private val DarkText = Color(0xFF0F1A16)

/**
 * Text color for a colored pill (form result, rating): dark or white, whichever contrasts more
 * with [fill]. Computed (WCAG contrast), so it stays right if a pill color changes.
 */
fun textOn(fill: Color): Color {
    val opaque = fill.copy(alpha = 1f).toArgb()
    return if (ColorUtils.calculateContrast(DarkText.toArgb(), opaque) >= ColorUtils.calculateContrast(Color.White.toArgb(), opaque)) DarkText else Color.White
}
