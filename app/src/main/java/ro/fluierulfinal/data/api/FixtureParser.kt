package ro.fluierulfinal.data.api

import org.json.JSONObject
import ro.fluierulfinal.data.Match
import ro.fluierulfinal.data.MatchStatus
import ro.fluierulfinal.data.Team
import ro.fluierulfinal.data.apiLogoUrl
import java.time.OffsetDateTime

/** Ce scoatem dintr-un răspuns /fixtures: meciurile și echipele care apar în ele. */
data class ParsedFixtures(val matches: List<Match>, val teams: List<Team>)

/**
 * Transformă JSON-ul de la /fixtures în clasele aplicației (Match, Team).
 * E o funcție pură: primește text, întoarce obiecte; nu atinge rețeaua, deci se testează ușor.
 */
object FixtureParser {

    /** ID-ul ligii în API-Football → ID-ul ligii în aplicație (vezi MatchRepository.leagues). */
    val LEAGUE_IDS: Map<Int, String> = mapOf(
        283 to "sl",   // Liga I (SuperLiga)
        284 to "l2",   // Liga II
        140 to "ll",   // La Liga
        39 to "pl",    // Premier League
        135 to "sa",   // Serie A
        2 to "ucl",    // UEFA Champions League
        78 to "bl",    // Bundesliga
        61 to "l1",    // Ligue 1
    )

    // Codurile scurte de status din API: https://www.api-football.com/documentation-v3#tag/Fixtures
    private val LIVE = setOf("1H", "HT", "2H", "ET", "BT", "P", "INT", "SUSP", "LIVE")
    private val FINISHED = setOf("FT", "AET", "PEN", "AWD", "WO")
    // restul (NS, TBD, PST, CANC, ABD) le tratăm ca „încă nejucat”

    fun parse(json: String): ParsedFixtures {
        val response = JSONObject(json).getJSONArray("response")
        val matches = mutableListOf<Match>()
        val teams = linkedMapOf<String, Team>() // după id, ca o echipă să apară o singură dată

        for (i in 0 until response.length()) {
            val item = response.getJSONObject(i)
            // păstrăm doar ligile pe care le are aplicația
            val leagueId = LEAGUE_IDS[item.getJSONObject("league").getInt("id")] ?: continue

            val fixture = item.getJSONObject("fixture")
            val status = fixture.getJSONObject("status")
            val home = item.getJSONObject("teams").getJSONObject("home")
            val away = item.getJSONObject("teams").getJSONObject("away")
            val goals = item.getJSONObject("goals")
            val city = fixture.getJSONObject("venue").optStringOrNull("city") ?: ""

            // data vine cu fusul orar cerut, ex. „2026-11-20T21:30:00+02:00”
            val start = OffsetDateTime.parse(fixture.getString("date"))
            val homeTeam = teamFrom(home, city)
            val awayTeam = teamFrom(away, city = "")
            teams.putIfAbsent(homeTeam.id, homeTeam)
            teams.putIfAbsent(awayTeam.id, awayTeam)

            matches += Match(
                id = fixture.getInt("id"),
                leagueId = leagueId,
                date = start.toLocalDate(),
                kickoff = "%02d:%02d".format(start.hour, start.minute),
                homeId = homeTeam.id,
                awayId = awayTeam.id,
                status = statusOf(status.getString("short")),
                minute = status.optIntOrNull("elapsed"),
                homeGoals = goals.optIntOrNull("home"),
                awayGoals = goals.optIntOrNull("away"),
                tvChannel = "",  // API-Football nu știe pe ce canal TV se dă meciul
                fromApi = true,
            )
        }
        return ParsedFixtures(matches, teams.values.toList())
    }

    fun statusOf(short: String): MatchStatus = when (short) {
        in LIVE -> MatchStatus.LIVE
        in FINISHED -> MatchStatus.FINISHED
        else -> MatchStatus.UPCOMING
    }

    /**
     * API-ul nu dă culoarea echipei, așa că alegem una din paletă după id
     * (mereu aceeași pentru aceeași echipă). Coordonatele stadionului rămân necunoscute deocamdată.
     */
    private fun teamFrom(obj: JSONObject, city: String): Team {
        val apiId = obj.getInt("id")
        return Team(
            id = "api$apiId",
            name = obj.getString("name"),
            color = PALETTE[apiId % PALETTE.size],
            city = city,
            logoUrl = obj.optStringOrNull("logo") ?: apiLogoUrl("api$apiId"),
        )
    }

    private val PALETTE = listOf(
        0xFFC8102E, 0xFF1D4FA3, 0xFF2E8B57, 0xFF5B2A86, 0xFFE8482C, 0xFF0B6BCB, 0xFF8A1538, 0xFF14427A,
    )

    // În JSON, câmpurile lipsă vin ca `null`; optInt ar întoarce 0, ceea ce ar însemna „0 goluri”.
    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
}
