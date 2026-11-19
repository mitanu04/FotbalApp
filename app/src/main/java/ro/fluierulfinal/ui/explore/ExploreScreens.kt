package ro.fluierulfinal.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.fluierulfinal.BuildConfig
import ro.fluierulfinal.data.repository.DataSource
import ro.fluierulfinal.domain.model.AppTime
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.ui.components.DateChipsRow
import ro.fluierulfinal.ui.components.LeagueLogo
import ro.fluierulfinal.ui.components.tabListPadding
import ro.fluierulfinal.ui.components.TeamLogo
import ro.fluierulfinal.ui.components.longDayLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Explorează, step 1: choose a league. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onLeagueClick: (leagueId: Int) -> Unit, viewModel: ExploreViewModel = hiltViewModel()) {
    val colors = MaterialTheme.colorScheme
    Scaffold(topBar = { TopAppBar(title = { Text("Explorează") }) }, contentWindowInsets = WindowInsets(0)) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = tabListPadding(padding, horizontal = 12.dp),
        ) {
            item {
                Text("Alege liga", fontWeight = FontWeight.SemiBold, color = colors.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            }
            item {
                Column(Modifier.clip(RoundedCornerShape(16.dp)).background(colors.surface)) {
                    viewModel.leagues.forEachIndexed { i, league ->
                        if (i > 0) HorizontalDivider(color = colors.outline)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onLeagueClick(league.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            LeagueLogo(league.name, league.logoUrl, size = 36.dp)
                            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                Text(league.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(league.country, fontSize = 13.sp, color = colors.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Explorează, steps 2–3: day chips and the fixtures of that day, grouped Live / Programate / Terminate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreLeagueScreen(
    onBack: () -> Unit,
    onFixtureClick: (fixtureId: Int) -> Unit,
    viewModel: ExploreLeagueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LeagueLogo(state.league.name, state.league.logoUrl, size = 28.dp)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(state.league.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(state.league.country, fontSize = 12.sp, color = colors.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Înapoi") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DateChipsRow(
                days = state.days, selected = state.selectedDay, today = state.today,
                onSelect = viewModel::selectDay,
                isEnabled = viewModel::isDayAvailable,
                onDisabledClick = {
                    scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar("Disponibil cu planul complet API-Football") }
                },
            )
            Text(
                longDayLabel(state.selectedDay),
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Box(Modifier.fillMaxSize()) {
                when (val f = state.fixtures) {
                    FixturesState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is FixturesState.NotAvailable -> Message("Zi indisponibilă", f.message, null) {}
                    is FixturesState.Error -> Message("Ceva n-a mers", f.message, "Reîncearcă", viewModel::retry)
                    is FixturesState.Loaded ->
                        if (f.groups.isEmpty) Message("Niciun meci", "${state.league.name} nu are meciuri în această zi.", null) {}
                        else FixtureList(f.groups, f.source, onFixtureClick)
                }
            }
        }
    }
}

@Composable
private fun FixtureList(groups: FixtureGroups, source: DataSource, onFixtureClick: (Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
        listOf("Live" to groups.live, "Programate" to groups.scheduled, "Terminate" to groups.finished)
            .filter { it.second.isNotEmpty() }
            .forEach { (title, fixtures) ->
                item(key = "header_$title") {
                    Text(
                        "$title (${fixtures.size})", fontWeight = FontWeight.SemiBold,
                        color = if (title == "Live") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 6.dp),
                    )
                }
                itemsIndexed(fixtures, key = { _, f -> f.id }) { i, fixture ->
                    FixtureRow(fixture, isFirst = i == 0, isLast = i == fixtures.lastIndex, onClick = { onFixtureClick(fixture.id) })
                }
            }
        item {
            Text(
                if (BuildConfig.USE_FAKE_DATA) "Date de test (USE_FAKE_DATA), nu de la API." else when (source) {
                    DataSource.CACHE -> "Din cache (nicio cerere la API)."
                    DataSource.NETWORK -> "Actualizat acum de la API-Football."
                    DataSource.STALE_CACHE -> "Date salvate anterior; ziua e în afara planului gratuit."
                },
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** One fixture: time or status on the left, the two teams, the score on the right. */
@Composable
private fun FixtureRow(fixture: Fixture, isFirst: Boolean, isLast: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val live = fixture.status == FixtureStatus.LIVE
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 16.dp else 0.dp, topEnd = if (isFirst) 16.dp else 0.dp,
        bottomStart = if (isLast) 16.dp else 0.dp, bottomEnd = if (isLast) 16.dp else 0.dp,
    )
    Column(Modifier.clip(shape).background(colors.surface)) {
        if (!isFirst) HorizontalDivider(color = colors.outline)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(fixture.kickoff.atZone(AppTime.ZONE).format(TIME), fontSize = 13.sp, color = colors.onSurfaceVariant)
                val status = statusText(fixture)
                if (status.isNotEmpty()) {
                    Text(status, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (live) colors.error else colors.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f).padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TeamLine(fixture.homeTeam.name, fixture.homeTeam.logoUrl, fixture.score.home, live)
                TeamLine(fixture.awayTeam.name, fixture.awayTeam.logoUrl, fixture.score.away, live)
            }
        }
    }
}

@Composable
private fun TeamLine(name: String, logoUrl: String?, goals: Int?, live: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeamLogo(name, logoUrl, size = 20.dp)
        Text(name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 8.dp))
        Text(
            goals?.toString() ?: "", fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            color = if (live) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun statusText(f: Fixture): String = when (f.status) {
    FixtureStatus.LIVE -> f.elapsedMinutes?.let { "$it'" } ?: "Live"
    FixtureStatus.FINISHED -> "Final"
    FixtureStatus.POSTPONED -> "Amânat"
    FixtureStatus.CANCELLED -> "Anulat"
    FixtureStatus.SCHEDULED -> ""
}

@Composable
private fun Message(title: String, text: String, button: String?, onClick: () -> Unit) {
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
