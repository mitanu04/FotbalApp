package ro.fluierulfinal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.AppTime
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.ui.theme.BarlowCondensed
import ro.fluierulfinal.ui.theme.LocalReduceMotion
import ro.fluierulfinal.ui.theme.Spacing
import ro.fluierulfinal.ui.theme.extendedColors
import ro.fluierulfinal.ui.theme.scoreType
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "● LIVE 67'": live color on a tinted background (not white on red, which fails contrast). */
@Composable
fun LiveBadge(minute: Int?, modifier: Modifier = Modifier) {
    val ext = MaterialTheme.extendedColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clip(CircleShape).background(ext.liveContainer).padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        PulsingDot()
        Text(
            "LIVE" + (minute?.let { " $it'" } ?: ""), style = MaterialTheme.scoreType.minute, color = ext.live,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Result of one past match, for [FormPills]. */
enum class FormResult(val letter: String) { WIN("V"), DRAW("E"), LOSS("Î") }

/** Last results as colored pills: V (victorie) / E (egal) / Î (înfrângere), most recent first. */
@Composable
fun FormPills(results: List<FormResult>, modifier: Modifier = Modifier) {
    val ext = MaterialTheme.extendedColors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        results.forEach { r ->
            val fill = when (r) {
                FormResult.WIN -> ext.win
                FormResult.DRAW -> ext.draw
                FormResult.LOSS -> ext.loss
            }
            Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(fill), contentAlignment = Alignment.Center) {
                Text(r.letter, fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, color = ext.onPill, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Player rating, e.g. "7.4", colored by band: < 6 red, 6–6.9 orange, 7–7.9 green, ≥ 8 blue. */
@Composable
fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    val ext = MaterialTheme.extendedColors
    val fill = when {
        rating < 6.0 -> ext.ratingPoor
        rating < 7.0 -> ext.ratingAverage
        rating < 8.0 -> ext.ratingGood
        else -> ext.ratingGreat
    }
    Text(
        String.format(Locale.US, "%.1f", rating),
        style = MaterialTheme.scoreType.minute, color = ext.onPill, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(fill).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * One statistic compared between the two teams, e.g. possession 58 – 42: the two values on the sides
 * and two bars that grow towards each other. The team with the higher value gets the accent color.
 */
@Composable
fun StatComparisonBar(label: String, home: Float, away: Float, modifier: Modifier = Modifier, format: (Float) -> String = { it.toInt().toString() }) {
    val colors = MaterialTheme.colorScheme
    val total = (home + away).takeIf { it > 0f } ?: 1f
    val homeColor = if (home >= away) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.4f)
    val awayColor = if (away >= home) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.4f)
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(format(home), style = MaterialTheme.scoreType.minute, color = colors.onSurface)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text(format(away), style = MaterialTheme.scoreType.minute, color = colors.onSurface)
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            // left half fills from the center to the left, right half from the center to the right
            Row(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(colors.surfaceVariant), horizontalArrangement = Arrangement.End) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(home / total).clip(CircleShape).background(homeColor))
            }
            Row(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(colors.surfaceVariant)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(away / total).clip(CircleShape).background(awayColor))
            }
        }
    }
}

/**
 * Match details header: 56dp logos, the big score in the middle (or kick-off time), status under it.
 */
@Composable
fun ScoreBoard(fixture: Fixture, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val ext = MaterialTheme.extendedColors
    val live = fixture.status == FixtureStatus.LIVE
    Row(modifier.fillMaxWidth().padding(vertical = Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        ScoreBoardTeam(fixture.homeTeam.name, fixture.homeTeam.logoUrl, Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(132.dp)) {
            if (fixture.score.home != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val c = if (live) ext.live else colors.onSurface
                    AnimatedBigScore(fixture.score.home, c)
                    Text(" – ", style = MaterialTheme.scoreType.scoreLarge, color = colors.onSurfaceVariant)
                    AnimatedBigScore(fixture.score.away, c)
                }
            } else {
                Text(
                    fixture.kickoff.atZone(AppTime.ZONE).format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.scoreType.scoreLarge, color = colors.onSurface,
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            when (fixture.status) {
                FixtureStatus.LIVE -> LiveBadge(fixture.elapsedMinutes)
                FixtureStatus.FINISHED -> Text("Final", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                FixtureStatus.POSTPONED -> Text("Amânat", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                FixtureStatus.CANCELLED -> Text("Anulat", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                FixtureStatus.SCHEDULED -> Text("Începe la ora", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
        }
        ScoreBoardTeam(fixture.awayTeam.name, fixture.awayTeam.logoUrl, Modifier.weight(1f))
    }
}

/** Same slide-in animation as the list score ([AnimatedScore]), in the big scoreboard style. */
@Composable
private fun AnimatedBigScore(goals: Int?, color: Color) {
    val duration = if (LocalReduceMotion.current) 0 else 250
    AnimatedContent(
        targetState = goals,
        transitionSpec = { slideInVertically(tween(duration)) { it } togetherWith slideOutVertically(tween(duration)) { -it } },
        label = "bigScore",
    ) { Text(it?.toString() ?: "", style = MaterialTheme.scoreType.scoreLarge, color = color) }
}

@Composable
private fun ScoreBoardTeam(name: String, logoUrl: String?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TeamLogo(name, logoUrl, size = 56.dp)
        Text(
            name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}

@ThemePreviews
@Composable
private fun BadgesPreview() {
    PreviewSurface(androidx.compose.foundation.isSystemInDarkTheme()) {
        MatchCard { ScoreBoard(PreviewData.live) }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            LiveBadge(67)
            FormPills(listOf(FormResult.WIN, FormResult.WIN, FormResult.DRAW, FormResult.LOSS, FormResult.WIN))
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(5.8, 6.4, 7.3, 8.6).forEach { RatingBadge(it) }
        }
        Spacer(Modifier.height(Spacing.md))
        MatchCard {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                StatComparisonBar("Posesie (%)", 58f, 42f)
                StatComparisonBar("Șuturi", 9f, 14f)
            }
        }
    }
}
