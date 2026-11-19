package ro.fluierulfinal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ro.fluierulfinal.R

// Fonts are bundled in res/font (Google Fonts, SIL Open Font License, see licenses/OFL-fonts.txt).
// Barlow Condensed: scores, minutes, screen titles, league headers. Inter: everything else.

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/** Inter is one variable font file; each weight is an instance of it (variation settings are still marked experimental). */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

/** Tabular figures: every digit has the same width, so a score or a minute does not "jump" when it changes. */
private const val TABULAR = "tnum"

/** Football-specific text styles, read with `MaterialTheme.scoreType`. */
@Immutable
data class ScoreTypography(
    val scoreLarge: TextStyle,     // Detalii meci scoreboard
    val scoreList: TextStyle,      // score in a list row
    val minute: TextStyle,         // live minute / kick-off time in a row
    val leagueHeader: TextStyle,   // "LIGA I", uppercase with letter spacing
    val brand: TextStyle,          // app name on the login screen
)

val AppScoreTypography = ScoreTypography(
    scoreLarge = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, fontFeatureSettings = TABULAR),
    scoreList = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR),
    minute = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 18.sp, fontFeatureSettings = TABULAR),
    leagueHeader = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.12.em),
    brand = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 52.sp, letterSpacing = 0.02.em),
)

val LocalScoreTypography = staticCompositionLocalOf { AppScoreTypography }

private val Base = Typography()

/** Material type scale: screen titles in Barlow Condensed (28sp), body text in Inter (15 / 13sp). */
val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp),
    titleMedium = Base.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = Base.titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = Base.bodySmall.copy(fontFamily = Inter, fontSize = 12.sp),
    labelLarge = Base.labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = Base.labelMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = Base.labelSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
)
