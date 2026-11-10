package ro.fluierulfinal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.fluierulfinal.data.League
import ro.fluierulfinal.data.Match
import ro.fluierulfinal.data.MatchStatus
import ro.fluierulfinal.data.Team
import java.time.LocalDate

// Bucăți mici de UI folosite pe mai multe ecrane.

/** Cerc colorat cu inițialele echipei (în locul logo-ului, până îl luăm din API cu Coil). */
@Composable
fun TeamCrest(team: Team, size: Dp = 22.dp) {
    val initials = team.name.filter { it.isUpperCase() }.take(2).ifEmpty { team.name.take(1) }
    Box(
        modifier = Modifier.size(size).background(Color(team.color), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
    }
}

/** Pătrățel colorat cu prescurtarea ligii. */
@Composable
fun LeagueBadge(league: League, size: Dp = 28.dp) {
    Box(
        modifier = Modifier.size(size).background(Color(league.color), RoundedCornerShape(size / 4)),
        contentAlignment = Alignment.Center,
    ) {
        Text(league.abbr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
    }
}

/** Iconiță de lupă desenată cu Canvas (fără bibliotecă de iconițe). */
@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val r = size.minDimension * 0.32f
        val c = Offset(size.width * 0.43f, size.height * 0.43f)
        drawCircle(color, r, c, style = Stroke(width = 2.dp.toPx()))
        drawLine(color, Offset(c.x + r * 0.7f, c.y + r * 0.7f), Offset(size.width * 0.9f, size.height * 0.9f), 2.dp.toPx())
    }
}

/** Iconiță de televizor. */
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

/** Săgeată mică (sus sau jos) pentru a strânge/desface o ligă. */
@Composable
fun ChevronIcon(up: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(14.dp)) {
        val stroke = 2.dp.toPx()
        val (yEdge, yTip) = if (up) size.height * 0.68f to size.height * 0.32f else size.height * 0.32f to size.height * 0.68f
        drawLine(color, Offset(size.width * 0.15f, yEdge), Offset(size.width * 0.5f, yTip), stroke)
        drawLine(color, Offset(size.width * 0.85f, yEdge), Offset(size.width * 0.5f, yTip), stroke)
    }
}

/** Logo-ul: un fluier stilizat, în culoarea textului din bara de sus (alb, ca logo-ul Sofascore). */
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

/** „Azi”, „Ieri”, „Mâine” sau „Mie, 28 oct”. */
fun shortDayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Azi"
    today.minusDays(1) -> "Ieri"
    today.plusDays(1) -> "Mâine"
    else -> "${DAYS[day.dayOfWeek.value - 1].take(3)}, ${day.dayOfMonth} ${MONTHS[day.monthValue - 1]}"
}

/** „Luni, 26 oct”. */
fun longDayLabel(day: LocalDate): String =
    "${DAYS[day.dayOfWeek.value - 1]}, ${day.dayOfMonth} ${MONTHS[day.monthValue - 1]}"

/** Ce scrie sub oră: minutul la live, „Final” la terminat, nimic la cele care urmează. */
fun statusLabel(match: Match): String = when (match.status) {
    MatchStatus.LIVE -> if (match.minute == 45) "Pauză" else "${match.minute}'"
    MatchStatus.FINISHED -> "Final"
    MatchStatus.UPCOMING -> ""
}
