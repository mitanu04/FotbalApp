package ro.fluierulfinal.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.fluierulfinal.data.FormResult
import ro.fluierulfinal.data.PlanSlot
import ro.fluierulfinal.ui.components.TeamCrest
import ro.fluierulfinal.ui.components.TvIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Cardurile de pe ecranul principal: forma favoritelor și planul de weekend.

/** Fundalul comun al cardurilor de pe ecranul principal. */
@Composable
private fun HomeCard(content: @Composable () -> Unit) {
    Column(
        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
    ) { content() }
}

// Culorile pentru Victorie / Egal / Înfrângere, ca la Sofascore (verde / portocaliu / roșu)
private val WinGreen = Color(0xFF3DA35D)
private val DrawOrange = Color(0xFFF0A030)
private val LossRed = Color(0xFFD9342B)
private val UpcomingGrey = Color(0xFF6B6B76)
private val TooltipGrey = Color(0xFF55555F)

/**
 * Ce pătrățel e apăsat acum: echipa și poziția.
 * Poziția -1 e pătrățelul „?” (meciul următor), 0..4 sunt rezultatele.
 */
private data class FormSelection(val teamId: String, val index: Int)

/** Forma favoritelor: „?” pentru meciul următor, apoi ultimele 5 rezultate V / E / Î. */
@Composable
fun FormCard(state: HomeUiState, vm: HomeViewModel, open: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    HomeCard {
        // null = nicio bulă deschisă
        var selected by remember { mutableStateOf<FormSelection?>(null) }
        SectionHeader("Forma favoritelor", open, onToggle, summary = "ultimele 5 meciuri")
        if (!open) return@HomeCard
        Text(
            "Cel mai recent meci e în stânga, „?” e meciul următor. Apasă o dată pentru scor, a doua oară pentru meci.",
            fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp),
        )
        state.favoriteTeamIds.forEach { teamId ->
            val team = vm.repository.team(teamId)
            val form = vm.repository.formFor(teamId)
            val next = vm.repository.nextMatchFor(teamId)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
            ) {
                TeamCrest(team, 24.dp)
                Text(
                    team.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    // primul pătrățel: meciul următor
                    if (next != null) {
                        val sel = FormSelection(teamId, -1)
                        FormSquare("?", UpcomingGrey, selected == sel,
                            onClick = {
                                if (selected == sel) { selected = null; vm.openMatch(next.id) } else selected = sel
                            },
                            bubble = {
                                val home = vm.repository.team(next.homeId).name
                                val away = vm.repository.team(next.awayId).name
                                FormBubble(bold = next.kickoff, rest = "($home - $away)", date = next.date,
                                    onClick = { selected = null; vm.openMatch(next.id) })
                            },
                        )
                    }
                    form.forEachIndexed { index, entry ->
                        val sel = FormSelection(teamId, index)
                        val (bg, letter) = when (entry.result) {
                            FormResult.WIN -> WinGreen to "V"
                            FormResult.DRAW -> DrawOrange to "E"
                            FormResult.LOSS -> LossRed to "Î"
                        }
                        // prima apăsare deschide bula, a doua (pe același pătrățel) deschide meciul
                        val openIt = { selected = null; entry.matchId?.let { vm.openMatch(it) }; Unit }
                        FormSquare(letter, bg, selected == sel,
                            onClick = { if (selected == sel) openIt() else selected = sel },
                            bubble = {
                                // scorul se scrie mereu gazde-oaspeți, ca la Sofascore: „1-3 (Strasbourg - PSG)”
                                val opponent = vm.repository.team(entry.opponentId).name
                                val (home, away) = if (entry.atHome) team.name to opponent else opponent to team.name
                                val (hg, ag) = if (entry.atHome) entry.goalsFor to entry.goalsAgainst else entry.goalsAgainst to entry.goalsFor
                                FormBubble(bold = "$hg-$ag", rest = "($home - $away)", date = entry.date, onClick = openIt)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Un pătrățel din formă; când e ales, are contur și arată bula dedesubt. */
@Composable
private fun FormSquare(
    letter: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    bubble: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(26.dp)
            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(5.dp)) else Modifier)
            .clip(RoundedCornerShape(4.dp)).background(color)
            .clickable(onClick = onClick),
    ) {
        Text(letter, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (isSelected) {
            // dismissOnClickOutside = false: altfel a doua apăsare pe pătrățel ar închide
            // întâi bula (fiind „în afara” ei) și n-am mai ajunge la meci
            // două popup-uri: vârful stă mereu sub mijlocul pătrățelului, iar corpul bulei
            // poate fi împins spre stânga dacă ar ieși din ecran (pătrățelele sunt la marginea dreaptă)
            val arrowPx = with(LocalDensity.current) { 8.dp.roundToPx() }
            val props = PopupProperties(dismissOnClickOutside = false)
            Popup(popupPositionProvider = BelowAnchor(gapPx = 4), properties = props) { BubbleArrow() }
            Popup(popupPositionProvider = BelowAnchor(gapPx = 4 + arrowPx), properties = props) { bubble() }
        }
    }
}

/**
 * Așază bula sub pătrățel. Vârful bulei stă exact sub mijlocul pătrățelului;
 * corpul bulei e centrat pe el, dar împins înapoi în ecran dacă ar ieși pe margine.
 */
private class BelowAnchor(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        return IntOffset(x, anchorBounds.bottom + gapPx)
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** Vârful bulei, un triunghi care arată în sus spre pătrățel. */
@Composable
private fun BubbleArrow() {
    Canvas(Modifier.size(width = 16.dp, height = 8.dp)) {
        val path = Path().apply {
            moveTo(0f, size.height); lineTo(size.width / 2, 0f); lineTo(size.width, size.height); close()
        }
        drawPath(path, TooltipGrey)
    }
}

/** Bula gri: pe primul rând scorul îngroșat (sau ora) și echipele, pe al doilea data. */
@Composable
private fun FormBubble(bold: String, rest: String, date: LocalDate, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(TooltipGrey)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                    append(" $rest")
                },
                color = Color.White, fontSize = 15.sp,
            )
            Text(date.format(DATE_FORMAT), color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        }
    }
}

/** Planul de weekend: un meci ales pe fiecare slot, la stadion sau la TV. */
@Composable
fun WeekendPlanCard(state: HomeUiState, vm: HomeViewModel, open: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (range, slots) = vm.weekendPlan(state)
    HomeCard {
        SectionHeader("Plan de weekend", open, onToggle, summary = range)
        if (!open) return@HomeCard
        Text(
            "$range · ales după echipele favorite și distanța față de tine (Timișoara).",
            fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp),
        )
        slots.forEachIndexed { i, slot ->
            if (i > 0) HorizontalDivider(color = colors.outline)
            PlanSlotRow(slot, state, vm)
        }
    }
}

@Composable
private fun PlanSlotRow(slot: PlanSlot, state: HomeUiState, vm: HomeViewModel) {
    val colors = MaterialTheme.colorScheme
    val match = slot.match
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = match != null) { match?.let { vm.openMatch(it.id) } }
            .padding(vertical = 10.dp),
    ) {
        // coloana din stânga: slotul și ora
        Column(Modifier.width(92.dp)) {
            Text(slot.label, fontSize = 12.sp, color = colors.onSurfaceVariant)
            Text(match?.kickoff ?: "—", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (match == null) {
            Text("Liber", color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            return@Row
        }
        Column(Modifier.weight(1f)) {
            listOf(match.homeId, match.awayId).forEach { id ->
                val team = vm.repository.team(id)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                    TeamCrest(team, 16.dp)
                    Text(
                        team.name, fontSize = 13.sp,
                        fontWeight = if (id in state.favoriteTeamIds) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        // pastila din dreapta: unde îl vezi
        if (slot.atStadium) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "La stadion", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(WinGreen).padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Text("${slot.distanceKm} km", fontSize = 11.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.surfaceVariant).padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    TvIcon(colors.onSurface, Modifier.size(16.dp))
                    Text("La TV", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                }
                Text(match.tvChannel, fontSize = 11.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}
