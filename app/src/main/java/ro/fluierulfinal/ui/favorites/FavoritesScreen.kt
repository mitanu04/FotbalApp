package ro.fluierulfinal.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ro.fluierulfinal.domain.model.AlertSettings
import ro.fluierulfinal.domain.model.Favorite
import ro.fluierulfinal.ui.components.TeamLogo

/**
 * Favorite tab (Read + Update + Delete of the favorites CRUD; Create is the "Adaugă echipă" screen).
 * Tap a team → team details (S10 placeholder); bell → alert settings; swipe left → delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onAddTeam: () -> Unit,
    onOpenTeam: (teamId: Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var alertsFor by remember { mutableStateOf<Favorite?>(null) }
    var toDelete by remember { mutableStateOf<Favorite?>(null) }

    // Snackbar for errors and "removed" with an Undo action; events are shown one after another.
    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            val result = snackbar.showSnackbar(
                message = e.message,
                actionLabel = if (e.undo != null) "Anulează" else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed && e.undo != null) viewModel.undoRemove(e.undo)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Favorite") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state.favorites.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = onAddTeam, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Adaugă echipă") })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && state.favorites.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null && state.favorites.isEmpty() -> CenteredMessage(
                    title = "Ceva n-a mers",
                    text = state.error!!,
                    button = "Reîncearcă", onClick = viewModel::refresh,
                )
                state.favorites.isEmpty() -> CenteredMessage(
                    title = "Nicio echipă favorită încă",
                    text = "Adaugă echipele pe care le urmărești, ca să le vezi meciurile și să primești alerte.",
                    button = "Adaugă echipă", onClick = onAddTeam,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.favorites, key = { it.teamId }) { favorite ->
                        SwipeToDelete(onSwiped = { toDelete = favorite }) {
                            FavoriteRow(favorite, onClick = { onOpenTeam(favorite.teamId) }, onAlerts = { alertsFor = favorite })
                        }
                    }
                    item {
                        Text(
                            "Glisează spre stânga ca să ștergi o echipă.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { favorite ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Ștergi ${favorite.name}?") },
            text = { Text("Echipa dispare din favorite, împreună cu setările ei de alerte.") },
            confirmButton = { TextButton(onClick = { toDelete = null; viewModel.remove(favorite) }) { Text("Șterge") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Anulează") } },
        )
    }

    alertsFor?.let { favorite ->
        // read the live version, so toggles show immediately
        val current = state.favorites.firstOrNull { it.teamId == favorite.teamId } ?: favorite
        AlertSettingsSheet(
            favorite = current,
            onChange = { viewModel.updateAlerts(current.teamId, it) },
            onDismiss = { alertsFor = null },
        )
    }
}

/**
 * Swipe from right to left reveals a red "delete" background. The row springs back right away;
 * the actual delete happens only after the confirmation dialog.
 */
@Composable
private fun SwipeToDelete(onSwiped: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onSwiped()
            scope.launch { state.reset() }
        },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.error).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Șterge", tint = Color.White)
            }
        },
    ) { content() }
}

@Composable
private fun FavoriteRow(favorite: Favorite, onClick: () -> Unit, onAlerts: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val activeAlerts = alertLabels(favorite.alerts)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface)
            .clickable(onClick = onClick).padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
    ) {
        TeamLogo(favorite.name, favorite.logoUrl, size = 40.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(favorite.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(favorite.country ?: "—", fontSize = 13.sp, color = colors.onSurfaceVariant)
            if (activeAlerts.isNotEmpty()) {
                Text("Alerte: ${activeAlerts.joinToString(", ")}", fontSize = 12.sp, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onAlerts) {
            Icon(
                if (activeAlerts.isNotEmpty()) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                contentDescription = "Setări alerte",
                tint = if (activeAlerts.isNotEmpty()) colors.primary else colors.onSurfaceVariant,
            )
        }
    }
}

private fun alertLabels(a: AlertSettings) = buildList {
    if (a.goalSound) add("gol")
    if (a.redCardVibrate) add("roșu")
    if (a.penaltyVibrate) add("penalty")
    if (a.lineups) add("echipe de start")
}

/** Update part of the CRUD: the four alert toggles, saved as soon as one is flipped. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertSettingsSheet(favorite: Favorite, onChange: (AlertSettings) -> Unit, onDismiss: () -> Unit) {
    val a = favorite.alerts
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(favorite.name, favorite.logoUrl, size = 32.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Setări alerte", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(favorite.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            AlertToggle("Sunet la gol", "Un sunet când echipa marchează", a.goalSound) { onChange(a.copy(goalSound = it)) }
            HorizontalDivider()
            AlertToggle("Vibrație la cartonaș roșu", null, a.redCardVibrate) { onChange(a.copy(redCardVibrate = it)) }
            HorizontalDivider()
            AlertToggle("Vibrație la penalty", null, a.penaltyVibrate) { onChange(a.copy(penaltyVibrate = it)) }
            HorizontalDivider()
            AlertToggle("Echipele de start", "Anunț când apar formațiile, de obicei cu o oră înainte", a.lineups) { onChange(a.copy(lineups = it)) }
            Text(
                "Setările se salvează imediat. Alertele propriu-zise vin în S10.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun AlertToggle(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp)
            subtitle?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun CenteredMessage(title: String, text: String, button: String?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        if (button != null) Button(onClick = onClick, modifier = Modifier.padding(top = 20.dp)) { Text(button) }
    }
}
