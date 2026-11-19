package ro.fluierulfinal.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.fluierulfinal.domain.model.Team
import ro.fluierulfinal.ui.components.TeamLogo

/** "Adaugă echipă": search box + results, each with an Add button (Create part of the CRUD). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFavoriteScreen(onBack: () -> Unit, viewModel: AddFavoriteViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }   // keyboard opens right away

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adaugă echipă") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Înapoi") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Caută după nume, ex. Rapid") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (state.searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).focusRequester(focus),
            )

            val q = state.query.trim()
            when {
                state.error != null -> Hint(state.error!!, colors.error)
                q.isNotEmpty() && q.length < AddFavoriteViewModel.MIN_QUERY -> Hint("Scrie cel puțin ${AddFavoriteViewModel.MIN_QUERY} litere.")
                state.searchedOnce && state.results.isEmpty() && !state.searching -> Hint("Nicio echipă găsită pentru „$q”.")
                q.isEmpty() -> Hint("Caută echipa pe care vrei s-o urmărești. Poți căuta în orice țară.")
            }

            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.results, key = { it.id }) { team ->
                    ResultRow(team, added = team.id in state.favoriteIds, onAdd = { viewModel.add(team) })
                }
            }
        }
    }
}

@Composable
private fun ResultRow(team: Team, added: Boolean, onAdd: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        TeamLogo(team.name, team.logoUrl, size = 36.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(team.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(team.country ?: "", fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        if (added) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Text("Adăugată", color = colors.primary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
            }
        } else {
            FilledTonalButton(onClick = onAdd) { Text("Adaugă") }
        }
    }
}

@Composable
private fun Hint(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(text, color = color, fontSize = 14.sp)
    }
}
