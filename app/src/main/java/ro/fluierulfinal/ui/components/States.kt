package ro.fluierulfinal.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ro.fluierulfinal.ui.theme.LocalReduceMotion
import ro.fluierulfinal.ui.theme.Spacing
import ro.fluierulfinal.ui.theme.extendedColors

/** Friendly empty screen: icon in a soft circle, title, explanation, optional main action. */
@Composable
fun EmptyState(icon: Painter, title: String, text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxSize().padding(horizontal = Spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Spacing.lg))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Spacing.sm))
        if (action != null) {
            PrimaryActionButton(action, onAction, Modifier.padding(top = Spacing.xl))
        }
    }
}

/**
 * The main action of a screen (sign in, add a team): gold in both themes (`tertiary`),
 * with dark text, so the brand color is the same in light and dark.
 */
@Composable
fun PrimaryActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: (@Composable () -> Unit)? = null) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary),
    ) { if (content != null) content() else Text(text) }
}

/** Error screen with a retry button. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier, title: String = "Ceva n-a mers") {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxSize().padding(horizontal = Spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.extendedColors.liveContainer), contentAlignment = Alignment.Center) {
            Text("!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.extendedColors.live)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Spacing.lg))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Spacing.sm))
        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = Spacing.xl)) { Text("Reîncearcă") }
    }
}

/**
 * A light band sweeps across the placeholder shapes while content loads.
 * With "reduce motion" on, the shapes stay still.
 */
fun Modifier.shimmer(): Modifier = composed {
    val ext = MaterialTheme.extendedColors
    if (LocalReduceMotion.current) return@composed background(ext.skeleton)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(
        Brush.linearGradient(
            colors = listOf(ext.skeleton, ext.skeletonHighlight, ext.skeleton),
            start = Offset(x, 0f), end = Offset(x + 400f, 0f),
        )
    )
}

/** Loading placeholder with the same layout as [MatchRow], so nothing jumps when data arrives. */
@Composable
fun SkeletonMatchRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(36.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmer())
        Spacer(Modifier.width(Spacing.xl))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(2) { i ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(CircleShape).shimmer())
                    Spacer(Modifier.width(Spacing.md))
                    Box(Modifier.fillMaxWidth(if (i == 0) 0.55f else 0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                }
            }
        }
    }
}

/** A card of skeleton rows, shown instead of a spinner while a list loads. */
@Composable
fun SkeletonMatchList(rows: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = Spacing.md)) {
        Spacer(Modifier.height(Spacing.xl))
        Box(Modifier.width(120.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmer())
        Spacer(Modifier.height(Spacing.md))
        MatchCard {
            repeat(rows) { i ->
                if (i > 0) MatchCardDivider()
                SkeletonMatchRow()
            }
        }
    }
}

@ThemePreviews
@Composable
private fun StatesPreview() {
    PreviewSurface(androidx.compose.foundation.isSystemInDarkTheme()) {
        SkeletonMatchList(rows = 2)
        Box(Modifier.height(320.dp)) {
            EmptyState(AppIcons.star, "Nicio echipă favorită", "Adaugă echipele pe care le urmărești.", action = "Adaugă echipă")
        }
    }
}
