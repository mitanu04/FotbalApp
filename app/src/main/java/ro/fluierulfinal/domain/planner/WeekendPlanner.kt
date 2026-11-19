package ro.fluierulfinal.domain.planner

import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on Earth (e.g. a stadium or the user's location). */
data class GeoPoint(val lat: Double, val lng: Double)

/** One row of the weekend plan: a time slot (e.g. "Sâmbătă seara") and the fixture picked for it. */
data class PlanSlot(
    val label: String,
    val fixture: Fixture?,      // null = free slot
    val distanceKm: Int?,       // distance to the stadium, if known
    val atStadium: Boolean,     // true = go to the stadium, false = watch on TV
)

/**
 * Weekend plan algorithm (planned for S12; kept here as tested, pure logic):
 * 1. take the scheduled fixtures from Friday to Sunday;
 * 2. split them into slots (Friday evening, Saturday/Sunday midday and evening);
 * 3. in each slot pick the fixture with the highest score;
 * 4. a fixture closer than [STADIUM_KM] km is marked "at the stadium", otherwise "on TV".
 *
 * Pure function: no network, no Android, so it is easy to unit test.
 */
object WeekendPlanner {

    const val STADIUM_KM = 60

    private data class SlotDef(val label: String, val day: DayOfWeek, val fromHour: Int, val toHour: Int)

    // Labels are shown in the UI, so they are in Romanian.
    private val SLOTS = listOf(
        SlotDef("Vineri seara", DayOfWeek.FRIDAY, 17, 24),
        SlotDef("Sâmbătă prânz", DayOfWeek.SATURDAY, 0, 17),
        SlotDef("Sâmbătă seara", DayOfWeek.SATURDAY, 17, 24),
        SlotDef("Duminică prânz", DayOfWeek.SUNDAY, 0, 17),
        SlotDef("Duminică seara", DayOfWeek.SUNDAY, 17, 24),
    )

    /** Friday of the coming weekend (or of the current one, if today is Friday to Sunday). */
    fun weekendStart(today: LocalDate): LocalDate = when (today.dayOfWeek) {
        DayOfWeek.SATURDAY -> today.minusDays(1)
        DayOfWeek.SUNDAY -> today.minusDays(2)
        else -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
    }

    /**
     * @param zone time zone used to decide the day and hour of each kick-off
     * @param stadiumOf where a fixture is played, or null if unknown
     */
    fun plan(
        today: LocalDate,
        fixtures: List<Fixture>,
        favoriteTeamIds: Set<Int>,
        zone: ZoneId,
        user: GeoPoint,
        stadiumOf: (Fixture) -> GeoPoint?,
    ): List<PlanSlot> {
        val friday = weekendStart(today)
        val weekend = fixtures.filter {
            val day = it.kickoff.atZone(zone).toLocalDate()
            it.status == FixtureStatus.SCHEDULED && day >= friday && day <= friday.plusDays(2)
        }

        return SLOTS.map { slot ->
            val candidates = weekend.filter {
                val local = it.kickoff.atZone(zone)
                local.dayOfWeek == slot.day && local.hour >= slot.fromHour && local.hour < slot.toHour
            }
            // highest score wins; on a tie, the earlier kick-off
            val best = candidates
                .map { f -> f to score(f, favoriteTeamIds, distanceKm(f, user, stadiumOf)) }
                .sortedWith(compareByDescending<Pair<Fixture, Double>> { it.second }.thenBy { it.first.kickoff })
                .firstOrNull()?.first

            val km = best?.let { distanceKm(it, user, stadiumOf) }
            PlanSlot(slot.label, best, km, atStadium = km != null && km <= STADIUM_KM)
        }
    }

    /**
     * Score of a fixture:
     *   +100 if a favorite team plays
     *   +0..100 for proximity (100 next to you, decreasing with distance)
     */
    private fun score(fixture: Fixture, favorites: Set<Int>, km: Int?): Double {
        var s = 0.0
        if (fixture.homeTeam.id in favorites || fixture.awayTeam.id in favorites) s += 100
        if (km != null) s += 100.0 * 10 / (10 + km)
        return s
    }

    private fun distanceKm(fixture: Fixture, user: GeoPoint, stadiumOf: (Fixture) -> GeoPoint?): Int? =
        stadiumOf(fixture)?.let { haversineKm(user, it).roundToInt() }

    /** Straight-line distance on the Earth's surface (haversine formula). */
    fun haversineKm(a: GeoPoint, b: GeoPoint): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }
}
