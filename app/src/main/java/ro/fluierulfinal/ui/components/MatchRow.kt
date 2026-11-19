package ro.fluierulfinal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ro.fluierulfinal.domain.model.AppTime
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.ui.theme.LocalReduceMotion
import ro.fluierulfinal.ui.theme.Spacing
import ro.fluierulfinal.ui.theme.extendedColors
import ro.fluierulfinal.ui.theme.scoreType
import java.time.format.DateTimeFormatter

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/**
 * One fixture in a list: kick-off time or live minute on the left, the two teams with 24dp logos,
 * the score right-aligned, and optionally a star (favorite). The score is the hero: Barlow Condensed,
 * tabular figures, animated when it changes. A live fixture shows its minute in the live color
 * next to a pulsing dot.
 *
 * The row has no background of its own: it sits on a card ([MatchCard]).
 */
@Composable
fun MatchRow(
    fixture: Fixture,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    starred: Boolean? = null,               // null = no star shown
    onStarClick: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val live = fixture.status == FixtureStatus.LIVE
    val liveColor = MaterialTheme.extendedColors.live
    val scoreColor = if (live) liveColor else colors.onSurface
    val finished = fixture.status == FixtureStatus.FINISHED
    // after the final whistle, the losing side is dimmed so the result reads at a glance
    val homeDim = finished && (fixture.score.home ?: 0) < (fixture.score.away ?: 0)
    val awayDim = finished && (fixture.score.away ?: 0) < (fixture.score.home ?: 0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(start = Spacing.lg, end = if (starred == null) Spacing.lg else Spacing.xs, top = Spacing.md, bottom = Spacing.md),
    ) {
        // left column: time / minute / status
        Column(Modifier.width(52.dp), horizontalAlignment = Alignment.Start) {
            if (live) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot()
                    Spacer(Modifier.width(Spacing.xs))
                    Text(fixture.elapsedMinutes?.let { "$it'" } ?: "Live", style = MaterialTheme.scoreType.minute, color = liveColor)
                }
            } else {
                Text(fixture.kickoff.atZone(AppTime.ZONE).format(TIME), style = MaterialTheme.scoreType.minute, color = colors.onSurfaceVariant)
                statusLabel(fixture.status)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }

        Column(Modifier.weight(1f).padding(start = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TeamLine(fixture.homeTeam.name, fixture.homeTeam.logoUrl, fixture.score.home, scoreColor, dim = homeDim)
            TeamLine(fixture.awayTeam.name, fixture.awayTeam.logoUrl, fixture.score.away, scoreColor, dim = awayDim)
        }

        if (starred != null) {
            IconButton(onClick = onStarClick) {
                Icon(
                    if (starred) AppIcons.starFilled else AppIcons.star,
                    contentDescription = if (starred) "Scoate din favorite" else "Adaugă la favorite",
                    tint = if (starred) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TeamLine(name: String, logoUrl: String?, goals: Int?, scoreColor: Color, dim: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (dim) 0.6f else 1f)) {
        TeamLogo(name, logoUrl, size = 24.dp)
        Text(
            name, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface,
            fontWeight = if (dim) FontWeight.Normal else FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = Spacing.md),
        )
        AnimatedScore(goals, scoreColor)
    }
}

/**
 * The goals of one team. When the number changes (a goal!), the new digit slides in from below
 * and the old one slides out on top. With "reduce motion" on, it just changes.
 */
@Composable
fun AnimatedScore(goals: Int?, color: Color) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedContent(
        targetState = goals,
        transitionSpec = {
            val duration = if (reduceMotion) 0 else 250
            slideInVertically(tween(duration)) { it } togetherWith slideOutVertically(tween(duration)) { -it }
        },
        label = "score",
    ) { value ->
        Text(
            value?.toString() ?: "",
            style = MaterialTheme.scoreType.scoreList, color = color,
            modifier = Modifier.width(22.dp), textAlign = TextAlign.End,
        )
    }
}

/** Small dot that fades in and out while a match is live (static when "reduce motion" is on). */
@Composable
fun PulsingDot(modifier: Modifier = Modifier) {
    val color = MaterialTheme.extendedColors.live
    val alpha = if (LocalReduceMotion.current) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "live")
        val a by transition.animateFloat(
            initialValue = 1f, targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "liveAlpha",
        )
        a
    }
    Box(modifier.size(6.dp).alpha(alpha).background(color, CircleShape))
}

private fun statusLabel(status: FixtureStatus): String? = when (status) {
    FixtureStatus.FINISHED -> "Final"
    FixtureStatus.POSTPONED -> "Amânat"
    FixtureStatus.CANCELLED -> "Anulat"
    else -> null
}

// ---------------------------------------------------------------------------------------------

@ThemePreviews
@Composable
private fun MatchRowPreview() {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    PreviewSurface(dark) {
        MatchCard {
            MatchRow(PreviewData.live, onClick = {}, starred = true)
            MatchCardDivider()
            MatchRow(PreviewData.scheduled, onClick = {}, starred = false)
            MatchCardDivider()
            MatchRow(PreviewData.finished, onClick = {})
            MatchCardDivider()
            MatchRow(PreviewData.postponed, onClick = {})
        }
    }
}
