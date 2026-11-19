package ro.fluierulfinal.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ro.fluierulfinal.ui.components.AppIcons
import ro.fluierulfinal.ui.components.DateChipsRow
import ro.fluierulfinal.ui.components.FormPills
import ro.fluierulfinal.ui.components.FormResult
import ro.fluierulfinal.ui.components.LeagueHeader
import ro.fluierulfinal.ui.components.LiveBadge
import ro.fluierulfinal.ui.components.MatchCard
import ro.fluierulfinal.ui.components.MatchCardDivider
import ro.fluierulfinal.ui.components.MatchRow
import ro.fluierulfinal.ui.components.PreviewData
import ro.fluierulfinal.ui.components.RatingBadge
import ro.fluierulfinal.ui.components.ScoreBoard
import ro.fluierulfinal.ui.components.SkeletonMatchRow
import ro.fluierulfinal.ui.components.StatComparisonBar
import ro.fluierulfinal.ui.theme.FluierulFinalTheme
import ro.fluierulfinal.ui.theme.LocalAppPalette
import ro.fluierulfinal.ui.theme.Spacing
import ro.fluierulfinal.ui.theme.ThemeMode
import java.time.LocalDate

/**
 * Debug screen (Setări → Dezvoltare): every design-system component, first in the light theme,
 * then in the dark one, with sample data. Same content as the @Preview functions, but on a device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentGalleryScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galerie componente") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons.back, "Înapoi") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            val palette = LocalAppPalette.current
            ThemeMode.entries.filter { it != ThemeMode.SYSTEM }.forEach { mode ->
                FluierulFinalTheme(mode, palette) { GallerySection(if (mode == ThemeMode.LIGHT) "LIGHT" else "DARK") }
            }
        }
    }
}

@Composable
private fun GallerySection(title: String) {
    val today = LocalDate.of(2026, 11, 19)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(Spacing.md)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)

        LeagueHeader("Liga I", "România", "https://media.api-sports.io/football/leagues/283.png", trailing = "4 meciuri")
        MatchCard {
            MatchRow(PreviewData.live, onClick = {}, starred = true)
            MatchCardDivider()
            MatchRow(PreviewData.scheduled, onClick = {}, starred = false)
            MatchCardDivider()
            MatchRow(PreviewData.finished, onClick = {})
            MatchCardDivider()
            MatchRow(PreviewData.postponed, onClick = {})
            MatchCardDivider()
            SkeletonMatchRow()
        }

        LeagueHeader("Detalii meci", "ScoreBoard", null)
        MatchCard { ScoreBoard(PreviewData.live) }

        Spacer(Modifier.height(Spacing.lg))
        DateChipsRow((-1..5).map { today.plusDays(it.toLong()) }, selected = today, today = today, onSelect = {})

        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            LiveBadge(67)
            FormPills(listOf(FormResult.WIN, FormResult.WIN, FormResult.DRAW, FormResult.LOSS, FormResult.WIN))
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) { listOf(5.8, 6.4, 7.3, 8.6).forEach { RatingBadge(it) } }

        Spacer(Modifier.height(Spacing.md))
        MatchCard {
            Box(Modifier.padding(Spacing.lg)) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    StatComparisonBar("Posesie (%)", 58f, 42f)
                    StatComparisonBar("Șuturi pe poartă", 3f, 7f)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}
