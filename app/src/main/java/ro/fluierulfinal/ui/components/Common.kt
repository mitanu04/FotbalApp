package ro.fluierulfinal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import java.time.LocalDate

// Small UI pieces shared by several screens.

/**
 * Team logo loaded by Coil from [logoUrl]. While it loads, if there is no internet,
 * or if the team has no logo, a colored circle with the team's initials is shown instead.
 */
@Composable
fun TeamLogo(name: String, logoUrl: String?, size: Dp = 22.dp) {
    if (logoUrl == null) {
        InitialsCircle(name, size)
        return
    }
    SubcomposeAsyncImage(
        model = logoUrl,
        contentDescription = "Sigla $name",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size),
        loading = { InitialsCircle(name, size) },
        error = { InitialsCircle(name, size) },
    )
}

/** League logo (same fallback as [TeamLogo]). */
@Composable
fun LeagueLogo(name: String, logoUrl: String?, size: Dp = 28.dp) = TeamLogo(name, logoUrl, size)

/** Circle with up to two initials, e.g. "FC" for "FC Botosani". Its color is derived from the name. */
@Composable
private fun InitialsCircle(name: String, size: Dp) {
    val initials = name.filter { it.isUpperCase() }.take(2).ifEmpty { name.take(1) }
    val color = PALETTE[(name.hashCode() and Int.MAX_VALUE) % PALETTE.size]
    Box(
        modifier = Modifier.size(size).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
    }
}

private val PALETTE = listOf(
    Color(0xFFC8102E), Color(0xFF1D4FA3), Color(0xFF2E8B57), Color(0xFF5B2A86),
    Color(0xFFE8482C), Color(0xFF0B6BCB), Color(0xFF8A1538), Color(0xFF14427A),
)

/** Magnifier icon drawn with Canvas (no icon library needed). */
@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val r = size.minDimension * 0.32f
        val c = Offset(size.width * 0.43f, size.height * 0.43f)
        drawCircle(color, r, c, style = Stroke(width = 2.dp.toPx()))
        drawLine(color, Offset(c.x + r * 0.7f, c.y + r * 0.7f), Offset(size.width * 0.9f, size.height * 0.9f), 2.dp.toPx())
    }
}

/** TV icon. */
@Composable
fun TvIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        drawRoundRect(color, Offset(size.width * 0.1f, size.height * 0.28f), Size(size.width * 0.8f, size.height * 0.52f),
            CornerRadius(3.dp.toPx()), style = Stroke(stroke))
        drawLine(color, Offset(size.width * 0.35f, size.height * 0.08f), Offset(size.width * 0.5f, size.height * 0.26f), stroke)
        drawLine(color, Offset(size.width * 0.65f, size.height * 0.08f), Offset(size.width * 0.5f, size.height * 0.26f), stroke)
    }
}

/** Small chevron (up or down) for collapsing/expanding a section. */
@Composable
fun ChevronIcon(up: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(14.dp)) {
        val stroke = 2.dp.toPx()
        val (yEdge, yTip) = if (up) size.height * 0.68f to size.height * 0.32f else size.height * 0.32f to size.height * 0.68f
        drawLine(color, Offset(size.width * 0.15f, yEdge), Offset(size.width * 0.5f, yTip), stroke)
        drawLine(color, Offset(size.width * 0.85f, yEdge), Offset(size.width * 0.5f, yTip), stroke)
    }
}

/** App logo: a stylized whistle in the top bar text color. */
@Composable
fun WhistleLogo(modifier: Modifier = Modifier) {
    val gold = MaterialTheme.colorScheme.onPrimaryContainer
    val hole = MaterialTheme.colorScheme.primaryContainer
    Canvas(modifier.size(28.dp)) {
        val w = size.width
        drawRoundRect(gold, Offset(0f, w * 0.28f), Size(w * 0.68f, w * 0.44f), CornerRadius(w * 0.22f))
        drawRoundRect(gold, Offset(w * 0.55f, w * 0.28f), Size(w * 0.42f, w * 0.18f), CornerRadius(w * 0.05f))
        drawCircle(hole, w * 0.09f, Offset(w * 0.34f, w * 0.5f))
    }
}

private val DAYS = listOf("Luni", "Marți", "Miercuri", "Joi", "Vineri", "Sâmbătă", "Duminică")
private val MONTHS = listOf("ian", "feb", "mar", "apr", "mai", "iun", "iul", "aug", "sep", "oct", "nov", "dec")

/** "Azi", "Ieri", "Mâine" or e.g. "Mie, 28 oct" (UI text, so in Romanian). */
fun shortDayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Azi"
    today.minusDays(1) -> "Ieri"
    today.plusDays(1) -> "Mâine"
    else -> "${DAYS[day.dayOfWeek.value - 1].take(3)}, ${day.dayOfMonth} ${MONTHS[day.monthValue - 1]}"
}

/** e.g. "Luni, 26 oct". */
fun longDayLabel(day: LocalDate): String =
    "${DAYS[day.dayOfWeek.value - 1]}, ${day.dayOfMonth} ${MONTHS[day.monthValue - 1]}"
