package ro.fluierulfinal.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ro.fluierulfinal.domain.model.FormLetter
import ro.fluierulfinal.domain.model.StandingGroup
import ro.fluierulfinal.domain.model.StandingRow
import ro.fluierulfinal.domain.model.SupportedLeagues
import ro.fluierulfinal.domain.model.TopScorer
import ro.fluierulfinal.ui.components.ErrorState
import ro.fluierulfinal.ui.components.FormPills
import ro.fluierulfinal.ui.components.FormResult
import ro.fluierulfinal.ui.components.MatchCard
import ro.fluierulfinal.ui.components.MatchCardDivider
import ro.fluierulfinal.ui.components.SkeletonMatchList
import ro.fluierulfinal.ui.components.TeamLogo
import ro.fluierulfinal.ui.theme.ExtendedColors
import ro.fluierulfinal.ui.theme.Spacing
import ro.fluierulfinal.ui.theme.extendedColors
import ro.fluierulfinal.ui.theme.scoreType

/** The views of the "Clasament" tab, as on Flashscore. */
enum class StandingsView(val label: String) {
    TOTAL("Total"),
    HOME("Acasă"),
    AWAY("Deplasare"),
    FORM("Formă"),
    SCORERS("Golgheteri"),
}

/**
 * "Clasament" tab of a league: season banner, a row of view chips (Total / Acasă / Deplasare /
 * Formă / Golgheteri) and, for the tables, one card per group (regular season, play-off, play-out).
 * Total / Acasă / Deplasare / Formă all come from the same /standings response; only Golgheteri
 * needs a second request, made the first time that view is opened.
 */
@Composable
fun StandingsTab(
    state: StandingsState,
    scorers: ScorersState,
    onRetry: () -> Unit,
    onScorersNeeded: () -> Unit,
    onRetryScorers: () -> Unit,
) {
    var view by rememberSaveable { mutableStateOf(StandingsView.TOTAL) }
    if (view == StandingsView.SCORERS) LaunchedEffect(Unit) { onScorersNeeded() }

    when (state) {
        StandingsState.NotLoaded, StandingsState.Loading -> SkeletonMatchList(rows = 6)
        is StandingsState.Error -> ErrorState(state.message, onRetry = onRetry)
        is StandingsState.Loaded -> {
            // one table at a time; the regular season first, then play-off / play-out (where they exist)
            val groups = state.standings.groups.sortedBy { groupOrder(it.name) }
            var groupName by rememberSaveable { mutableStateOf(groups.first().name) }
            val group = groups.firstOrNull { it.name == groupName } ?: groups.first()
            val ext = MaterialTheme.extendedColors
            val primary = MaterialTheme.colorScheme.primary
            LazyColumn(contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.xl)) {
                item { SeasonBanner(state.standings.season) }
                item { ViewChips(view) { view = it } }

                if (view == StandingsView.SCORERS) {
                    item { ScorersContent(scorers, onRetryScorers) }
                } else {
                    if (groups.size > 1) {
                        item { GroupSelector(groups, group.name) { groupName = it } }
                    } else {
                        item { Spacer(Modifier.height(Spacing.xs)) }
                    }
                    item(key = "t_${group.name}_$view") { StandingsTable(group, view, ext, primary) }
                    item { Legend(listOf(group), view, ext, primary) }
                }
            }
        }
    }
}

/**
 * Which table of a split league to show (Liga I: Sezon regulat / Play-off / Play-out).
 * Segmented buttons, so it reads as a choice between parts of the season, separate from the view chips above.
 */
@Composable
private fun GroupSelector(groups: List<StandingGroup>, selected: String, onSelect: (String) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
        groups.forEachIndexed { i, g ->
            SegmentedButton(
                selected = g.name == selected,
                onClick = { onSelect(g.name) },
                shape = SegmentedButtonDefaults.itemShape(i, groups.size),
                label = { Text(groupLabel(g.name), maxLines = 1) },
            )
        }
    }
}

/** Regular season first, then the championship round (play-off), then the relegation round (play-out). */
private fun groupOrder(name: String): Int = when (name.lowercase()) {
    "regular season", "league stage", "league phase" -> 0
    "championship round" -> 1
    "relegation round" -> 2
    else -> 3
}

@Composable
private fun ViewChips(selected: StandingsView, onSelect: (StandingsView) -> Unit) {
    val colors = MaterialTheme.colorScheme
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(vertical = Spacing.md),
    ) {
        items(StandingsView.entries) { v ->
            FilterChip(
                selected = v == selected,
                onClick = { onSelect(v) },
                label = { Text(v.label) },
                shape = CircleShape,
                // selected view: the theme accent (gold in dark, grass green in light)
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = colors.onPrimary,
                ),
            )
        }
    }
}

/** Says clearly which season is shown and why it is not the current one. */
@Composable
private fun SeasonBanner(season: Int) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(top = Spacing.md).clip(MaterialTheme.shapes.medium).background(colors.surfaceVariant).padding(Spacing.md),
    ) {
        Text("Sezonul $season/${(season + 1) % 100}", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
        if (season != SupportedLeagues.CURRENT_SEASON) {
            Text(
                "Ultimul sezon complet disponibil pe planul gratuit API-Football. Clasamentul curent necesită planul complet.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tables: Total / Acasă / Deplasare / Formă
// ---------------------------------------------------------------------------------------------

private val COL_SMALL: Dp = 28.dp
private val COL_DIFF: Dp = 36.dp
private val COL_POINTS: Dp = 32.dp

/** One displayed line, whatever the view: Home/Away tables are recomputed from those matches only. */
private data class TableLine(
    val rank: Int, val row: StandingRow, val played: Int, val win: Int, val draw: Int, val lose: Int,
    val goalDiff: Int, val points: Int,
)

private fun linesFor(group: StandingGroup, view: StandingsView): List<TableLine> = when (view) {
    StandingsView.HOME, StandingsView.AWAY -> group.rows
        .map { r -> r to (if (view == StandingsView.HOME) r.home else r.away) }
        // ranking for a home/away-only table: points, then goal difference, then goals scored
        .sortedWith(compareByDescending<Pair<StandingRow, ro.fluierulfinal.domain.model.StandingRecord>> { it.second.points }
            .thenByDescending { it.second.goalDiff }.thenByDescending { it.second.goalsFor }.thenBy { it.first.team.name })
        .mapIndexed { i, (r, rec) -> TableLine(i + 1, r, rec.played, rec.win, rec.draw, rec.lose, rec.goalDiff, rec.points) }
    else -> group.rows.map { r -> TableLine(r.rank, r, r.played, r.win, r.draw, r.lose, r.goalDiff, r.points) }
}

@Composable
private fun StandingsTable(group: StandingGroup, view: StandingsView, ext: ExtendedColors, primary: Color) {
    val colors = MaterialTheme.colorScheme
    val lines = linesFor(group, view)
    val form = view == StandingsView.FORM
    // narrow phones (< 400dp, e.g. Pixel 4a): drop the W/D/L columns so team names stay readable
    val showWdl = LocalConfiguration.current.screenWidthDp >= 400
    MatchCard {
        Row(
            Modifier.fillMaxWidth().padding(start = Spacing.lg + 4.dp, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCell("#", 24.dp, TextAlign.Start)
            Text("Echipă", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f).padding(start = 30.dp))
            if (form) {
                HeaderCell("Ultimele 5", 136.dp)
            } else {
                HeaderCell("M", COL_SMALL)
                if (showWdl) { HeaderCell("V", COL_SMALL); HeaderCell("E", COL_SMALL); HeaderCell("Î", COL_SMALL) }
                HeaderCell("GD", COL_DIFF)
            }
            HeaderCell("P", COL_POINTS)
        }
        lines.forEachIndexed { i, line ->
            if (i > 0) MatchCardDivider()
            // the zone strip (Europe, relegation...) belongs to the real final position, so only on Total and Formă
            val zone = if (view == StandingsView.TOTAL || form) zoneColor(line.row.description, ext, primary) else null
            LineView(line, zone, form, showWdl)
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp, align: TextAlign = TextAlign.End) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = align, modifier = Modifier.width(width))
}

@Composable
private fun LineView(line: TableLine, zone: Color?, form: Boolean, showWdl: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).fillMaxHeight().padding(vertical = 10.dp).background(zone ?: Color.Transparent, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)))
        Row(Modifier.weight(1f).padding(start = Spacing.lg, end = Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Text(line.rank.toString(), style = MaterialTheme.scoreType.minute, color = colors.onSurfaceVariant, modifier = Modifier.width(24.dp))
            TeamLogo(line.row.team.name, line.row.team.logoUrl, size = 22.dp)
            Text(
                line.row.team.name, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            if (form) {
                FormPills(line.row.form.take(5).map { it.toFormResult() })
            } else {
                NumberCell(line.played.toString(), COL_SMALL)
                if (showWdl) {
                    NumberCell(line.win.toString(), COL_SMALL)
                    NumberCell(line.draw.toString(), COL_SMALL)
                    NumberCell(line.lose.toString(), COL_SMALL)
                }
                NumberCell(if (line.goalDiff > 0) "+${line.goalDiff}" else line.goalDiff.toString(), COL_DIFF)
            }
            Text(
                line.points.toString(), style = MaterialTheme.scoreType.minute, fontWeight = FontWeight.Bold,
                color = colors.onSurface, textAlign = TextAlign.End, modifier = Modifier.width(COL_POINTS),
            )
        }
    }
}

@Composable
private fun NumberCell(text: String, width: Dp) {
    Text(text, style = MaterialTheme.scoreType.minute, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(width))
}

private fun FormLetter.toFormResult() = when (this) {
    FormLetter.WIN -> FormResult.WIN
    FormLetter.DRAW -> FormResult.DRAW
    FormLetter.LOSS -> FormResult.LOSS
}

// ---------------------------------------------------------------------------------------------
// Golgheteri
// ---------------------------------------------------------------------------------------------

@Composable
private fun ScorersContent(state: ScorersState, onRetry: () -> Unit) {
    when (state) {
        ScorersState.NotLoaded, ScorersState.Loading -> SkeletonMatchList(rows = 6)
        is ScorersState.Error -> Box(Modifier.height(320.dp)) { ErrorState(state.message, onRetry = onRetry) }
        is ScorersState.Loaded -> {
            val colors = MaterialTheme.colorScheme
            MatchCard {
                Row(Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs)) {
                    HeaderCell("#", 24.dp, TextAlign.Start)
                    Text("Jucător", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f).padding(start = 42.dp))
                    HeaderCell("A", COL_SMALL); HeaderCell("Pen", 34.dp); HeaderCell("G", COL_POINTS)
                }
                state.scorers.forEachIndexed { i, s ->
                    if (i > 0) MatchCardDivider()
                    ScorerRow(s)
                }
            }
            Text(
                "G = goluri · A = pase decisive · Pen = goluri din penalty",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.md, start = Spacing.xs),
            )
        }
    }
}

@Composable
private fun ScorerRow(s: TopScorer) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Text(s.rank.toString(), style = MaterialTheme.scoreType.minute, color = colors.onSurfaceVariant, modifier = Modifier.width(24.dp))
        TeamLogo(s.name, s.photoUrl, size = 34.dp)
        Column(Modifier.weight(1f).padding(start = Spacing.sm)) {
            Text(s.name, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(s.team.name, s.team.logoUrl, size = 14.dp)
                Text(
                    s.team.name, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = Spacing.xs),
                )
            }
        }
        NumberCell(s.assists.toString(), COL_SMALL)
        NumberCell(s.penalties.toString(), 34.dp)
        Text(
            s.goals.toString(), style = MaterialTheme.scoreType.scoreList, color = colors.onSurface,
            textAlign = TextAlign.End, modifier = Modifier.width(COL_POINTS),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Zones, legend, Romanian labels
// ---------------------------------------------------------------------------------------------

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

/** Legend for the strip colors actually used in this table, plus the column abbreviations. */
@Composable
private fun Legend(groups: List<StandingGroup>, view: StandingsView, ext: ExtendedColors, primary: Color) {
    val colors = MaterialTheme.colorScheme
    val showZones = view == StandingsView.TOTAL || view == StandingsView.FORM
    val entries = if (!showZones) emptyList() else groups.flatMap { it.rows }.mapNotNull { it.description }.distinct()
        .mapNotNull { d -> zoneColor(d, ext, primary)?.let { it to descriptionLabel(d) } }
        .distinctBy { it.second }
    Column(Modifier.padding(top = Spacing.lg, start = Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        entries.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = Spacing.sm))
            }
        }
        Text(
            when (view) {
                StandingsView.FORM -> "V = victorie · E = egal · Î = înfrângere · cel mai recent meci e primul · P = puncte"
                StandingsView.HOME -> "Doar meciurile jucate acasă · M = meciuri · V/E/Î = victorii/egaluri/înfrângeri · GD = golaveraj · P = puncte"
                StandingsView.AWAY -> "Doar meciurile jucate în deplasare · M = meciuri · V/E/Î = victorii/egaluri/înfrângeri · GD = golaveraj · P = puncte"
                else -> "M = meciuri · V/E/Î = victorii/egaluri/înfrângeri · GD = golaveraj · P = puncte"
            },
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
        )
    }
}

/** Romanian names for the API's group names (Liga I has a regular season, then play-off / play-out). */
private fun groupLabel(name: String): String = when (name.lowercase()) {
    "regular season" -> "Sezon regulat"
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
