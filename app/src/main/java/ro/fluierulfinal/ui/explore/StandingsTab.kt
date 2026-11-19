package ro.fluierulfinal.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ro.fluierulfinal.domain.model.StandingGroup
import ro.fluierulfinal.domain.model.StandingRow
import ro.fluierulfinal.domain.model.SupportedLeagues
import ro.fluierulfinal.ui.components.ErrorState
import ro.fluierulfinal.ui.components.MatchCard
import ro.fluierulfinal.ui.components.MatchCardDivider
import ro.fluierulfinal.ui.components.SectionLabel
import ro.fluierulfinal.ui.components.SkeletonMatchList
import ro.fluierulfinal.ui.components.TeamLogo
import ro.fluierulfinal.ui.theme.ExtendedColors
import ro.fluierulfinal.ui.theme.Spacing
import ro.fluierulfinal.ui.theme.extendedColors
import ro.fluierulfinal.ui.theme.scoreType

/** "Clasament" tab of a league: the season banner, one table per group, and a color legend. */
@Composable
fun StandingsTab(state: StandingsState, onRetry: () -> Unit) {
    when (state) {
        StandingsState.NotLoaded, StandingsState.Loading -> SkeletonMatchList(rows = 6)
        is StandingsState.Error -> ErrorState(state.message, onRetry = onRetry)
        is StandingsState.Loaded -> {
            val groups = state.standings.groups
            val ext = MaterialTheme.extendedColors
            val primary = MaterialTheme.colorScheme.primary
            LazyColumn(contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.xl)) {
                item { SeasonBanner(state.standings.season) }
                groups.forEach { group ->
                    item(key = "g_${group.name}") {
                        if (groups.size > 1) SectionLabel(groupLabel(group.name)) else androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.md))
                    }
                    item(key = "t_${group.name}") { StandingsTable(group, ext, primary) }
                }
                item { Legend(groups, ext, primary) }
            }
        }
    }
}

/** Says clearly which season is shown and why it is not the current one. */
@Composable
private fun SeasonBanner(season: Int) {
    val colors = MaterialTheme.colorScheme
    val current = season == SupportedLeagues.CURRENT_SEASON
    Column(
        Modifier.fillMaxWidth().padding(top = Spacing.md).clip(MaterialTheme.shapes.medium).background(colors.surfaceVariant).padding(Spacing.md),
    ) {
        Text("Sezonul $season/${(season + 1) % 100}", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
        if (!current) {
            Text(
                "Ultimul sezon complet disponibil pe planul gratuit API-Football. Clasamentul curent necesită planul complet.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            )
        }
    }
}

private val COL_PLAYED: Dp = 28.dp
private val COL_DIFF: Dp = 36.dp
private val COL_POINTS: Dp = 34.dp

@Composable
private fun StandingsTable(group: StandingGroup, ext: ExtendedColors, primary: Color) {
    val colors = MaterialTheme.colorScheme
    MatchCard {
        // header
        Row(Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs)) {
            Text("#", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(28.dp))
            Text("Echipă", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            HeaderCell("M", COL_PLAYED)
            HeaderCell("GD", COL_DIFF)
            HeaderCell("P", COL_POINTS)
        }
        group.rows.forEachIndexed { i, row ->
            if (i > 0) MatchCardDivider()
            StandingRowView(row, zoneColor(row.description, ext, primary))
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(width))
}

@Composable
private fun StandingRowView(row: StandingRow, zone: Color?) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        // thin colored strip on the left: what this position leads to (Europe, relegation...)
        Box(Modifier.width(4.dp).fillMaxHeight().padding(vertical = 10.dp).background(zone ?: Color.Transparent, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)))
        Row(Modifier.weight(1f).padding(start = Spacing.md, end = Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Text(row.rank.toString(), style = MaterialTheme.scoreType.minute, color = colors.onSurfaceVariant, modifier = Modifier.width(24.dp))
            TeamLogo(row.team.name, row.team.logoUrl, size = 22.dp)
            Text(
                row.team.name, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            NumberCell(row.played.toString(), COL_PLAYED, colors.onSurfaceVariant)
            NumberCell(if (row.goalDiff > 0) "+${row.goalDiff}" else row.goalDiff.toString(), COL_DIFF, colors.onSurfaceVariant)
            NumberCell(row.points.toString(), COL_POINTS, colors.onSurface, bold = true)
        }
    }
}

@Composable
private fun NumberCell(text: String, width: Dp, color: Color, bold: Boolean = false) {
    Text(
        text, style = if (bold) MaterialTheme.scoreType.scoreList.copy(fontSize = MaterialTheme.scoreType.minute.fontSize) else MaterialTheme.scoreType.minute,
        fontWeight = if (bold) FontWeight.Bold else null, color = color, textAlign = TextAlign.End, modifier = Modifier.width(width),
    )
}

/** Colors for the zone strip, from the API's description text. */
private fun zoneColor(description: String?, ext: ExtendedColors, primary: Color): Color? {
    val d = description?.lowercase() ?: return null
    return when {
        "relegation" in d && "round" !in d -> ext.loss
        "champions league" in d -> primary
        "europa league" in d -> ext.ratingGreat
        "conference league" in d -> ext.win
        "promotion" in d -> ext.win
        else -> ext.draw            // play-offs, championship/relegation round, etc.
    }
}

/** Legend for the strip colors actually used in this table, with the descriptions translated. */
@Composable
private fun Legend(groups: List<StandingGroup>, ext: ExtendedColors, primary: Color) {
    val entries = groups.flatMap { it.rows }.mapNotNull { it.description }.distinct()
        .mapNotNull { d -> zoneColor(d, ext, primary)?.let { it to descriptionLabel(d) } }
        .distinctBy { it.second }
    if (entries.isEmpty()) return
    Column(Modifier.padding(top = Spacing.lg, start = Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        entries.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = Spacing.sm))
            }
        }
        Text("M = meciuri jucate · GD = golaveraj · P = puncte", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Romanian names for the API's group names (Liga I has a regular season, then play-off / play-out). */
private fun groupLabel(name: String): String = when (name.lowercase()) {
    "regular season" -> "Sezonul regulat"
    "championship round" -> "Play-off"
    "relegation round" -> "Play-out"
    "league stage", "league phase" -> "Faza principală"
    else -> name
}

/** Romanian text for the most common API descriptions; anything else is shown as received. */
private fun descriptionLabel(d: String): String {
    val l = d.lowercase()
    return when {
        "champions league" in l && ("qualif" in l || "play-off" in l) -> "Preliminarii Liga Campionilor"
        "champions league" in l -> "Liga Campionilor"
        "europa league" in l && ("qualif" in l || "play-off" in l) -> "Preliminarii Europa League"
        "europa league" in l -> "Europa League"
        "conference league" in l && ("qualif" in l || "play-off" in l) -> "Preliminarii Conference League"
        "conference league" in l -> "Conference League"
        "relegation" in l && "round" !in l -> if ("play" in l) "Baraj de menținere" else "Retrogradare"
        "promotion" in l -> if ("play" in l) "Baraj de promovare" else "Promovare"
        "championship round" in l -> "Calificare în play-off"
        "relegation round" in l -> "Play-out"
        else -> d
    }
}
