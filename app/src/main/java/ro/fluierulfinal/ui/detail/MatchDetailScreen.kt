package ro.fluierulfinal.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.fluierulfinal.data.EventType
import ro.fluierulfinal.data.MatchEvent
import ro.fluierulfinal.data.MatchStatus
import ro.fluierulfinal.data.StatLine
import ro.fluierulfinal.data.Team
import ro.fluierulfinal.ui.components.LeagueBadge
import ro.fluierulfinal.ui.components.TeamCrest
import ro.fluierulfinal.ui.components.TvIcon
import ro.fluierulfinal.ui.components.longDayLabel
import ro.fluierulfinal.ui.components.statusLabel
import ro.fluierulfinal.ui.home.HomeViewModel

/** Detaliile unui meci: scor, evenimente, statistici. */
@Composable
fun MatchDetailScreen(matchId: Int, vm: HomeViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val match = vm.repository.match(matchId)
    val league = vm.repository.league(match.leagueId)
    val home = vm.repository.team(match.homeId)
    val away = vm.repository.team(match.awayId)
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Surface(color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
            ) {
                IconButton(onClick = onBack) { Text("←", fontSize = 22.sp) }
                LeagueBadge(league, 24.dp)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(league.name, fontWeight = FontWeight.SemiBold)
                    Text(longDayLabel(match.date), fontSize = 12.sp, color = colors.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .navigationBarsPadding()
        ) {
            // Cardul cu scorul mare, cu echipele de o parte și de alta
            Column(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(16.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                TeamColumn(home, home.id in state.favoriteTeamIds, Modifier.weight(1f)) { vm.toggleFavorite(home.id) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (match.homeGoals != null) "${match.homeGoals} - ${match.awayGoals}" else match.kickoff,
                        fontSize = 40.sp, fontWeight = FontWeight.Black,
                    )
                    Text(
                        when (match.status) {
                            MatchStatus.LIVE -> "Live · ${statusLabel(match)}"
                            MatchStatus.FINISHED -> "Final"
                            MatchStatus.UPCOMING -> "Începe la ${match.kickoff}"
                        },
                        color = if (match.status == MatchStatus.LIVE) colors.error else colors.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    )
                }
                TeamColumn(away, away.id in state.favoriteTeamIds, Modifier.weight(1f)) { vm.toggleFavorite(away.id) }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TvIcon(colors.onSurfaceVariant, Modifier.size(18.dp))
                Text(match.tvChannel, color = colors.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            }
            }

            if (match.events.isNotEmpty()) {
                Section("Evenimente") {
                    match.events.forEach { EventRow(it) }
                }
            }

            val stats = vm.repository.statsFor(match)
            if (stats.isNotEmpty()) {
                Section("Statistici") {
                    stats.forEach { StatRow(it) }
                }
            } else {
                Section("Înainte de meci") {
                    Text(
                        "Echipele de start apar cu 30–60 de minute înainte de fluierul de start.",
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TeamColumn(team: Team, isFavorite: Boolean, modifier: Modifier, onToggleFavorite: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        TeamCrest(team, 52.dp)
        Text(team.name, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(
            if (isFavorite) "★ Favorită" else "☆ Adaugă",
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (isFavorite) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, if (isFavorite) colors.primary else colors.outline, RoundedCornerShape(50))
                .clickable(onClick = onToggleFavorite)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** O secțiune pe card separat, ca la Sofascore. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

/** Un eveniment: gazdele în stânga, oaspeții în dreapta, minutul la mijloc. */
@Composable
private fun EventRow(event: MatchEvent) {
    val icon: @Composable () -> Unit = {
        when (event.type) {
            EventType.GOAL -> Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
            EventType.YELLOW_CARD -> Box(Modifier.size(9.dp, 12.dp).background(Color(0xFFF5CC2E), RoundedCornerShape(2.dp)))
            EventType.RED_CARD -> Box(Modifier.size(9.dp, 12.dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp)))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (event.isHome) {
                Text(event.player, fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
                icon()
            }
        }
        Text(
            "${event.minute}'", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.width(48.dp),
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (!event.isHome) {
                icon()
                Text(event.player, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

/** O statistică: valorile la capete și o bară împărțită proporțional. */
@Composable
private fun StatRow(stat: StatLine) {
    val colors = MaterialTheme.colorScheme
    val total = (stat.home + stat.away).coerceAtLeast(1)
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(stat.label, fontSize = 12.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text("${stat.home}", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
            Row(Modifier.weight(1f).height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                // weight trebuie să fie > 0, de aici coerceAtLeast
                Box(
                    Modifier.weight(stat.home.toFloat().coerceAtLeast(0.01f) / total).fillMaxSize().clip(RoundedCornerShape(3.dp))
                        .background(if (stat.home >= stat.away) colors.primary else colors.outline)
                )
                Box(
                    Modifier.weight(stat.away.toFloat().coerceAtLeast(0.01f) / total).fillMaxSize().clip(RoundedCornerShape(3.dp))
                        .background(if (stat.away > stat.home) colors.primary else colors.outline)
                )
            }
            Text("${stat.away}", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
        }
    }
}
