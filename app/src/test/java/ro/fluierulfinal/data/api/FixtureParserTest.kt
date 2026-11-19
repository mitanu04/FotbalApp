package ro.fluierulfinal.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ro.fluierulfinal.data.MatchStatus
import java.time.LocalDate

class FixtureParserTest {

    // Bucăți dintr-un răspuns real /fixtures (20 nov 2026), plus un meci live inventat
    // și unul dintr-o ligă pe care aplicația nu o are (trebuie ignorat).
    private val json = """
    {
      "errors": [],
      "response": [
        {
          "fixture": { "id": 1575221, "date": "2026-11-20T21:30:00+02:00",
                       "venue": { "id": 700, "name": "Allianz Arena", "city": "Munich" },
                       "status": { "short": "NS", "elapsed": null } },
          "league": { "id": 78, "name": "Bundesliga" },
          "teams": { "home": { "id": 157, "name": "Bayern München", "logo": "https://media.api-sports.io/football/teams/157.png" },
                     "away": { "id": 192, "name": "1. FC Köln" } },
          "goals": { "home": null, "away": null }
        },
        {
          "fixture": { "id": 999, "date": "2026-11-20T20:30:00+02:00",
                       "venue": { "id": 1, "name": "Central Stadium", "city": "Ovidiu" },
                       "status": { "short": "2H", "elapsed": 67 } },
          "league": { "id": 283, "name": "Liga I" },
          "teams": { "home": { "id": 2599, "name": "Farul Constanta" },
                     "away": { "id": 6230, "name": "Universitatea Cluj" } },
          "goals": { "home": 0, "away": 2 }
        },
        {
          "fixture": { "id": 5, "date": "2026-11-20T15:00:00+02:00",
                       "venue": { "id": 2, "name": "X", "city": null },
                       "status": { "short": "FT", "elapsed": 90 } },
          "league": { "id": 12345, "name": "Super League Zambia" },
          "teams": { "home": { "id": 1, "name": "A" }, "away": { "id": 2, "name": "B" } },
          "goals": { "home": 1, "away": 0 }
        }
      ]
    }
    """.trimIndent()

    @Test
    fun keepsOnlyOurLeagues() {
        val parsed = FixtureParser.parse(json)
        assertEquals(listOf(1575221, 999), parsed.matches.map { it.id })
        assertEquals(4, parsed.teams.size)
    }

    @Test
    fun upcomingMatchHasNoScore() {
        val m = FixtureParser.parse(json).matches.first { it.id == 1575221 }
        assertEquals("bl", m.leagueId)
        assertEquals(LocalDate.of(2026, 11, 20), m.date)
        assertEquals("21:30", m.kickoff)
        assertEquals(MatchStatus.UPCOMING, m.status)
        assertNull(m.homeGoals)
        assertNull(m.minute)
        assertEquals("api157", m.homeId)
    }

    @Test
    fun liveMatchHasMinuteAndScore() {
        val m = FixtureParser.parse(json).matches.first { it.id == 999 }
        assertEquals("sl", m.leagueId)
        assertEquals(MatchStatus.LIVE, m.status)
        assertEquals(67, m.minute)
        assertEquals(0, m.homeGoals)
        assertEquals(2, m.awayGoals)
    }

    @Test
    fun homeTeamGetsVenueCity() {
        val teams = FixtureParser.parse(json).teams.associateBy { it.id }
        assertEquals("Munich", teams.getValue("api157").city)
        assertEquals("https://media.api-sports.io/football/teams/157.png", teams.getValue("api157").logoUrl)
    }

    @Test
    fun statusCodes() {
        assertEquals(MatchStatus.LIVE, FixtureParser.statusOf("HT"))
        assertEquals(MatchStatus.FINISHED, FixtureParser.statusOf("PEN"))
        assertEquals(MatchStatus.UPCOMING, FixtureParser.statusOf("PST"))
    }
}
