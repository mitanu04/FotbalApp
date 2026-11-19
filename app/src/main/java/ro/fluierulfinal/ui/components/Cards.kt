package ro.fluierulfinal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ro.fluierulfinal.ui.theme.Spacing
import ro.fluierulfinal.ui.theme.scoreType

/**
 * A card: surface color on the page background, 16dp corners, no shadow.
 * Separation comes from surface vs. background, not from borders or elevation.
 */
@Composable
fun MatchCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface),
        content = content,
    )
}

/** Thin divider between rows inside a card, indented to line up with the team names. */
@Composable
fun MatchCardDivider() {
    HorizontalDivider(Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outline)
}

/**
 * League header above a group of fixtures: 20dp logo, uppercase name in Barlow Condensed with
 * letter spacing, country in secondary text. Drawn on the page background, so as a sticky header
 * in a LazyColumn it hides the rows scrolling under it.
 */
@Composable
fun LeagueHeader(name: String, country: String, logoUrl: String?, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().background(colors.background)
            .padding(start = Spacing.xs, end = Spacing.xs, top = Spacing.xl, bottom = Spacing.sm),
    ) {
        LeagueLogo(name, logoUrl, size = 20.dp)
        Text(
            name.uppercase(), style = MaterialTheme.scoreType.leagueHeader, color = colors.onBackground,
            modifier = Modifier.padding(start = Spacing.sm),
        )
        Text(
            "  ·  $country", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant) }
    }
}

/** Section title inside a screen, e.g. "LIVE (2)": same style as the league header, no logo. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text.uppercase(), style = MaterialTheme.scoreType.leagueHeader, color = color,
        modifier = modifier.padding(start = Spacing.xs, top = Spacing.xl, bottom = Spacing.sm),
    )
}

@ThemePreviews
@Composable
private fun LeagueHeaderPreview() {
    PreviewSurface(androidx.compose.foundation.isSystemInDarkTheme()) {
        LeagueHeader("Liga I", "România", "https://media.api-sports.io/football/leagues/283.png", trailing = "3 meciuri")
        MatchCard { MatchRow(PreviewData.live, onClick = {}) }
        LeagueHeader("La Liga", "Spania", "https://media.api-sports.io/football/leagues/140.png")
    }
}
