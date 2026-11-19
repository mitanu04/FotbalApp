package ro.fluierulfinal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ro.fluierulfinal.ui.theme.Spacing
import ro.fluierulfinal.ui.theme.scoreType
import java.time.LocalDate

private val WEEKDAYS = listOf("LUN", "MAR", "MIE", "JOI", "VIN", "SÂM", "DUM")

/**
 * Day picker for Explorează: "Ieri", "Azi", "Mâine", then weekday + day number.
 * The selected day is filled with the accent; the list starts scrolled to "Azi".
 */
@Composable
fun DateChipsRow(days: List<LocalDate>, selected: LocalDate, today: LocalDate, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem((days.indexOf(today) - 1).coerceAtLeast(0)) }
    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(days) { day -> DateChip(day, today, day == selected) { onSelect(day) } }
    }
}

@Composable
private fun DateChip(day: LocalDate, today: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (top, bottom) = when (day) {
        today -> "AZI" to "${day.dayOfMonth}"
        today.minusDays(1) -> "IERI" to "${day.dayOfMonth}"
        today.plusDays(1) -> "MÂINE" to "${day.dayOfMonth}"
        else -> WEEKDAYS[day.dayOfWeek.value - 1] to "${day.dayOfMonth}"
    }
    // selected day: the theme accent (gold in dark, grass green in light)
    val bg = if (selected) colors.primary else colors.surface
    val fg = if (selected) colors.onPrimary else colors.onSurface
    val fg2 = if (selected) colors.onPrimary else colors.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp).clip(RoundedCornerShape(14.dp)).background(bg)
            .clickable(role = Role.Tab, onClick = onClick).padding(vertical = Spacing.sm),
    ) {
        Text(top, style = MaterialTheme.scoreType.leagueHeader, color = fg2)
        Text(bottom, style = MaterialTheme.scoreType.scoreList, color = fg)
    }
}

@ThemePreviews
@Composable
private fun DateChipsRowPreview() {
    val today = LocalDate.of(2026, 11, 19)
    PreviewSurface(androidx.compose.foundation.isSystemInDarkTheme()) {
        DateChipsRow((-3..4).map { today.plusDays(it.toLong()) }, selected = today, today = today, onSelect = {})
    }
}
