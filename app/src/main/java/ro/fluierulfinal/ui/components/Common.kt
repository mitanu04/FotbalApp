package ro.fluierulfinal.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import ro.fluierulfinal.R
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.domain.model.Score
import ro.fluierulfinal.domain.model.Team
import ro.fluierulfinal.ui.theme.BarlowCondensed
import ro.fluierulfinal.ui.theme.FluierulFinalTheme
import ro.fluierulfinal.ui.theme.ThemeMode
import java.time.Instant
import java.time.LocalDate

// Small UI pieces shared by several screens.

/**
 * Team logo loaded by Coil from [logoUrl], fading in when it arrives (crossfade).
 * While loading, offline, or without a logo: a circle with the team's initials.
 */
@Composable
fun TeamLogo(name: String, logoUrl: String?, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    if (logoUrl == null) {
        InitialsCircle(name, size, modifier)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(logoUrl).crossfade(true).build(),
        contentDescription = "Sigla $name",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
        loading = { InitialsCircle(name, size) },
        error = { InitialsCircle(name, size) },
    )
}

/** League logo (same fallback as [TeamLogo]). */
@Composable
fun LeagueLogo(name: String, logoUrl: String?, size: Dp = 20.dp, modifier: Modifier = Modifier) =
    TeamLogo(name, logoUrl, size, modifier)

/** Circle with up to two initials, e.g. "FC" for "FC Botosani", in neutral theme colors. */
@Composable
private fun InitialsCircle(name: String, size: Dp, modifier: Modifier = Modifier) {
    val initials = name.filter { it.isUpperCase() }.take(2).ifEmpty { name.take(1) }
    Box(
        modifier = modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = BarlowCondensed,
            fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.labelSmall.fontSize * (size.value / 24f),
        )
    }
}

/** Material Symbols Rounded icons (vector drawables in res/drawable). Filled variants mark the active tab. */
object AppIcons {
    val star @Composable get() = painterResource(R.drawable.ic_star)
    val starFilled @Composable get() = painterResource(R.drawable.ic_star_filled)
    val explore @Composable get() = painterResource(R.drawable.ic_explore)
    val exploreFilled @Composable get() = painterResource(R.drawable.ic_explore_filled)
    val ticket @Composable get() = painterResource(R.drawable.ic_confirmation_number)
    val ticketFilled @Composable get() = painterResource(R.drawable.ic_confirmation_number_filled)
    val settings @Composable get() = painterResource(R.drawable.ic_settings)
    val settingsFilled @Composable get() = painterResource(R.drawable.ic_settings_filled)
    val notifications @Composable get() = painterResource(R.drawable.ic_notifications)
    val notificationsFilled @Composable get() = painterResource(R.drawable.ic_notifications_filled)
    val delete @Composable get() = painterResource(R.drawable.ic_delete)
    val add @Composable get() = painterResource(R.drawable.ic_add)
    val search @Composable get() = painterResource(R.drawable.ic_search)
    val back @Composable get() = painterResource(R.drawable.ic_arrow_back)
    val check @Composable get() = painterResource(R.drawable.ic_check)
    val chevronRight @Composable get() = painterResource(R.drawable.ic_chevron_right)
    val ball @Composable get() = painterResource(R.drawable.ic_sports_soccer)
}

/** Kept for the old login header; replaced by the new login design. */
@Composable
fun WhistleLogo(modifier: Modifier = Modifier) {
    val fg = MaterialTheme.colorScheme.onPrimaryContainer
    val hole = MaterialTheme.colorScheme.primaryContainer
    Canvas(modifier.size(28.dp)) {
        val w = size.width
        drawRoundRect(fg, Offset(0f, w * 0.28f), Size(w * 0.68f, w * 0.44f), CornerRadius(w * 0.22f))
        drawRoundRect(fg, Offset(w * 0.55f, w * 0.28f), Size(w * 0.42f, w * 0.18f), CornerRadius(w * 0.05f))
        drawCircle(hole, w * 0.09f, Offset(w * 0.34f, w * 0.5f))
    }
}

private val DAYS = listOf("Luni", "Marți", "Miercuri", "Joi", "Vineri", "Sâmbătă", "Duminică")
private val MONTHS = listOf("ian", "feb", "mar", "apr", "mai", "iun", "iul", "aug", "sep", "oct", "nov", "dec")

/** "Azi", "Ieri", "Mâine" or e.g. "Mie, 28 oct" (UI text, so in Romanian). */
fun shortDayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Azi"
    today.minusDays(1) -> "Ieri"
    today.plusDays(1) -> "Mâine"
    else -> "${DAYS[day.dayOfWeek.value - 1].take(3)}, ${day.dayOfMonth} ${MONTHS[day.monthValue - 1]}"
}

/** e.g. "Luni, 26 oct". */
fun longDayLabel(day: LocalDate): String =
    "${DAYS[day.dayOfWeek.value - 1]}, ${day.dayOfMonth} ${MONTHS[day.monthValue - 1]}"

// ---------------------------------------------------------------------------------------------
// Preview helpers: every component shows itself in both themes with the same sample data.
// ---------------------------------------------------------------------------------------------

/** Wraps preview content in our theme and the screen background color. */
@Composable
fun PreviewSurface(dark: Boolean, content: @Composable () -> Unit) {
    FluierulFinalTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
        Column(Modifier.background(MaterialTheme.colorScheme.background).padding(12.dp)) { content() }
    }
}

/** Light + dark side by side in Android Studio's preview pane. */
@Preview(name = "Light", showBackground = true, widthDp = 380)
@Preview(name = "Dark", showBackground = true, widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews

/** Sample data for previews and the component gallery (real API-Football ids, so logos load). */
object PreviewData {
    private fun team(id: Int, name: String) = Team(id, name, "https://media.api-sports.io/football/teams/$id.png")
    val rapid = team(6231, "Rapid")
    val fcsb = team(559, "FCSB")
    val craiova = team(632, "Universitatea Craiova")
    val cluj = team(2246, "CFR 1907 Cluj")
    private val kickoff: Instant = Instant.parse("2026-11-20T18:30:00Z")

    val live = Fixture(1, 283, 2026, kickoff, FixtureStatus.LIVE, 67, rapid, fcsb, Score(2, 1))
    val scheduled = Fixture(2, 283, 2026, kickoff.plusSeconds(7200), FixtureStatus.SCHEDULED, null, craiova, cluj, Score(null, null))
    val finished = Fixture(3, 283, 2026, kickoff.minusSeconds(86400), FixtureStatus.FINISHED, null, cluj, rapid, Score(0, 0))
    val postponed = Fixture(4, 283, 2026, kickoff, FixtureStatus.POSTPONED, null, fcsb, craiova, Score(null, null))
}
