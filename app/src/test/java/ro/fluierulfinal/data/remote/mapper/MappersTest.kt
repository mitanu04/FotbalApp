package ro.fluierulfinal.data.remote.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import ro.fluierulfinal.data.remote.ApiJson
import ro.fluierulfinal.data.remote.dto.ApiResponseDto
import ro.fluierulfinal.data.remote.dto.FixtureItemDto
import ro.fluierulfinal.data.remote.dto.LeagueItemDto
import ro.fluierulfinal.data.remote.dto.StandingsItemDto
import ro.fluierulfinal.data.remote.dto.TeamItemDto
import ro.fluierulfinal.data.remote.dto.TopScorerItemDto
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.domain.model.FormLetter
import ro.fluierulfinal.domain.model.Score
import java.time.Instant

/**
 * Mapper tests on real API-Football responses (trimmed copies in src/test/resources/api),
 * so they also check that the DTOs match what the API really sends. No network is used.
 */
class MappersTest {

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("api/$name")) { "missing test resource $name" }.readText()

    private val fixtures by lazy {
        ApiJson.decodeFromString<ApiResponseDto<List<FixtureItemDto>>>(resource("fixtures_sample.json"))
            .response.map { it.toDomain() }.associateBy { it.id }
    }

    @Test
    fun scheduledFixture_hasNoScoreAndCorrectKickoff() {
        val f = fixtures.getValue(1575221) // Bayern München - 1. FC Köln, 20 Nov 2026 21:30 (Bucharest)
        assertEquals(78, f.leagueId)
        assertEquals(2026, f.season)
        assertEquals(FixtureStatus.SCHEDULED, f.status)
        assertEquals(Instant.parse("2026-11-20T19:30:00Z"), f.kickoff)
        assertEquals(Score(null, null), f.score)
        assertNull(f.elapsedMinutes)
        assertEquals("Bayern München", f.homeTeam.name)
        assertEquals("https://media.api-sports.io/football/teams/157.png", f.homeTeam.logoUrl)
        assertEquals("Allianz Arena", f.venueName)
    }

    @Test
    fun liveFixtures_haveMinuteAndScore() {
        val firstHalf = fixtures.getValue(1638292)
        assertEquals(FixtureStatus.LIVE, firstHalf.status)
        assertEquals(45, firstHalf.elapsedMinutes)
        assertEquals(Score(0, 2), firstHalf.score)

        assertEquals(FixtureStatus.LIVE, fixtures.getValue(1651044).status) // half-time counts as live
        assertEquals(FixtureStatus.LIVE, fixtures.getValue(1611455).status)
    }

    @Test
    fun finishedFixture_keepsScoreButNoMinute() {
        val f = fixtures.getValue(1631779)
        assertEquals(FixtureStatus.FINISHED, f.status)
        assertEquals(Score(2, 0), f.score)
        assertNull(f.elapsedMinutes) // the API still sends 90; we only keep the minute while live
    }

    @Test
    fun postponedAndCancelled() {
        assertEquals(FixtureStatus.POSTPONED, fixtures.getValue(1649584).status)
        assertEquals(FixtureStatus.CANCELLED, fixtures.getValue(1605134).status)
    }

    @Test
    fun statusCodes() {
        assertEquals(FixtureStatus.SCHEDULED, fixtureStatusOf("NS"))
        assertEquals(FixtureStatus.SCHEDULED, fixtureStatusOf("TBD"))
        assertEquals(FixtureStatus.LIVE, fixtureStatusOf("ET"))
        assertEquals(FixtureStatus.FINISHED, fixtureStatusOf("PEN"))
        assertEquals(FixtureStatus.CANCELLED, fixtureStatusOf("ABD"))
        assertEquals(FixtureStatus.SCHEDULED, fixtureStatusOf("SOMETHING_NEW"))
    }

    @Test
    fun team_fromTeamsEndpoint() {
        val teams = ApiJson.decodeFromString<ApiResponseDto<List<TeamItemDto>>>(resource("teams_sample.json"))
            .response.map { it.toDomain() }.associateBy { it.id }
        val fcsb = teams.getValue(559)
        assertEquals("FCSB", fcsb.name)
        assertEquals("STE", fcsb.code)
        assertEquals("Romania", fcsb.country)
        assertEquals(1326, fcsb.venueId)
        assertEquals("Rapid", teams.getValue(6231).name)
    }

    @Test
    fun league_picksRequestedSeasonOrCurrent() {
        val item = ApiJson.decodeFromString<ApiResponseDto<List<LeagueItemDto>>>(resource("leagues_sample.json"))
            .response.single()

        val current = item.toDomain()!!
        assertEquals(2026, current.season)
        assertEquals("La Liga", current.name)
        assertEquals("Spain", current.country)
        assertTrue(current.coverage.events)
        assertFalse(current.coverage.lineups)
        assertTrue(current.coverage.statisticsFixtures)

        val old = item.toDomain(season = 2024)!!
        assertTrue(old.coverage.lineups)
        assertTrue(old.coverage.odds)

        assertNull(item.toDomain(season = 1990))
    }

    @Test
    fun standings_liga1_2024_hasThreeGroups() {
        val s = ApiJson.decodeFromString<ApiResponseDto<List<StandingsItemDto>>>(resource("standings_283_2024.json"))
            .response.single().toDomain()
        assertEquals(283, s.leagueId)
        assertEquals(2024, s.season)
        assertEquals(listOf("Championship Round", "Relegation Round", "Regular Season"), s.groups.map { it.name })
        assertEquals(listOf(6, 10, 16), s.groups.map { it.rows.size })

        val leader = s.groups.first().rows.first()
        assertEquals(1, leader.rank)
        assertEquals("FCSB", leader.team.name)
        assertEquals(52, leader.points)
        assertEquals(10, leader.played)
        assertEquals(18, leader.goalsFor)
        assertEquals(9, leader.goalsAgainst)
        assertEquals("UEFA Champions League Qualifiers", leader.description)
        // API form "DWWWW" lists the most recent result last; we keep the most recent first
        assertEquals(FormLetter.WIN, leader.form.first())
        assertEquals(FormLetter.DRAW, leader.form.last())
        // home / away split of the same 10 play-off matches
        assertEquals(5, leader.home.played)
        assertEquals(4, leader.home.win)
        assertEquals(13, leader.home.points)
        assertEquals(5, leader.away.played)
        assertEquals(11, leader.away.points)
    }

    @Test
    fun topScorers_liga1_2024() {
        val scorers = ApiJson.decodeFromString<ApiResponseDto<List<TopScorerItemDto>>>(resource("topscorers_283_2024.json"))
            .response.toTopScorers()
        assertEquals(20, scorers.size)
        val first = scorers.first()
        assertEquals(1, first.rank)
        assertEquals("A. Mitriță", first.name)
        assertEquals("Universitatea Craiova", first.team.name)
        assertEquals(18, first.goals)
        assertEquals(4, first.assists)
        assertEquals(4, first.penalties)
        // players with the same number of goals share a rank
        scorers.zipWithNext().forEach { (a, b) -> if (a.goals == b.goals) assertEquals(a.rank, b.rank) }
    }

    @Test
    fun errorsField_isObjectWhenApiRefuses() {
        val json = """{"errors":{"plan":"Free plans do not have access to this date"},"results":0,"response":[]}"""
        val parsed = ApiJson.decodeFromString<ApiResponseDto<List<FixtureItemDto>>>(json)
        assertTrue(parsed.response.isEmpty())
        assertTrue(parsed.errors.toString().contains("Free plans"))
    }
}
