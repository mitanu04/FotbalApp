package ro.fluierulfinal.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Algoritmul planului de weekend (din planul aplicației):
 * 1. ia meciurile de vineri până duminică;
 * 2. le împarte pe sloturi (vineri seara, sâmbătă prânz/seara, duminică prânz/seara);
 * 3. în fiecare slot alege meciul cu scorul cel mai mare;
 * 4. un meci la mai puțin de [STADIUM_KM] km e marcat „la stadion”, restul „la TV”.
 *
 * E o funcție pură (nu atinge rețeaua sau Android-ul), deci se poate testa ușor cu unit tests.
 */
object WeekendPlanner {

    const val STADIUM_KM = 60

    private data class SlotDef(val label: String, val day: DayOfWeek, val fromHour: Int, val toHour: Int)

    private val SLOTS = listOf(
        SlotDef("Vineri seara", DayOfWeek.FRIDAY, 0, 24),
        SlotDef("Sâmbătă prânz", DayOfWeek.SATURDAY, 0, 17),
        SlotDef("Sâmbătă seara", DayOfWeek.SATURDAY, 17, 24),
        SlotDef("Duminică prânz", DayOfWeek.SUNDAY, 0, 17),
        SlotDef("Duminică seara", DayOfWeek.SUNDAY, 17, 24),
    )

    /** Vinerea weekendului următor (sau a celui curent, dacă azi e vineri–duminică). */
    fun weekendStart(today: LocalDate): LocalDate = when (today.dayOfWeek) {
        DayOfWeek.SATURDAY -> today.minusDays(1)
        DayOfWeek.SUNDAY -> today.minusDays(2)
        else -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
    }

    fun plan(
        today: LocalDate,
        matches: List<Match>,
        favoriteTeamIds: Set<String>,
        teamById: (String) -> Team,
        userLat: Double,
        userLng: Double,
    ): List<PlanSlot> {
        val friday = weekendStart(today)
        val weekend = matches.filter { it.date >= friday && it.date <= friday.plusDays(2) && it.status == MatchStatus.UPCOMING }

        return SLOTS.map { slot ->
            val candidates = weekend.filter {
                val hour = it.kickoff.substringBefore(":").toInt()
                it.date.dayOfWeek == slot.day && hour >= slot.fromHour && hour < slot.toHour
            }
            // scorul fiecărui meci candidat; la egalitate câștigă cel care începe mai devreme
            val best = candidates
                .map { m -> m to score(m, favoriteTeamIds, distanceKm(m, teamById, userLat, userLng)) }
                .sortedWith(compareByDescending<Pair<Match, Double>> { it.second }.thenBy { it.first.kickoff })
                .firstOrNull()?.first

            val km = best?.let { distanceKm(it, teamById, userLat, userLng) }
            PlanSlot(slot.label, best, km, atStadium = km != null && km <= STADIUM_KM)
        }
    }

    /**
     * Scorul unui meci:
     *   +100 dacă joacă o echipă favorită
     *   +0..100 pentru apropiere (100 la stadion lângă tine, scade odată cu distanța)
     */
    private fun score(match: Match, favorites: Set<String>, km: Int?): Double {
        var s = 0.0
        if (match.homeId in favorites || match.awayId in favorites) s += 100
        if (km != null) s += 100.0 * 10 / (10 + km)
        return s
    }

    /** Distanța până la stadionul gazdei, în km, sau null dacă nu știm unde e stadionul. */
    private fun distanceKm(match: Match, teamById: (String) -> Team, userLat: Double, userLng: Double): Int? {
        val home = teamById(match.homeId)
        val lat = home.lat ?: return null
        val lng = home.lng ?: return null
        return haversineKm(userLat, userLng, lat, lng).roundToInt()
    }

    /** Distanța „în linie dreaptă” pe suprafața Pământului (formula haversine). */
    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
