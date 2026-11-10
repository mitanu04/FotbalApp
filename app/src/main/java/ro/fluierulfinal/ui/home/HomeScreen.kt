package ro.fluierulfinal.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.fluierulfinal.data.EventType
import ro.fluierulfinal.data.Match
import ro.fluierulfinal.data.MatchStatus
import ro.fluierulfinal.ui.components.ChevronIcon
import ro.fluierulfinal.ui.components.LeagueBadge
import ro.fluierulfinal.ui.components.SearchIcon
import ro.fluierulfinal.ui.components.TeamCrest
import ro.fluierulfinal.ui.components.TvIcon
import ro.fluierulfinal.ui.components.WhistleLogo
import ro.fluierulfinal.ui.components.shortDayLabel
import ro.fluierulfinal.ui.components.statusLabel

// Colțurile cardului cu meciuri. Cardul e format din mai multe item-uri ale listei,
// așa că primul are colțurile de sus rotunjite, ultimul pe cele de jos, iar cele din mijloc niciunul.
private val CardTop = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val CardBottom = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
private val CardMiddle = RoundedCornerShape(0.dp)

@Composable
private fun Modifier.cardPart(shape: Shape) =
    this.padding(horizontal = 12.dp).clip(shape).background(MaterialTheme.colorScheme.surface)

/** Ecranul principal, în stilul Sofascore: antet cu bandă live, ligile alese și cardul cu meciuri. */
@Composable
fun HomeScreen(vm: HomeViewModel) {
    val state by vm.state.collectAsState()

    // Stări care țin doar de ecran (ce e deschis acum), nu de date.
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    // ce carduri sunt deschise; rememberSaveable le păstrează și la rotirea ecranului
    var favoritesOpen by rememberSaveable { mutableStateOf(true) }
    var matchesOpen by rememberSaveable { mutableStateOf(true) }
    var formOpen by rememberSaveable { mutableStateOf(true) }
    var planOpen by rememberSaveable { mutableStateOf(true) }

    val groups = vm.visibleMatches(state)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Header(
            live = vm.liveMatches(), vm = vm,
            onSearch = { searchOpen = !searchOpen; if (!searchOpen) query = "" },
            onFavorites = { sheet = HomeSheet.FAVORITES },
            onTv = { sheet = HomeSheet.TV },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            item { LeagueChips(state, vm, onPickLeagues = { sheet = HomeSheet.LEAGUES }) }
            if (searchOpen) {
                item { SearchBox(query, onQueryChange = { query = it }, state = state, vm = vm) }
            }
            item {
                FavoriteTeamsStrip(state, vm, open = favoritesOpen,
                    onToggle = { favoritesOpen = !favoritesOpen }, onAdd = { searchOpen = true })
            }

            item { FormCard(state, vm, open = formOpen, onToggle = { formOpen = !formOpen }) }
            item { WeekendPlanCard(state, vm, open = planOpen, onToggle = { planOpen = !planOpen }) }

            // --- cardul cu meciuri ---
            item {
                ListControls(state, vm, open = matchesOpen, onToggle = { matchesOpen = !matchesOpen },
                    modifier = Modifier.cardPart(if (matchesOpen) CardTop else RoundedCornerShape(16.dp)))
            }
            if (matchesOpen) {

            if (groups.isEmpty()) {
                item {
                    Text(
                        text = "Niciun meci în ligile alese în această zi.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.cardPart(CardMiddle).fillMaxWidth().padding(32.dp),
                    )
                }
            }
            groups.forEach { (leagueId, matches) ->
                val collapsed = leagueId in state.collapsedLeagueIds
                item(key = "league_$leagueId") {
                    LeagueHeader(leagueId, matches, collapsed, vm, Modifier.cardPart(CardMiddle))
                }
                if (!collapsed) {
                    items(matches, key = { it.id }) { match ->
                        MatchRow(match, state, vm, Modifier.cardPart(CardMiddle))
                    }
                }
            }
            item { Spacer(Modifier.cardPart(CardBottom).fillMaxWidth().height(12.dp)) }
            }
            // --- sfârșitul cardului ---

            item {
                Text(
                    "Date de exemplu. Meciurile reale vor veni din API-Football.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    when (sheet) {
        HomeSheet.LEAGUES -> LeaguePickerSheet(state, vm, onDismiss = { sheet = null })
        HomeSheet.FAVORITES -> FavoritesSheet(state, vm, onDismiss = { sheet = null })
        HomeSheet.TV -> TvScheduleSheet(state, vm, onDismiss = { sheet = null })
        null -> Unit
    }
}

enum class HomeSheet { LEAGUES, FAVORITES, TV }

/** Antetul: logo + butoane, iar dedesubt banda cu meciurile live. Albastru pe light, închis pe dark. */
@Composable
private fun Header(
    live: List<Match>,
    vm: HomeViewModel,
    onSearch: () -> Unit,
    onFavorites: () -> Unit,
    onTv: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val onHeader = colors.onPrimaryContainer
    var profileOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().background(colors.primaryContainer).statusBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        ) {
            WhistleLogo()
            Text(
                "Fluierul Final",
                color = onHeader, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            IconButton(onClick = onSearch) { SearchIcon(onHeader) }
            IconButton(onClick = onFavorites) { Text("★", fontSize = 22.sp, color = onHeader) }
            IconButton(onClick = onTv) { TvIcon(onHeader) }
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(onHeader.copy(alpha = 0.15f))
                        .clickable { profileOpen = true },
                ) {
                    Text("M", color = onHeader, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = profileOpen, onDismissRequest = { profileOpen = false }) {
                    DropdownMenuItem(text = { Text("Mihai", fontWeight = FontWeight.SemiBold) }, onClick = {}, enabled = false)
                    DropdownMenuItem(text = { Text("Setări") }, onClick = { profileOpen = false })
                    DropdownMenuItem(text = { Text("Deconectare") }, onClick = { profileOpen = false })
                }
            }
        }
        LiveTicker(live, vm)
    }
}

/** Banda cu toate meciurile live: pastile cu fundal roșiatic, minut, scor, echipe. */
@Composable
private fun LiveTicker(live: List<Match>, vm: HomeViewModel) {
    val colors = MaterialTheme.colorScheme
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
    ) {
        items(live, key = { it.id }) { m ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.errorContainer)
                    .border(1.dp, colors.error.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .clickable { vm.openMatch(m.id) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("● ${statusLabel(m)}", color = colors.onErrorContainer, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                TeamCrest(vm.repository.team(m.homeId), 18.dp)
                Text("${m.homeGoals} - ${m.awayGoals}", fontWeight = FontWeight.Bold, color = colors.onSurface)
                TeamCrest(vm.repository.team(m.awayId), 18.dp)
            }
        }
    }
}

/** Bara cu ligile alese de utilizator (în locul barei cu sporturi). */
@Composable
private fun LeagueChips(state: HomeUiState, vm: HomeViewModel, onPickLeagues: () -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        item { Chip("Toate ligile", selected = state.selectedLeagueId == null) { vm.selectLeague(null) } }
        items(state.myLeagueIds) { id ->
            val league = vm.repository.league(id)
            Chip(league.name, selected = state.selectedLeagueId == id, leading = { LeagueBadge(league, 18.dp) }) {
                vm.selectLeague(id)
            }
        }
        item { Chip("+ Alege ligile", selected = false, accent = true, onClick = onPickLeagues) }
    }
}

/** Pastilă ca „Football / Tennis” din Sofascore: albă când e selectată, altfel transparentă. */
@Composable
private fun Chip(
    text: String,
    selected: Boolean,
    accent: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val bg = if (selected) colors.onSurface else colors.surface
    val fg = when {
        selected -> colors.surface
        accent -> colors.primary
        else -> colors.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        leading?.invoke()
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/**
 * Cardul „Favorite”: următoarele meciuri ale echipelor favorite, ca niște mini-carduri
 * (gazde | ora | oaspeți), derulate orizontal ca banda live. Meciurile live apar primele.
 */
@Composable
private fun FavoriteTeamsStrip(state: HomeUiState, vm: HomeViewModel, open: Boolean, onToggle: () -> Unit, onAdd: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val matches = vm.upcomingFavoriteMatches(state)
        .sortedBy { if (it.status == MatchStatus.LIVE) 0 else 1 } // sortarea e stabilă: restul rămân după dată/oră
    Column(
        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(vertical = 12.dp)
    ) {
        SectionHeader("Favorite", open, onToggle, summary = "${matches.size} meciuri",
            modifier = Modifier.padding(start = 16.dp, end = 12.dp))
        if (!open) return@Column
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(matches, key = { it.id }) { match -> FavoriteMatchCard(match, state, vm) }
            // ultimul element: adaugă o echipă (deschide căutarea)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.height(112.dp).width(84.dp).clip(RoundedCornerShape(12.dp))
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                        .clickable(onClick = onAdd),
                ) {
                    Box(Modifier.size(36.dp).border(1.dp, colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 20.sp, color = colors.primary)
                    }
                    Text("Adaugă echipă", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.primary,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        if (matches.isEmpty()) {
            Text(
                "Echipele tale favorite nu au meciuri programate.",
                fontSize = 13.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
        }
    }
}

/** Un mini-card: echipa gazdă, la mijloc ora (sau scorul live), echipa oaspete. */
@Composable
private fun FavoriteMatchCard(match: Match, state: HomeUiState, vm: HomeViewModel) {
    val colors = MaterialTheme.colorScheme
    val live = match.status == MatchStatus.LIVE
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(112.dp).width(220.dp).clip(RoundedCornerShape(12.dp))
            .background(if (live) colors.errorContainer else colors.surfaceVariant)
            .clickable { vm.openMatch(match.id) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        MiniTeam(match.homeId, state, vm, Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
            Text(
                if (live) "${match.homeGoals} - ${match.awayGoals}" else match.kickoff,
                fontSize = 20.sp, fontWeight = FontWeight.Black,
                color = if (live) colors.onErrorContainer else colors.onSurface,
            )
            Text(
                if (live) "● ${statusLabel(match)}" else shortDayLabel(match.date, vm.repository.today),
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = if (live) colors.onErrorContainer else colors.onSurfaceVariant,
            )
        }
        MiniTeam(match.awayId, state, vm, Modifier.weight(1f))
    }
}

@Composable
private fun MiniTeam(teamId: String, state: HomeUiState, vm: HomeViewModel, modifier: Modifier) {
    val team = vm.repository.team(teamId)
    val fav = teamId in state.favoriteTeamIds
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        TeamCrest(team, 36.dp)
        Text(
            team.name, fontSize = 12.sp, textAlign = TextAlign.Center,
            fontWeight = if (fav) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * Titlul unui card care se poate strânge: numele, săgeata, iar când e strâns un rezumat scurt.
 * Îl folosesc toate cardurile de pe ecranul principal, ca să arate și să se poarte la fel.
 */
@Composable
fun SectionHeader(title: String, open: Boolean, onToggle: () -> Unit, summary: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onToggle).padding(vertical = 8.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        ChevronIcon(up = open, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
        Spacer(Modifier.weight(1f))
        if (!open && summary != null) {
            Text(summary, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
    }
}

/** Partea de sus a cardului: titlul, selectorul de zi, filtrele de stare. */
@Composable
private fun ListControls(state: HomeUiState, vm: HomeViewModel, open: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val dayCount = vm.visibleMatches(state).values.sumOf { it.size }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)) {
            SectionHeader("Meciuri", open, onToggle, summary = if (open) null else "$dayCount meciuri", modifier = Modifier.weight(1f))
            if (!open) return@Row
            // selectorul de zi, într-o pastilă cu contur
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outline, RoundedCornerShape(10.dp)),
            ) {
                Text("‹", fontSize = 20.sp, color = colors.primary,
                    modifier = Modifier.clickable(onClick = vm::previousDay).padding(horizontal = 12.dp, vertical = 2.dp))
                Text(shortDayLabel(state.day, vm.repository.today), fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = colors.primary, textAlign = TextAlign.Center, modifier = Modifier.width(76.dp))
                Text("›", fontSize = 20.sp, color = colors.primary,
                    modifier = Modifier.clickable(onClick = vm::nextDay).padding(horizontal = 12.dp, vertical = 2.dp))
            }
        }
        if (!open) return@Column
        HorizontalDivider(color = colors.outline)

        val liveCount = vm.repository.matches.count { it.date == state.day && it.status == MatchStatus.LIVE }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            listOf(
                StatusFilter.LIVE to "Live ($liveCount)",
                StatusFilter.FINISHED to "Terminate",
                StatusFilter.UPCOMING to "Urmează",
            ).forEach { (f, label) ->
                val selected = state.filter == f
                // a doua apăsare pe același filtru îl scoate (ca la Sofascore)
                val onClick = { vm.setFilter(if (selected) StatusFilter.ALL else f) }
                val (bg, fg) = when {
                    f == StatusFilter.LIVE && selected -> colors.error to Color.White
                    f == StatusFilter.LIVE -> colors.errorContainer to colors.onErrorContainer
                    selected -> colors.onSurface to colors.surface
                    else -> colors.surfaceVariant to colors.onSurface
                }
                Text(
                    label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).clickable(onClick = onClick)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/** Antetul unei ligi: insigna, numele, țara, iar în dreapta „live/total” și săgeata de strângere. */
@Composable
private fun LeagueHeader(leagueId: String, matches: List<Match>, collapsed: Boolean, vm: HomeViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val league = vm.repository.league(leagueId)
    val live = matches.count { it.status == MatchStatus.LIVE }
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.outline)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { vm.toggleCollapsed(leagueId) }.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            LeagueBadge(league)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(league.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(league.country, fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
            Text(
                if (live > 0) "$live/${matches.size}" else "${matches.size}",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = if (live > 0) colors.onErrorContainer else colors.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (live > 0) colors.errorContainer else colors.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(start = 10.dp).size(32.dp).border(1.dp, colors.outline, CircleShape),
            ) { ChevronIcon(up = !collapsed, color = colors.onSurfaceVariant) }
        }
    }
}

/** Un rând de meci: ora + minutul, cele două echipe (cu cartonaș roșu), scorul, steaua. */
@Composable
private fun MatchRow(match: Match, state: HomeUiState, vm: HomeViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val live = match.status == MatchStatus.LIVE
    val followed = vm.isFavoriteMatch(match, state)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().clickable { vm.openMatch(match.id) }
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Column(Modifier.width(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(match.kickoff, fontSize = 12.sp, color = colors.onSurfaceVariant)
            Text(
                statusLabel(match).ifEmpty { "-" }, fontSize = 12.sp,
                fontWeight = if (live) FontWeight.SemiBold else FontWeight.Normal,
                color = if (live) colors.error else colors.onSurfaceVariant,
            )
        }
        // linie verticală subțire între oră și echipe
        Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(36.dp).background(colors.outline))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(match.homeId to true, match.awayId to false).forEach { (id, isHome) ->
                val team = vm.repository.team(id)
                val reds = match.events.count { it.type == EventType.RED_CARD && it.isHome == isHome }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamCrest(team, 18.dp)
                    Text(
                        team.name, fontSize = 14.sp,
                        fontWeight = if (id in state.favoriteTeamIds) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false),
                    )
                    repeat(reds) {
                        Box(Modifier.padding(start = 6.dp).size(7.dp, 10.dp).background(colors.error, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
            val scoreColor = if (live) colors.error else colors.onSurface
            listOf(match.homeGoals, match.awayGoals).forEach { goals ->
                Text(goals?.toString() ?: "", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = scoreColor)
            }
        }
        Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(36.dp).background(colors.outline))
        // steaua: urmărește meciul (îl pune la „Favorite”)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, colors.outline, CircleShape)
                .clickable { vm.toggleFollow(match.id) },
        ) {
            Text(if (followed) "★" else "☆", fontSize = 17.sp, color = if (followed) colors.primary else colors.onSurfaceVariant)
        }
    }
}

/** Căutare: echipe (le poți adăuga la favorite direct de aici) și ligi. */
@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit, state: HomeUiState, vm: HomeViewModel) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Caută echipă sau ligă") },
            leadingIcon = { SearchIcon(colors.onSurfaceVariant) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth(),
        )
        val q = query.trim().lowercase()
        if (q.isNotEmpty()) {
            val teams = vm.repository.teams.filter { q in it.name.lowercase() }.take(6)
            val leagues = vm.repository.leagues.filter { q in it.name.lowercase() }.take(3)
            if (teams.isEmpty() && leagues.isEmpty()) {
                Text("Nimic găsit pentru „$query”", color = colors.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            }
            teams.forEach { team ->
                val fav = team.id in state.favoriteTeamIds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { vm.toggleFavorite(team.id) }.padding(vertical = 10.dp, horizontal = 4.dp),
                ) {
                    TeamCrest(team)
                    Text(team.name, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text(if (fav) "★ Favorită" else "☆ Adaugă", color = if (fav) colors.primary else colors.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            leagues.forEach { league ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (league.id !in state.myLeagueIds) vm.setMyLeagues(state.myLeagueIds + league.id)
                        vm.selectLeague(league.id)
                        onQueryChange("")
                    }.padding(vertical = 10.dp, horizontal = 4.dp),
                ) {
                    LeagueBadge(league, 22.dp)
                    Text(league.name, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text(league.country, color = colors.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
    }
}
