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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ro.fluierulfinal.domain.model.AlertSettings
import ro.fluierulfinal.domain.model.Favorite
import ro.fluierulfinal.ui.components.AppIcons
import ro.fluierulfinal.ui.components.EmptyState
import ro.fluierulfinal.ui.components.ErrorState
import ro.fluierulfinal.ui.components.SectionLabel
import ro.fluierulfinal.ui.components.TeamLogo
import ro.fluierulfinal.ui.components.shimmer
import ro.fluierulfinal.ui.theme.Spacing

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
    val colors = MaterialTheme.colorScheme

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
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Favorite") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state.favorites.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddTeam,
                    icon = { Icon(AppIcons.add, null) },
                    text = { Text("Adaugă echipă") },
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && state.favorites.isEmpty() -> FavoritesSkeleton()
                state.error != null && state.favorites.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel::refresh)
                state.favorites.isEmpty() -> EmptyState(
                    icon = AppIcons.star,
                    title = "Nicio echipă favorită",
                    text = "Adaugă echipele pe care le urmărești, ca să le vezi meciurile și să primești alerte.",
                    action = "Adaugă echipă", onAction = onAddTeam,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    item { SectionLabel("${state.favorites.size} ${if (state.favorites.size == 1) "echipă" else "echipe"}", Modifier.padding(top = 0.dp)) }
                    items(state.favorites, key = { it.teamId }) { favorite ->
                        SwipeToDelete(onSwiped = { toDelete = favorite }) {
                            FavoriteRow(favorite, onClick = { onOpenTeam(favorite.teamId) }, onAlerts = { alertsFor = favorite })
                        }
                    }
                    item {
                        Text(
                            "Glisează spre stânga ca să ștergi o echipă.",
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
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
 * Swipe from right to left reveals a red background; the trash icon fades and grows in as the row moves.
 * The row springs back right away; the actual delete happens only after the confirmation dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        backgroundContent = { DeleteBackground(state) },
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteBackground(state: SwipeToDismissBoxState) {
    // 0 when the row is at rest, growing to 1 as it is dragged towards the delete threshold
    val progress = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) (state.progress * 2.5f).coerceIn(0f, 1f) else 0f
    Box(
        Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.error).padding(horizontal = Spacing.xl),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            AppIcons.delete, contentDescription = "Șterge", tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.graphicsLayer { alpha = progress; scaleX = 0.6f + 0.4f * progress; scaleY = 0.6f + 0.4f * progress },
        )
    }
}

@Composable
private fun FavoriteRow(favorite: Favorite, onClick: () -> Unit, onAlerts: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val activeAlerts = alertLabels(favorite.alerts)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(colors.surface)
            .clickable(onClick = onClick).padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs),
    ) {
        TeamLogo(favorite.name, favorite.logoUrl, size = 40.dp)
        Column(Modifier.weight(1f).padding(start = Spacing.lg)) {
            Text(favorite.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                favorite.country ?: "—", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onAlerts) {
            Icon(
                if (activeAlerts.isNotEmpty()) AppIcons.notificationsFilled else AppIcons.notifications,
                contentDescription = if (activeAlerts.isNotEmpty()) "Alerte: ${activeAlerts.joinToString()}" else "Setări alerte",
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

/** Loading placeholder shaped like the favorite rows. */
@Composable
private fun FavoritesSkeleton() {
    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Spacer(Modifier.height(Spacing.xl))
        repeat(4) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface).padding(Spacing.lg),
            ) {
                Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).shimmer())
                Column(Modifier.padding(start = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Box(Modifier.fillMaxWidth(0.5f).height(14.dp).clip(MaterialTheme.shapes.small).shimmer())
                    Box(Modifier.fillMaxWidth(0.3f).height(12.dp).clip(MaterialTheme.shapes.small).shimmer())
                }
            }
        }
    }
}

/** Update part of the CRUD: the four alert toggles, saved as soon as one is flipped (with a short haptic tick). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertSettingsSheet(favorite: Favorite, onChange: (AlertSettings) -> Unit, onDismiss: () -> Unit) {
    val a = favorite.alerts
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = Spacing.xl).padding(bottom = Spacing.xxl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(favorite.name, favorite.logoUrl, size = 40.dp)
                Column(Modifier.padding(start = Spacing.md)) {
                    Text("Setări alerte", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(favorite.name, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                }
            }
            Spacer(Modifier.height(Spacing.md))
            AlertToggle("Sunet la gol", "Un sunet când echipa marchează", a.goalSound) { onChange(a.copy(goalSound = it)) }
            HorizontalDivider(color = colors.outline)
            AlertToggle("Vibrație la cartonaș roșu", null, a.redCardVibrate) { onChange(a.copy(redCardVibrate = it)) }
            HorizontalDivider(color = colors.outline)
            AlertToggle("Vibrație la penalty", null, a.penaltyVibrate) { onChange(a.copy(penaltyVibrate = it)) }
            HorizontalDivider(color = colors.outline)
            AlertToggle("Echipele de start", "Anunț când apar formațiile, de obicei cu o oră înainte", a.lineups) { onChange(a.copy(lineups = it)) }
            Text(
                "Setările se salvează imediat. Alertele propriu-zise vin în S10.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.md),
            )
        }
    }
}

@Composable
private fun AlertToggle(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val toggle: (Boolean) -> Unit = { on ->
        haptics.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
        onCheckedChange(on)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { toggle(!checked) }.padding(vertical = Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = toggle)
    }
}
