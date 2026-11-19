package ro.fluierulfinal.ui.league

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.fluierulfinal.ui.components.LeagueBadge
import ro.fluierulfinal.ui.components.longDayLabel
import ro.fluierulfinal.ui.components.shortDayLabel
import ro.fluierulfinal.ui.home.HomeViewModel
import ro.fluierulfinal.ui.home.MatchRow

/**
 * Pagina unei ligi (ca la Sofascore): toate meciurile ligii pe care le avem, grupate pe zile.
 * Zilele ieri/azi/mâine vin din API-Football; celelalte sunt marcate „date de exemplu”.
 * Mai târziu aici adăugăm și clasamentul (/standings).
 */
@Composable
fun LeagueScreen(leagueId: String, vm: HomeViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val league = vm.repository.league(leagueId)
    val colors = MaterialTheme.colorScheme
    val byDay = vm.leagueMatchesByDay(leagueId)

    Column(Modifier.fillMaxSize().background(colors.background)) {
        // antetul: înapoi, insigna, numele și țara ligii
        Surface(color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 10.dp),
            ) {
                IconButton(onClick = onBack) { Text("←", fontSize = 22.sp) }
                LeagueBadge(league, 36.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(league.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(league.country, fontSize = 13.sp, color = colors.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            if (byDay.isEmpty()) {
                item {
                    Text(
                        "Niciun meci în zilele pe care le avem.",
                        color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            byDay.forEach { (day, matches) ->
                // antetul zilei, de ex. „Mâine · Vineri, 20 nov”
                item(key = "day_$day") {
                    val short = shortDayLabel(day, vm.repository.today)
                    val long = longDayLabel(day)
                    // „Azi · Joi, 19 nov”; pentru celelalte zile ajunge data lungă
                    val title = if (short in setOf("Azi", "Ieri", "Mâine")) "$short · $long" else long
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp),
                    ) {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f),
                        )
                        if (!vm.repository.isApiDay(day)) {
                            Text("date de exemplu", fontSize = 12.sp, color = colors.onSurfaceVariant)
                        }
                    }
                }
                // meciurile zilei, într-un card alb
                item(key = "card_$day") {
                    Column(
                        Modifier.padding(horizontal = 12.dp).fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)).background(colors.surface)
                    ) {
                        matches.forEachIndexed { i, match ->
                            if (i > 0) HorizontalDivider(color = colors.outline)
                            MatchRow(match, state, vm, Modifier)
                        }
                    }
                }
            }
        }
    }
}
