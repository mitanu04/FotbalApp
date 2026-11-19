package ro.fluierulfinal.domain.model

/** A league table for one season; [groups] has one entry for simple leagues, several for split formats. */
data class Standings(
    val leagueId: Int,
    val season: Int,
    val groups: List<StandingGroup>,
)

/** One table, e.g. "Regular Season" or "Championship Round". */
data class StandingGroup(
    val name: String,
    val rows: List<StandingRow>,
)

data class StandingRow(
    val rank: Int,
    val team: Team,
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDiff: Int,
    val points: Int,
    val form: List<FormLetter>,        // most recent first
    val description: String?,          // what this position leads to, e.g. "UEFA Champions League Qualifiers"
    val home: StandingRecord = StandingRecord(),   // matches played at home only
    val away: StandingRecord = StandingRecord(),   // matches played away only
)

/** Results over a set of matches (home only, away only...). Points are 3 per win, 1 per draw. */
data class StandingRecord(
    val played: Int = 0,
    val win: Int = 0,
    val draw: Int = 0,
    val lose: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
) {
    val points get() = win * 3 + draw
    val goalDiff get() = goalsFor - goalsAgainst
}

/** One line of a league's top scorers list. */
data class TopScorer(
    val rank: Int,
    val playerId: Int,
    val name: String,
    val photoUrl: String?,
    val team: Team,
    val goals: Int,
    val assists: Int,
    val penalties: Int,              // goals scored from the penalty spot
    val appearances: Int,
)

/** One past result in the form column. */
enum class FormLetter { WIN, DRAW, LOSS }
