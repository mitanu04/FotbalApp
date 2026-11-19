package ro.fluierulfinal.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// "Stadium at night": dark and clean, with a single electric lime accent (the floodlights).
// Contrast was checked (WCAG): every text color is at least 4.5:1 on the background it sits on.

private val Ink = Color(0xFF0D1117)
private val Lime = Color(0xFFD7FF3A)

val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    primaryContainer = Lime,
    onPrimaryContainer = Ink,
    secondaryContainer = Color(0xFF2B3A12),          // selected bottom-bar pill, selected chips
    onSecondaryContainer = Lime,
    background = Ink,
    onBackground = Color(0xFFE8EDF3),
    surface = Color(0xFF151B23),
    onSurface = Color(0xFFE8EDF3),
    surfaceVariant = Color(0xFF1E2631),
    onSurfaceVariant = Color(0xFF8B98A9),             // secondary text, 5.9:1 on surface
    surfaceContainer = Color(0xFF151B23),             // bottom bar
    surfaceContainerLow = Color(0xFF151B23),          // bottom sheets
    surfaceContainerHigh = Color(0xFF1E2631),         // dialogs, menus
    surfaceContainerHighest = Color(0xFF1E2631),      // text fields, switches off
    outline = Color(0xFF2A3441),
    outlineVariant = Color(0xFF2A3441),
    error = Color(0xFFFF4D4F),
    onError = Ink,
    errorContainer = Color(0xFF3A1A1F),
    onErrorContainer = Color(0xFFFF8A8C),
    inverseSurface = Color(0xFFE8EDF3),
    inverseOnSurface = Ink,
    inversePrimary = Color(0xFF3F6212),
)

val LightColors = lightColorScheme(
    primary = Color(0xFF3F6212),                      // dark lime: 7.1:1 on white
    onPrimary = Color.White,
    primaryContainer = Lime,
    onPrimaryContainer = Ink,
    secondaryContainer = Lime,
    onSecondaryContainer = Ink,
    background = Color(0xFFF5F7FA),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = Color(0xFF5B6776),             // 5.8:1 on white
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEEF2F6),
    outline = Color(0xFFDCE3EA),
    outlineVariant = Color(0xFFDCE3EA),
    error = Color(0xFFC2272D),
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF9B1C1F),
    inverseSurface = Ink,
    inverseOnSurface = Color(0xFFE8EDF3),
    inversePrimary = Lime,
)

/**
 * Football colors that Material's ColorScheme has no slot for. Read them with
 * `MaterialTheme.extendedColors` (see Theme.kt). Two kinds:
 * - `…Text` colors are used for text, so they are darker in light mode to keep 4.5:1 contrast;
 * - pill colors (form, ratings) are fills with dark text [onPill] on top, the same in both themes.
 */
@Immutable
data class ExtendedColors(
    val live: Color,              // live minute, live score, pulsing dot
    val liveContainer: Color,     // background of the LIVE badge
    val winText: Color,
    val drawText: Color,
    val lossText: Color,
    val win: Color,               // form pill fills
    val draw: Color,
    val loss: Color,
    val yellowCard: Color,
    val redCard: Color,
    val ratingPoor: Color,        // < 6.0
    val ratingAverage: Color,     // 6.0–6.9
    val ratingGood: Color,        // 7.0–7.9
    val ratingGreat: Color,       // ≥ 8.0
    val onPill: Color,            // text on any of the pill fills above
    val skeleton: Color,          // loading placeholders
    val skeletonHighlight: Color, // shimmer highlight
)

val DarkExtendedColors = ExtendedColors(
    live = Color(0xFFFF4D4F),
    liveContainer = Color(0xFF3A1A1F),
    winText = Color(0xFF22C55E),
    drawText = Color(0xFF94A3B8),
    lossText = Color(0xFFEF4444),
    win = Color(0xFF22C55E),
    draw = Color(0xFF94A3B8),
    loss = Color(0xFFEF4444),
    yellowCard = Color(0xFFFACC15),
    redCard = Color(0xFFE11D48),
    ratingPoor = Color(0xFFEF4444),
    ratingAverage = Color(0xFFF97316),
    ratingGood = Color(0xFF22C55E),
    ratingGreat = Color(0xFF3B82F6),
    onPill = Ink,
    skeleton = Color(0xFF1E2631),
    skeletonHighlight = Color(0xFF2A3441),
)

val LightExtendedColors = ExtendedColors(
    live = Color(0xFFC2272D),                 // #FF4D4F is only 3.3:1 on white
    liveContainer = Color(0xFFFDE8E8),
    winText = Color(0xFF15803D),
    drawText = Color(0xFF64748B),
    lossText = Color(0xFFB91C1C),
    win = Color(0xFF22C55E),
    draw = Color(0xFF94A3B8),
    loss = Color(0xFFEF4444),
    yellowCard = Color(0xFFEAB308),
    redCard = Color(0xFFE11D48),
    ratingPoor = Color(0xFFEF4444),
    ratingAverage = Color(0xFFF97316),
    ratingGood = Color(0xFF22C55E),
    ratingGreat = Color(0xFF3B82F6),
    onPill = Ink,
    skeleton = Color(0xFFEEF2F6),
    skeletonHighlight = Color(0xFFF8FAFC),
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
