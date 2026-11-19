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
)

/** One past result in the form column. */
enum class FormLetter { WIN, DRAW, LOSS }
