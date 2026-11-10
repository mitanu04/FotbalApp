package ro.fluierulfinal.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.fluierulfinal.ui.components.LeagueBadge
import ro.fluierulfinal.ui.components.TeamCrest
import ro.fluierulfinal.ui.components.shortDayLabel

// Ferestrele care urcă de jos: alegerea ligilor, echipele favorite, programul TV.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheet(title: String, subtitle: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            content()
        }
    }
}

@Composable
fun LeaguePickerSheet(state: HomeUiState, vm: HomeViewModel, onDismiss: () -> Unit) {
    // copie locală a bifelor; se salvează în ViewModel doar la „Salvează”
    val checked = remember { mutableStateListOf(*state.myLeagueIds.toTypedArray()) }
    BottomSheet("Ligile tale", "Alege ce ligi apar în bara de sus și în listă.", onDismiss) {
        vm.repository.leagues.forEach { league ->
            val isChecked = league.id in checked
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable {
                    if (isChecked) checked.remove(league.id) else checked.add(league.id)
                },
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { if (it) checked.add(league.id) else checked.remove(league.id) },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                LeagueBadge(league, 24.dp)
                Text(league.name, modifier = Modifier.padding(start = 10.dp).weight(1f))
                Text(league.country, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        Button(
            onClick = {
                // păstrăm ordinea din lista completă de ligi
                vm.setMyLeagues(vm.repository.leagues.map { it.id }.filter { it in checked })
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) { Text("Salvează", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun FavoritesSheet(state: HomeUiState, vm: HomeViewModel, onDismiss: () -> Unit) {
    BottomSheet(
        "Echipe favorite",
        "Primești sunet la gol și vibrații la roșu sau penalty pentru aceste echipe.",
        onDismiss,
    ) {
        if (state.favoriteTeamIds.isEmpty()) {
            Text("Nu ai nicio echipă favorită. Caută una cu lupa de sus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.favoriteTeamIds.forEach { id ->
            val team = vm.repository.team(id)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                TeamCrest(team, 26.dp)
                Text(team.name, modifier = Modifier.padding(start = 12.dp).weight(1f))
                TextButton(onClick = { vm.toggleFavorite(id) }) {
                    Text("Șterge", color = MaterialTheme.colorScheme.error)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text("Gata", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TvScheduleSheet(state: HomeUiState, vm: HomeViewModel, onDismiss: () -> Unit) {
    val matches = vm.upcomingFavoriteMatches(state)
    BottomSheet("Program TV", "Meciurile echipelor tale favorite din zilele următoare.", onDismiss) {
        if (matches.isEmpty()) Text("Niciun meci programat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        matches.forEach { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Column(Modifier.width(80.dp)) {
                    Text(shortDayLabel(m.date, vm.repository.today), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(m.kickoff, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("${vm.repository.team(m.homeId).name} – ${vm.repository.team(m.awayId).name}")
                    Text(m.tvChannel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text("Gata", fontWeight = FontWeight.Bold)
        }
    }
}
