package ro.fluierulfinal.domain.planner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.domain.model.Score
import ro.fluierulfinal.domain.model.Team
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WeekendPlannerTest {

    private val zone = ZoneId.of("Europe/Bucharest")
    private val timisoara = GeoPoint(45.7489, 21.2087)
    private val thursday = LocalDate.of(2026, 11, 19)

    private fun fixture(id: Int, home: Int, away: Int, at: LocalDateTime, status: FixtureStatus = FixtureStatus.SCHEDULED) =
        Fixture(
            id = id, leagueId = 283, season = 2026,
            kickoff = at.atZone(zone).toInstant(), status = status, elapsedMinutes = null,
            homeTeam = Team(home, "Team $home", null), awayTeam = Team(away, "Team $away", null),
            score = Score(null, null),
        )

    @Test
    fun weekendStart_isComingFriday() {
        assertEquals(LocalDate.of(2026, 11, 20), WeekendPlanner.weekendStart(thursday))
        assertEquals(LocalDate.of(2026, 11, 20), WeekendPlanner.weekendStart(LocalDate.of(2026, 11, 22))) // Sunday
    }

    @Test
    fun favoriteBeatsCloserMatch_andNearbyMatchIsAtStadium() {
        val sat = LocalDate.of(2026, 11, 21)
        val nearby = fixture(1, home = 10, away = 11, at = sat.atTime(20, 0))   // played in Timișoara
        val favorite = fixture(2, home = 20, away = 21, at = sat.atTime(20, 30)) // far away, but favorite
        val stadiums = mapOf(1 to timisoara, 2 to GeoPoint(44.44, 26.10))         // Bucharest

        val plan = WeekendPlanner.plan(thursday, listOf(nearby, favorite), favoriteTeamIds = setOf(21),
            zone = zone, user = timisoara) { stadiums[it.id] }

        val saturdayEvening = plan.single { it.label == "Sâmbătă seara" }
        assertEquals(2, saturdayEvening.fixture?.id)
        assertFalse(saturdayEvening.atStadium)

        // without the favorite, the nearby match wins and is 0 km away → at the stadium
        val plan2 = WeekendPlanner.plan(thursday, listOf(nearby), emptySet(), zone, timisoara) { stadiums[it.id] }
        val slot = plan2.single { it.label == "Sâmbătă seara" }
        assertEquals(1, slot.fixture?.id)
        assertEquals(0, slot.distanceKm)
        assertTrue(slot.atStadium)
    }

    @Test
    fun friday_afternoon_isNotFridayEvening_andFinishedMatchesAreSkipped() {
        val fri = LocalDate.of(2026, 11, 20)
        val afternoon = fixture(1, 1, 2, fri.atTime(14, 30))
        val finished = fixture(2, 3, 4, fri.atTime(20, 0), FixtureStatus.FINISHED)

        val plan = WeekendPlanner.plan(thursday, listOf(afternoon, finished), emptySet(), zone, timisoara) { null }
        assertNull(plan.single { it.label == "Vineri seara" }.fixture)
    }

    @Test
    fun haversine_timisoaraToBucharest() {
        val km = WeekendPlanner.haversineKm(timisoara, GeoPoint(44.4268, 26.1025))
        assertTrue("got $km", km in 410.0..430.0)
    }
}
