package ro.fluierulfinal.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale: use these instead of ad-hoc numbers, so the whole app breathes the same way. */
@Immutable
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

/** Corners: cards 16dp, bottom sheets 24dp (Material uses extraLarge for them), chips fully round. */
val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** True when the user turned animations off ("Remove animations" in Android accessibility settings). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** The palette currently in use (e.g. for the component gallery, which redraws it in both modes). */
val LocalAppPalette = staticCompositionLocalOf { AppPalette.GAZON }

/**
 * App theme. [mode] and [palette] come from [ThemeController]; SYSTEM follows the phone setting.
 * Dynamic color (wallpaper-based, Android 12+) is deliberately not used: the brand colors always apply.
 */
@Composable
fun FluierulFinalTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    palette: AppPalette = AppPalette.GAZON,
    content: @Composable () -> Unit,
) {
    val dark = mode.isDark()
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(
        LocalExtendedColors provides when (palette) {
            AppPalette.GAZON -> if (dark) GazonDarkExtendedColors else GazonLightExtendedColors
            AppPalette.STADION -> if (dark) StadionDarkExtendedColors else StadionLightExtendedColors
        },
        LocalAppPalette provides palette,
        LocalScoreTypography provides AppScoreTypography,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = when (palette) {
                AppPalette.GAZON -> if (dark) GazonDarkColors else GazonLightColors
                AppPalette.STADION -> if (dark) StadionDarkColors else StadionLightColors
            },
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Whether this mode means dark colors right now (SYSTEM asks the phone). */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** `MaterialTheme.extendedColors.live` etc. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

/** `MaterialTheme.scoreType.scoreList` etc. */
val MaterialTheme.scoreType: ScoreTypography
    @Composable @ReadOnlyComposable get() = LocalScoreTypography.current
